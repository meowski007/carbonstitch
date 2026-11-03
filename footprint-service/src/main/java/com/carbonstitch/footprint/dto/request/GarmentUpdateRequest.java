package com.carbonstitch.footprint.dto.request;

import com.carbonstitch.footprint.model.enums.GarmentCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload to update an existing garment")
public class GarmentUpdateRequest {

  @NotBlank(message = "Product name is required")
  @Size(max = 255, message = "Product name cannot exceed 255 characters")
  @Schema(example = "Classic Organic Crew Tee v2")
  private String name;

  @NotNull(message = "Category is required")
  private GarmentCategory category;

  @NotNull(message = "Weight in kilograms is required")
  @DecimalMin(value = "0.010", message = "Weight must be at least 10 grams (0.010 kg)")
  @DecimalMax(value = "20.000", message = "Weight cannot exceed 20 kg")
  private BigDecimal weightKg;

  @Valid
  @NotNull(message = "Production info is required")
  private ProductionInfoDto productionInfo;

  @Valid private List<MaterialCompositionDto> materials;
}
