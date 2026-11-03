package com.carbonstitch.footprint.dto.response;

import com.carbonstitch.footprint.model.enums.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Acknowledgement response for asynchronous batch calculation job")
public class BatchJobResponse {

  @Schema(example = "550e8400-e29b-41d4-a716-446655440000", description = "Unique tracking job ID")
  private String jobId;

  @Schema(example = "PENDING")
  private JobStatus status;

  @Schema(example = "25", description = "Total number of garments queued for calculation")
  private int totalGarments;

  @Schema(example = "/api/v1/jobs/550e8400-e29b-41d4-a716-446655440000")
  private String statusUrl;

  @Schema(example = "Batch footprint calculation job accepted and processing in background")
  private String message;
}
