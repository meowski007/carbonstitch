package com.carbonstitch.footprint.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carbonstitch.footprint.dto.client.MlPredictRequest;
import com.carbonstitch.footprint.dto.client.MlPredictResponse;
import com.carbonstitch.footprint.dto.response.FootprintResponse;
import com.carbonstitch.footprint.exception.ResourceNotFoundException;
import com.carbonstitch.footprint.model.entity.FootprintResult;
import com.carbonstitch.footprint.model.entity.Garment;
import com.carbonstitch.footprint.model.entity.MaterialComposition;
import com.carbonstitch.footprint.model.entity.ProductionInfo;
import com.carbonstitch.footprint.model.enums.CalculationMethod;
import com.carbonstitch.footprint.model.enums.GarmentCategory;
import com.carbonstitch.footprint.model.enums.TransportMode;
import com.carbonstitch.footprint.repository.FootprintResultRepository;
import com.carbonstitch.footprint.repository.GarmentRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {

  @Mock private GarmentRepository garmentRepository;
  @Mock private FootprintResultRepository footprintRepository;
  @Mock private EmissionFactorService factorService;
  @Mock private MlServiceClient mlServiceClient;

  @InjectMocks private CalculationService calculationService;

  private Garment sampleGarment;

  @BeforeEach
  void setUp() {
    sampleGarment =
        Garment.builder()
            .id(1L)
            .sku("TEST-TEE-001")
            .name("Organic Cotton Tee")
            .category(GarmentCategory.T_SHIRT)
            .weightKg(new BigDecimal("0.200"))
            .productionInfo(
                ProductionInfo.builder()
                    .country("BD")
                    .transportMode(TransportMode.SEA)
                    .distanceKm(new BigDecimal("8000.00"))
                    .build())
            .materials(new ArrayList<>())
            .build();
  }

  @Test
  @DisplayName("Should calculate rule-based footprint when material composition is complete")
  void testCalculateRuleBased_CompleteMaterials() {
    sampleGarment.addMaterial(
        MaterialComposition.builder()
            .material("COTTON")
            .percentage(new BigDecimal("100.00"))
            .build());

    when(garmentRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(sampleGarment));
    when(factorService.getMaterialFactor("COTTON")).thenReturn(new BigDecimal("8.5000"));
    when(factorService.getGridFactor("BD")).thenReturn(new BigDecimal("4.8000"));
    when(factorService.getTransportFactor(TransportMode.SEA)).thenReturn(new BigDecimal("0.0150"));
    when(footprintRepository.findByGarmentId(1L)).thenReturn(Optional.empty());
    when(footprintRepository.save(any(FootprintResult.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    FootprintResponse response = calculationService.calculateAndSaveFootprint(1L);

    assertThat(response).isNotNull();
    assertThat(response.getCalculationMethod()).isEqualTo(CalculationMethod.RULE_BASED);
    assertThat(response.getPrimaryEmissionDriver()).isEqualTo("COTTON");

    // Expected breakdown:
    // Material: 0.200 * 1.0 * 8.5 = 1.7000
    // Mfg: 0.200 * 4.8 = 0.9600
    // Transport: (0.200 / 1000) * 8000 * 0.015 = 0.0240
    // Total = 2.6840
    assertThat(response.getMaterialKgCO2e()).isEqualByComparingTo("1.7000");
    assertThat(response.getManufacturingKgCO2e()).isEqualByComparingTo("0.9600");
    assertThat(response.getTransportKgCO2e()).isEqualByComparingTo("0.0240");
    assertThat(response.getTotalKgCO2e()).isEqualByComparingTo("2.6840");
  }

  @Test
  @DisplayName("Should invoke ML service when material composition is missing")
  void testCalculate_MissingMaterials_CallsMlService() {
    // Garment has no material composition
    when(garmentRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(sampleGarment));
    when(footprintRepository.findByGarmentId(1L)).thenReturn(Optional.empty());
    when(footprintRepository.save(any(FootprintResult.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MlPredictResponse mlResponse =
        MlPredictResponse.builder()
            .estimatedKgCO2e(new BigDecimal("3.2000"))
            .modelName("RandomForestRegressor")
            .modelVersion("1.0.0")
            .build();

    when(mlServiceClient.predictFootprint(any(MlPredictRequest.class)))
        .thenReturn(Optional.of(mlResponse));

    FootprintResponse response = calculationService.calculateAndSaveFootprint(1L);

    assertThat(response).isNotNull();
    assertThat(response.getCalculationMethod()).isEqualTo(CalculationMethod.ML_PREDICTION);
    assertThat(response.getTotalKgCO2e()).isEqualByComparingTo("3.2000");
    assertThat(response.getPrimaryEmissionDriver()).contains("ML_ESTIMATE");
    verify(mlServiceClient).predictFootprint(any(MlPredictRequest.class));
  }

  @Test
  @DisplayName("Should gracefully fall back to hybrid rule-based estimate when ML service is down")
  void testCalculate_MlServiceDown_Fallback() {
    // Garment has no material composition
    when(garmentRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(sampleGarment));
    when(footprintRepository.findByGarmentId(1L)).thenReturn(Optional.empty());
    when(footprintRepository.save(any(FootprintResult.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    when(mlServiceClient.predictFootprint(any(MlPredictRequest.class)))
        .thenReturn(Optional.empty());
    when(factorService.getMaterialFactor("OTHER")).thenReturn(new BigDecimal("8.0000"));
    when(factorService.getGridFactor("BD")).thenReturn(new BigDecimal("4.8000"));
    when(factorService.getTransportFactor(TransportMode.SEA)).thenReturn(new BigDecimal("0.0150"));

    FootprintResponse response = calculationService.calculateAndSaveFootprint(1L);

    assertThat(response).isNotNull();
    assertThat(response.getCalculationMethod()).isEqualTo(CalculationMethod.HYBRID_FALLBACK);
    assertThat(response.getPrimaryEmissionDriver()).isEqualTo("ESTIMATED_GENERIC_FIBER");
    assertThat(response.getTotalKgCO2e()).isGreaterThan(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException for unknown garment ID")
  void testCalculate_NotFound() {
    when(garmentRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> calculationService.calculateAndSaveFootprint(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("999");
  }
}
