package com.carbonstitch.footprint.controller;

import com.carbonstitch.footprint.dto.response.JobStatusResponse;
import com.carbonstitch.footprint.service.BatchCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Asynchronous batch calculation job status monitoring")
public class JobController {

  private final BatchCalculationService batchService;

  @GetMapping("/{jobId}")
  @Operation(
      summary = "Check batch job status",
      description =
          "Poll progress and execution status of an asynchronous bulk calculation job by its jobId")
  @ApiResponse(responseCode = "200", description = "Job status retrieved")
  @ApiResponse(responseCode = "404", description = "Job ID not found")
  public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
    return ResponseEntity.ok(batchService.getJobStatus(jobId));
  }
}
