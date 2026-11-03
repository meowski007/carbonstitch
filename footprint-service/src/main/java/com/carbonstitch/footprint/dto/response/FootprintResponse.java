package com.carbonstitch.footprint.dto.response;

import com.carbonstitch.footprint.model.enums.CalculationMethod;
import com.carbonstitch.footprint.model.enums.GarmentCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Calculated Life Cycle Assessment (LCA) carbon footprint result")
public class FootprintResponse {

  @Schema(example = "1", description = "Garment catalog ID")
  private Long garmentId;

  @Schema(example = "CS-TEE-001")
  private String sku;

  @Schema(example = "Classic Organic Crew Tee")
  private String garmentName;

  @Schema(example = "T_SHIRT")
  private GarmentCategory category;

  @Schema(example = "0.220", description = "Weight in kg")
  private BigDecimal weightKg;

  @Schema(example = "2.3450", description = "Total carbon footprint in kg CO2 equivalent")
  private BigDecimal totalKgCO2e;

  @Schema(example = "0.9240", description = "Scope 3 raw material production emissions")
  private BigDecimal materialKgCO2e;

  @Schema(example = "1.0560", description = "Scope 1 & 2 cut-and-sew manufacturing emissions")
  private BigDecimal manufacturingKgCO2e;

  @Schema(example = "0.3650", description = "Scope 3 inbound logistics transport emissions")
  private BigDecimal transportKgCO2e;

  @Schema(example = "COTTON", description = "Dominant emission driver")
  private String primaryEmissionDriver;

  @Schema(
      example = "RULE_BASED",
      description = "Method: RULE_BASED, ML_PREDICTION, HYBRID_FALLBACK")
  private CalculationMethod calculationMethod;

  @Schema(description = "Timestamp when calculation was performed")
  private Instant calculatedAt;
}
