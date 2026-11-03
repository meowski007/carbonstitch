package com.carbonstitch.footprint.dto.response;

import com.carbonstitch.footprint.model.enums.CalculationMethod;
import com.carbonstitch.footprint.model.enums.GarmentCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Carbon Hotspots Portfolio Report ranking highest-emitting garments")
public class HotspotReportResponse {

  @Schema(example = "10")
  private int requestedLimit;

  @Schema(example = "10")
  private int totalHotspots;

  private Instant generatedAt;

  private List<HotspotItem> hotspots;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Individual garment emission hotspot entry")
  public static class HotspotItem {

    @Schema(example = "1")
    private int rank;

    @Schema(example = "42")
    private Long garmentId;

    @Schema(example = "CS-JEANS-088")
    private String sku;

    @Schema(example = "Heavyweight Selvedge Denim")
    private String name;

    @Schema(example = "JEANS")
    private GarmentCategory category;

    @Schema(example = "0.850")
    private BigDecimal weightKg;

    @Schema(example = "14.8250")
    private BigDecimal totalKgCO2e;

    @Schema(example = "COTTON")
    private String drivingMaterial;

    @Schema(example = "RULE_BASED")
    private CalculationMethod calculationMethod;
  }
}
