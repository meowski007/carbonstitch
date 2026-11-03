package com.carbonstitch.footprint.service;

import com.carbonstitch.footprint.dto.response.BatchJobResponse;
import com.carbonstitch.footprint.dto.response.JobStatusResponse;
import com.carbonstitch.footprint.exception.ResourceNotFoundException;
import com.carbonstitch.footprint.model.entity.CalculationJob;
import com.carbonstitch.footprint.model.enums.JobStatus;
import com.carbonstitch.footprint.repository.CalculationJobRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchCalculationService {

  private final CalculationJobRepository jobRepository;
  private final CalculationService calculationService;

  @Transactional
  public BatchJobResponse submitBatch(List<Long> garmentIds) {
    String jobId = UUID.randomUUID().toString();
    CalculationJob job =
        CalculationJob.builder()
            .id(jobId)
            .status(JobStatus.PENDING)
            .totalItems(garmentIds.size())
            .processedItems(0)
            .createdAt(Instant.now())
            .build();

    jobRepository.save(job);
    log.info("Enqueued batch calculation job {} for {} garments", jobId, garmentIds.size());

    // Trigger async processing on custom ThreadPoolTaskExecutor
    processBatchAsync(jobId, garmentIds);

    return BatchJobResponse.builder()
        .jobId(jobId)
        .status(JobStatus.PENDING)
        .totalGarments(garmentIds.size())
        .statusUrl("/api/v1/jobs/" + jobId)
        .message("Batch footprint calculation job accepted and processing in background")
        .build();
  }

  @Async("calculationTaskExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CompletableFuture<Void> processBatchAsync(String jobId, List<Long> garmentIds) {
    log.info("Starting asynchronous execution of batch job {}", jobId);
    CalculationJob job =
        jobRepository
            .findById(jobId)
            .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));

    job.setStatus(JobStatus.PROCESSING);
    jobRepository.save(job);

    int processed = 0;
    try {
      for (Long garmentId : garmentIds) {
        try {
          calculationService.calculateAndSaveFootprint(garmentId);
        } catch (Exception ex) {
          log.warn(
              "Error calculating footprint for garment ID {} in job {}: {}",
              garmentId,
              jobId,
              ex.getMessage());
        }
        processed++;
        job.setProcessedItems(processed);
        jobRepository.saveAndFlush(job);
      }

      job.setStatus(JobStatus.COMPLETED);
      job.setCompletedAt(Instant.now());
      jobRepository.save(job);
      log.info(
          "Successfully completed batch job {} (processed {}/{} items)",
          jobId,
          processed,
          garmentIds.size());
    } catch (Exception ex) {
      log.error("Fatal failure in batch job {}: {}", jobId, ex.getMessage(), ex);
      job.setStatus(JobStatus.FAILED);
      job.setErrorMessage(ex.getMessage());
      job.setCompletedAt(Instant.now());
      jobRepository.save(job);
    }

    return CompletableFuture.completedFuture(null);
  }

  @Transactional(readOnly = true)
  public JobStatusResponse getJobStatus(String jobId) {
    CalculationJob job =
        jobRepository
            .findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));

    double percent =
        job.getTotalItems() > 0
            ? Math.round(((double) job.getProcessedItems() / job.getTotalItems()) * 10000.0) / 100.0
            : 0.0;

    return JobStatusResponse.builder()
        .jobId(job.getId())
        .status(job.getStatus())
        .totalGarments(job.getTotalItems())
        .processedGarments(job.getProcessedItems())
        .progressPercentage(percent)
        .errorMessage(job.getErrorMessage())
        .createdAt(job.getCreatedAt())
        .completedAt(job.getCompletedAt())
        .build();
  }
}
