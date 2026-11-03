package com.carbonstitch.footprint.dto.request;

import com.carbonstitch.footprint.model.enums.TransportMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Manufacturing facility and logistics specifications")
public class ProductionInfoDto {

  @NotBlank(message = "Country code is required")
  @Size(min = 2, max = 2, message = "Country code must be ISO 2-letter format (e.g. BD, VN, CN)")
  @Schema(example = "BD", description = "ISO 2-letter manufacturing origin country")
  private String country;

  @NotNull(message = "Transport mode is required")
  @Schema(example = "SEA", description = "Freight logistics mode: AIR, SEA, ROAD, RAIL")
  private TransportMode transportMode;

  @NotNull(message = "Distance in kilometers is required")
  @PositiveOrZero(message = "Distance cannot be negative")
  @Schema(example = "8200.00", description = "Total shipping distance in kilometers")
  private BigDecimal distanceKm;
}
