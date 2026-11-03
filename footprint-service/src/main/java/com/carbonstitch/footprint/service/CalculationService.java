package com.carbonstitch.footprint.service;

import com.carbonstitch.footprint.dto.client.MlPredictRequest;
import com.carbonstitch.footprint.dto.client.MlPredictResponse;
import com.carbonstitch.footprint.dto.response.FootprintResponse;
import com.carbonstitch.footprint.exception.ResourceNotFoundException;
import com.carbonstitch.footprint.model.entity.FootprintResult;
import com.carbonstitch.footprint.model.entity.Garment;
import com.carbonstitch.footprint.model.entity.MaterialComposition;
import com.carbonstitch.footprint.model.entity.ProductionInfo;
import com.carbonstitch.footprint.model.enums.CalculationMethod;
import com.carbonstitch.footprint.repository.FootprintResultRepository;
import com.carbonstitch.footprint.repository.GarmentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalculationService {

  private final GarmentRepository garmentRepository;
  private final FootprintResultRepository footprintRepository;
  private final EmissionFactorService factorService;
  private final MlServiceClient mlServiceClient;

  private static final BigDecimal HUNDRED = new BigDecimal("100");
  private static final BigDecimal THOUSAND = new BigDecimal("1000");

  @Transactional
  public FootprintResponse calculateAndSaveFootprint(Long garmentId) {
    Garment garment =
        garmentRepository
            .findByIdWithDetails(garmentId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Garment not found with ID: " + garmentId));

    FootprintResult result = calculate(garment);

    // If an existing footprint exists, update its values to preserve DB foreign key uniqueness
    FootprintResult entityToSave =
        footprintRepository
            .findByGarmentId(garmentId)
            .map(
                existing -> {
                  existing.setTotalKgCO2e(result.getTotalKgCO2e());
                  existing.setMaterialKgCO2e(result.getMaterialKgCO2e());
                  existing.setManufacturingKgCO2e(result.getManufacturingKgCO2e());
                  existing.setTransportKgCO2e(result.getTransportKgCO2e());
                  existing.setPrimaryEmissionDriver(result.getPrimaryEmissionDriver());
                  existing.setCalculationMethod(result.getCalculationMethod());
                  existing.setCalculatedAt(Instant.now());
                  return existing;
                })
            .orElseGet(
                () -> {
                  result.setGarment(garment);
                  return result;
                });

    FootprintResult saved = footprintRepository.save(entityToSave);
    garment.setFootprintResult(saved);

    log.info(
        "Calculated footprint for garment ID {} (SKU: {}): {} kg CO2e via {}",
        garment.getId(),
        garment.getSku(),
        saved.getTotalKgCO2e(),
        saved.getCalculationMethod());

    return toResponse(garment, saved);
  }

  @Transactional(readOnly = true)
  public FootprintResponse getFootprint(Long garmentId) {
    Garment garment =
        garmentRepository
            .findById(garmentId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Garment not found with ID: " + garmentId));

    FootprintResult result =
        footprintRepository
            .findByGarmentId(garmentId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "No footprint calculation found for garment ID: " + garmentId));

    return toResponse(garment, result);
  }

  /** Core Life Cycle Assessment (LCA) calculation logic. */
  public FootprintResult calculate(Garment garment) {
    List<MaterialComposition> materials = garment.getMaterials();
    ProductionInfo prod = garment.getProductionInfo();
    BigDecimal weight = garment.getWeightKg();

    // Check if material composition is present and covers substantial portion (> 50%)
    BigDecimal totalPercentage =
        materials != null
            ? materials.stream()
                .map(MaterialComposition::getPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
            : BigDecimal.ZERO;

    boolean hasSufficientMaterialData =
        materials != null
            && !materials.isEmpty()
            && totalPercentage.compareTo(new BigDecimal("50")) >= 0;

    if (hasSufficientMaterialData) {
      return calculateRuleBased(garment, materials, prod, weight);
    }

    // Material data is incomplete: call ML Service for inference
    log.info(
        "Garment ID {} (SKU: {}) has incomplete material composition ({}%); invoking ML service...",
        garment.getId(), garment.getSku(), totalPercentage);

    String primaryMaterial =
        (materials != null && !materials.isEmpty()) ? materials.get(0).getMaterial() : "COTTON";

    MlPredictRequest mlRequest =
        MlPredictRequest.builder()
            .category(garment.getCategory().name())
            .weightKg(weight)
            .mainMaterial(primaryMaterial)
            .country(prod.getCountry())
            .transportMode(prod.getTransportMode().name())
            .distanceKm(prod.getDistanceKm())
            .build();

    Optional<MlPredictResponse> mlResponse = mlServiceClient.predictFootprint(mlRequest);

    if (mlResponse.isPresent()) {
      BigDecimal predictedTotal =
          mlResponse.get().getEstimatedKgCO2e().setScale(4, RoundingMode.HALF_UP);
      // Derive proportional breakdown based on empirical apparel LCA ratios
      BigDecimal materialShare =
          predictedTotal.multiply(new BigDecimal("0.50")).setScale(4, RoundingMode.HALF_UP);
      BigDecimal mfgShare =
          predictedTotal.multiply(new BigDecimal("0.35")).setScale(4, RoundingMode.HALF_UP);
      BigDecimal transportShare =
          predictedTotal
              .subtract(materialShare)
              .subtract(mfgShare)
              .setScale(4, RoundingMode.HALF_UP);

      return FootprintResult.builder()
          .garment(garment)
          .totalKgCO2e(predictedTotal)
          .materialKgCO2e(materialShare)
          .manufacturingKgCO2e(mfgShare)
          .transportKgCO2e(transportShare)
          .primaryEmissionDriver("ML_ESTIMATE (" + primaryMaterial + ")")
          .calculationMethod(CalculationMethod.ML_PREDICTION)
          .calculatedAt(Instant.now())
          .build();
    }

    // Graceful fallback when ML service is down or unreachable
    log.warn(
        "ML Service unavailable for garment ID {}; performing rule-based hybrid fallback estimation",
        garment.getId());
    return calculateFallbackRuleBased(garment, prod, weight);
  }

  private FootprintResult calculateRuleBased(
      Garment garment,
      List<MaterialComposition> materials,
      ProductionInfo prod,
      BigDecimal weight) {
    BigDecimal materialTotal = BigDecimal.ZERO;
    String dominantMaterial = "NONE";
    BigDecimal maxMaterialEmission = BigDecimal.ZERO;

    for (MaterialComposition mat : materials) {
      BigDecimal factor = factorService.getMaterialFactor(mat.getMaterial());
      // weightKg * (percentage / 100) * factor
      BigDecimal fraction = mat.getPercentage().divide(HUNDRED, 6, RoundingMode.HALF_UP);
      BigDecimal matEmission = weight.multiply(fraction).multiply(factor);

      materialTotal = materialTotal.add(matEmission);
      if (matEmission.compareTo(maxMaterialEmission) > 0) {
        maxMaterialEmission = matEmission;
        dominantMaterial = mat.getMaterial();
      }
    }

    // Manufacturing: weightKg * gridFactor(country)
    BigDecimal gridFactor = factorService.getGridFactor(prod.getCountry());
    BigDecimal mfgEmission = weight.multiply(gridFactor);

    // Transport: (weightKg / 1000) * distanceKm * transportFactor
    BigDecimal transportFactor = factorService.getTransportFactor(prod.getTransportMode());
    BigDecimal weightTonnes = weight.divide(THOUSAND, 6, RoundingMode.HALF_UP);
    BigDecimal transportEmission =
        weightTonnes.multiply(prod.getDistanceKm()).multiply(transportFactor);

    BigDecimal total =
        materialTotal.add(mfgEmission).add(transportEmission).setScale(4, RoundingMode.HALF_UP);

    return FootprintResult.builder()
        .garment(garment)
        .totalKgCO2e(total)
        .materialKgCO2e(materialTotal.setScale(4, RoundingMode.HALF_UP))
        .manufacturingKgCO2e(mfgEmission.setScale(4, RoundingMode.HALF_UP))
        .transportKgCO2e(transportEmission.setScale(4, RoundingMode.HALF_UP))
        .primaryEmissionDriver(dominantMaterial)
        .calculationMethod(CalculationMethod.RULE_BASED)
        .calculatedAt(Instant.now())
        .build();
  }

  private FootprintResult calculateFallbackRuleBased(
      Garment garment, ProductionInfo prod, BigDecimal weight) {
    BigDecimal defaultMaterialFactor = factorService.getMaterialFactor("OTHER");
    BigDecimal materialEmission = weight.multiply(defaultMaterialFactor);

    BigDecimal gridFactor = factorService.getGridFactor(prod.getCountry());
    BigDecimal mfgEmission = weight.multiply(gridFactor);

    BigDecimal transportFactor = factorService.getTransportFactor(prod.getTransportMode());
    BigDecimal weightTonnes = weight.divide(THOUSAND, 6, RoundingMode.HALF_UP);
    BigDecimal transportEmission =
        weightTonnes.multiply(prod.getDistanceKm()).multiply(transportFactor);

    BigDecimal total =
        materialEmission.add(mfgEmission).add(transportEmission).setScale(4, RoundingMode.HALF_UP);

    return FootprintResult.builder()
        .garment(garment)
        .totalKgCO2e(total)
        .materialKgCO2e(materialEmission.setScale(4, RoundingMode.HALF_UP))
        .manufacturingKgCO2e(mfgEmission.setScale(4, RoundingMode.HALF_UP))
        .transportKgCO2e(transportEmission.setScale(4, RoundingMode.HALF_UP))
        .primaryEmissionDriver("ESTIMATED_GENERIC_FIBER")
        .calculationMethod(CalculationMethod.HYBRID_FALLBACK)
        .calculatedAt(Instant.now())
        .build();
  }

  public FootprintResponse toResponse(Garment garment, FootprintResult result) {
    return FootprintResponse.builder()
        .garmentId(garment.getId())
        .sku(garment.getSku())
        .garmentName(garment.getName())
        .category(garment.getCategory())
        .weightKg(garment.getWeightKg())
        .totalKgCO2e(result.getTotalKgCO2e())
        .materialKgCO2e(result.getMaterialKgCO2e())
        .manufacturingKgCO2e(result.getManufacturingKgCO2e())
        .transportKgCO2e(result.getTransportKgCO2e())
        .primaryEmissionDriver(result.getPrimaryEmissionDriver())
        .calculationMethod(result.getCalculationMethod())
        .calculatedAt(result.getCalculatedAt())
        .build();
  }
}
