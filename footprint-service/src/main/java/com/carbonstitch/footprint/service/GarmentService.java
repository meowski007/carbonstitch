package com.carbonstitch.footprint.service;

import com.carbonstitch.footprint.dto.request.GarmentCreateRequest;
import com.carbonstitch.footprint.dto.request.GarmentUpdateRequest;
import com.carbonstitch.footprint.dto.request.MaterialCompositionDto;
import com.carbonstitch.footprint.dto.request.ProductionInfoDto;
import com.carbonstitch.footprint.dto.response.FootprintResponse;
import com.carbonstitch.footprint.dto.response.GarmentResponse;
import com.carbonstitch.footprint.dto.response.HotspotReportResponse;
import com.carbonstitch.footprint.exception.DuplicateResourceException;
import com.carbonstitch.footprint.exception.ResourceNotFoundException;
import com.carbonstitch.footprint.model.entity.FootprintResult;
import com.carbonstitch.footprint.model.entity.Garment;
import com.carbonstitch.footprint.model.entity.MaterialComposition;
import com.carbonstitch.footprint.model.entity.ProductionInfo;
import com.carbonstitch.footprint.repository.FootprintResultRepository;
import com.carbonstitch.footprint.repository.GarmentRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GarmentService {

  private final GarmentRepository garmentRepository;
  private final FootprintResultRepository footprintRepository;

  @Transactional
  public GarmentResponse createGarment(GarmentCreateRequest req) {
    if (garmentRepository.existsBySku(req.getSku())) {
      throw new DuplicateResourceException("Garment already exists with SKU: " + req.getSku());
    }

    ProductionInfo prod =
        ProductionInfo.builder()
            .country(req.getProductionInfo().getCountry().toUpperCase())
            .transportMode(req.getProductionInfo().getTransportMode())
            .distanceKm(req.getProductionInfo().getDistanceKm())
            .build();

    Garment garment =
        Garment.builder()
            .sku(req.getSku())
            .name(req.getName())
            .category(req.getCategory())
            .weightKg(req.getWeightKg())
            .productionInfo(prod)
            .build();

    if (req.getMaterials() != null) {
      for (MaterialCompositionDto mDto : req.getMaterials()) {
        MaterialComposition comp =
            MaterialComposition.builder()
                .material(mDto.getMaterial().toUpperCase())
                .percentage(mDto.getPercentage())
                .build();
        garment.addMaterial(comp);
      }
    }

    Garment saved = garmentRepository.save(garment);
    log.info("Created garment with ID {} and SKU {}", saved.getId(), saved.getSku());
    return toResponse(saved);
  }

  @Transactional
  public GarmentResponse updateGarment(Long id, GarmentUpdateRequest req) {
    Garment garment =
        garmentRepository
            .findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Garment not found with ID: " + id));

    garment.setName(req.getName());
    garment.setCategory(req.getCategory());
    garment.setWeightKg(req.getWeightKg());

    ProductionInfo prod =
        ProductionInfo.builder()
            .country(req.getProductionInfo().getCountry().toUpperCase())
            .transportMode(req.getProductionInfo().getTransportMode())
            .distanceKm(req.getProductionInfo().getDistanceKm())
            .build();
    garment.setProductionInfo(prod);

    garment.clearMaterials();
    if (req.getMaterials() != null) {
      for (MaterialCompositionDto mDto : req.getMaterials()) {
        MaterialComposition comp =
            MaterialComposition.builder()
                .material(mDto.getMaterial().toUpperCase())
                .percentage(mDto.getPercentage())
                .build();
        garment.addMaterial(comp);
      }
    }

    Garment updated = garmentRepository.save(garment);
    log.info("Updated garment ID {}", updated.getId());
    return toResponse(updated);
  }

  @Transactional(readOnly = true)
  public GarmentResponse getGarment(Long id) {
    Garment garment =
        garmentRepository
            .findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Garment not found with ID: " + id));
    return toResponse(garment);
  }

  @Transactional(readOnly = true)
  public List<GarmentResponse> listGarments() {
    return garmentRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional
  public void deleteGarment(Long id) {
    if (!garmentRepository.existsById(id)) {
      throw new ResourceNotFoundException("Garment not found with ID: " + id);
    }
    garmentRepository.deleteById(id);
    log.info("Deleted garment ID {}", id);
  }

  @Transactional(readOnly = true)
  public HotspotReportResponse getHotspotsReport(int limit) {
    int queryLimit = Math.max(1, Math.min(limit, 100));
    List<FootprintResult> topHotspots =
        footprintRepository.findTopEmissionHotspots(PageRequest.of(0, queryLimit));

    List<HotspotReportResponse.HotspotItem> items = new ArrayList<>();
    int rank = 1;
    for (FootprintResult result : topHotspots) {
      Garment g = result.getGarment();
      items.add(
          HotspotReportResponse.HotspotItem.builder()
              .rank(rank++)
              .garmentId(g.getId())
              .sku(g.getSku())
              .name(g.getName())
              .category(g.getCategory())
              .weightKg(g.getWeightKg())
              .totalKgCO2e(result.getTotalKgCO2e())
              .drivingMaterial(result.getPrimaryEmissionDriver())
              .calculationMethod(result.getCalculationMethod())
              .build());
    }

    return HotspotReportResponse.builder()
        .requestedLimit(queryLimit)
        .totalHotspots(items.size())
        .generatedAt(Instant.now())
        .hotspots(items)
        .build();
  }

  public GarmentResponse toResponse(Garment g) {
    ProductionInfoDto prodDto =
        g.getProductionInfo() != null
            ? ProductionInfoDto.builder()
                .country(g.getProductionInfo().getCountry())
                .transportMode(g.getProductionInfo().getTransportMode())
                .distanceKm(g.getProductionInfo().getDistanceKm())
                .build()
            : null;

    List<MaterialCompositionDto> matDtos =
        g.getMaterials() != null
            ? g.getMaterials().stream()
                .map(
                    m ->
                        MaterialCompositionDto.builder()
                            .material(m.getMaterial())
                            .percentage(m.getPercentage())
                            .build())
                .toList()
            : List.of();

    FootprintResponse fpResponse =
        g.getFootprintResult() != null
            ? FootprintResponse.builder()
                .garmentId(g.getId())
                .sku(g.getSku())
                .garmentName(g.getName())
                .category(g.getCategory())
                .weightKg(g.getWeightKg())
                .totalKgCO2e(g.getFootprintResult().getTotalKgCO2e())
                .materialKgCO2e(g.getFootprintResult().getMaterialKgCO2e())
                .manufacturingKgCO2e(g.getFootprintResult().getManufacturingKgCO2e())
                .transportKgCO2e(g.getFootprintResult().getTransportKgCO2e())
                .primaryEmissionDriver(g.getFootprintResult().getPrimaryEmissionDriver())
                .calculationMethod(g.getFootprintResult().getCalculationMethod())
                .calculatedAt(g.getFootprintResult().getCalculatedAt())
                .build()
            : null;

    return GarmentResponse.builder()
        .id(g.getId())
        .sku(g.getSku())
        .name(g.getName())
        .category(g.getCategory())
        .weightKg(g.getWeightKg())
        .productionInfo(prodDto)
        .materials(matDtos)
        .footprint(fpResponse)
        .createdAt(g.getCreatedAt())
        .updatedAt(g.getUpdatedAt())
        .build();
  }
}
