package com.carbonstitch.footprint.dto.response;

import com.carbonstitch.footprint.model.enums.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Real-time status of an asynchronous calculation job")
public class JobStatusResponse {

  private String jobId;
  private JobStatus status;
  private int totalGarments;
  private int processedGarments;
  private double progressPercentage;
  private String errorMessage;
  private Instant createdAt;
  private Instant completedAt;
}
