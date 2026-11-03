package com.carbonstitch.footprint.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bulk asynchronous footprint calculation request")
public class BatchCalculationRequest {

  @NotEmpty(message = "Garment ID list cannot be empty")
  @Size(max = 1000, message = "A single batch cannot exceed 1000 garments")
  @Schema(example = "[1, 2, 3, 4, 5]", description = "List of garment catalog IDs to calculate")
  private List<Long> garmentIds;
}
