package com.carbonstitch.footprint.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.carbonstitch.footprint.dto.request.GarmentCreateRequest;
import com.carbonstitch.footprint.dto.request.MaterialCompositionDto;
import com.carbonstitch.footprint.dto.request.ProductionInfoDto;
import com.carbonstitch.footprint.model.enums.GarmentCategory;
import com.carbonstitch.footprint.model.enums.TransportMode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GarmentValidationTest {

  private static Validator validator;

  @BeforeAll
  static void setupValidator() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("Valid GarmentCreateRequest should have zero constraint violations")
  void testValidGarmentRequest() {
    GarmentCreateRequest request =
        GarmentCreateRequest.builder()
            .sku("TEE-VALID-01")
            .name("Valid Cotton Tee")
            .category(GarmentCategory.T_SHIRT)
            .weightKg(new BigDecimal("0.250"))
            .productionInfo(
                ProductionInfoDto.builder()
                    .country("PT")
                    .transportMode(TransportMode.ROAD)
                    .distanceKm(new BigDecimal("1500.00"))
                    .build())
            .materials(
                List.of(
                    MaterialCompositionDto.builder()
                        .material("COTTON")
                        .percentage(new BigDecimal("100.00"))
                        .build()))
            .build();

    Set<ConstraintViolation<GarmentCreateRequest>> violations = validator.validate(request);
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("Should detect invalid weight and missing SKU")
  void testInvalidGarmentFields() {
    GarmentCreateRequest request =
        GarmentCreateRequest.builder()
            .sku("") // Blank SKU
            .name("Bad Tee")
            .category(GarmentCategory.T_SHIRT)
            .weightKg(new BigDecimal("0.001")) // Below 0.010 kg minimum
            .productionInfo(
                ProductionInfoDto.builder()
                    .country("PORTUGAL") // Exceeds 2 chars
                    .transportMode(TransportMode.AIR)
                    .distanceKm(new BigDecimal("-50.00")) // Negative distance
                    .build())
            .build();

    Set<ConstraintViolation<GarmentCreateRequest>> violations = validator.validate(request);
    assertThat(violations).isNotEmpty();
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("sku"));
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("weightKg"));
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("productionInfo.country"));
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("productionInfo.distanceKm"));
  }

  @Test
  @DisplayName("Should detect invalid material percentage")
  void testInvalidMaterialPercentage() {
    MaterialCompositionDto invalidDto =
        MaterialCompositionDto.builder()
            .material("POLYESTER")
            .percentage(new BigDecimal("120.00")) // Exceeds 100
            .build();

    Set<ConstraintViolation<MaterialCompositionDto>> violations = validator.validate(invalidDto);
    assertThat(violations).isNotEmpty();
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("percentage"));
  }
}
