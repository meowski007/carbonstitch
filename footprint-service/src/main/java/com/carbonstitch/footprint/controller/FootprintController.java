package com.carbonstitch.footprint.controller;

import com.carbonstitch.footprint.dto.request.BatchCalculationRequest;
import com.carbonstitch.footprint.dto.response.BatchJobResponse;
import com.carbonstitch.footprint.dto.response.FootprintResponse;
import com.carbonstitch.footprint.service.BatchCalculationService;
import com.carbonstitch.footprint.service.CalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Footprints", description = "Carbon footprint calculation and batch processing")
public class FootprintController {

  private final CalculationService calculationService;
  private final BatchCalculationService batchService;

  @PostMapping("/api/v1/garments/{id}/footprint")
  @Operation(
      summary = "Calculate garment footprint",
      description =
          "Calculates LCA carbon footprint (kg CO2e) for a single garment using rule-based factors or ML fallback")
  @ApiResponse(responseCode = "200", description = "Footprint calculated successfully")
  @ApiResponse(responseCode = "404", description = "Garment not found")
  public ResponseEntity<FootprintResponse> calculateFootprint(@PathVariable Long id) {
    return ResponseEntity.ok(calculationService.calculateAndSaveFootprint(id));
  }

  @GetMapping("/api/v1/garments/{id}/footprint")
  @Operation(
      summary = "Get garment footprint",
      description = "Retrieves previously calculated carbon footprint results for a garment")
  @ApiResponse(responseCode = "200", description = "Footprint found")
  @ApiResponse(responseCode = "404", description = "Footprint not calculated yet")
  public ResponseEntity<FootprintResponse> getFootprint(@PathVariable Long id) {
    return ResponseEntity.ok(calculationService.getFootprint(id));
  }

  @PostMapping("/api/v1/footprints/batch")
  @Operation(
      summary = "Bulk asynchronous footprint calculation",
      description =
          "Enqueues a background job to calculate emissions for a collection of garments asynchronously. Returns 202 Accepted with tracking jobId.")
  @ApiResponse(responseCode = "202", description = "Batch calculation job enqueued")
  @ApiResponse(responseCode = "400", description = "Invalid batch request")
  public ResponseEntity<BatchJobResponse> submitBatchCalculation(
      @Valid @RequestBody BatchCalculationRequest request) {
    BatchJobResponse response = batchService.submitBatch(request.getGarmentIds());
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
  }
}
