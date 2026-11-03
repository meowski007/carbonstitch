package com.carbonstitch.footprint.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Garment fiber material composition entry")
public class MaterialCompositionDto {

  @NotBlank(message = "Material identifier is required (e.g., COTTON, POLYESTER, WOOL)")
  @Schema(example = "COTTON", description = "Material name or fiber code")
  private String material;

  @NotNull(message = "Percentage is required")
  @DecimalMin(value = "0.01", message = "Percentage must be greater than 0")
  @DecimalMax(value = "100.00", message = "Percentage cannot exceed 100")
  @Schema(example = "100.00", description = "Composition share (0-100%)")
  private BigDecimal percentage;
}
