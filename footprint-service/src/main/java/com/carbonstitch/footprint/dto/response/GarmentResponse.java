package com.carbonstitch.footprint.dto.response;

import com.carbonstitch.footprint.dto.request.MaterialCompositionDto;
import com.carbonstitch.footprint.dto.request.ProductionInfoDto;
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
@Schema(description = "Garment entity response")
public class GarmentResponse {

  private Long id;
  private String sku;
  private String name;
  private GarmentCategory category;
  private BigDecimal weightKg;
  private ProductionInfoDto productionInfo;
  private List<MaterialCompositionDto> materials;
  private FootprintResponse footprint;
  private Instant createdAt;
  private Instant updatedAt;
}
