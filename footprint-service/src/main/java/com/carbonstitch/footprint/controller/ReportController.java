package com.carbonstitch.footprint.controller;

import com.carbonstitch.footprint.dto.response.HotspotReportResponse;
import com.carbonstitch.footprint.service.GarmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(
    name = "Reports",
    description = "Carbon footprint analytics and supply-chain hotspot identification")
public class ReportController {

  private final GarmentService garmentService;

  @GetMapping("/hotspots")
  @Operation(
      summary = "Emission hotspots report",
      description =
          "Returns the highest-emission garments in the portfolio and the dominant material driving their carbon footprint")
  @ApiResponse(responseCode = "200", description = "Hotspot report generated successfully")
  public ResponseEntity<HotspotReportResponse> getHotspots(
      @Parameter(
              description = "Maximum number of highest-emitting garments to return (1-100)",
              example = "10")
          @RequestParam(defaultValue = "10")
          int limit) {
    return ResponseEntity.ok(garmentService.getHotspotsReport(limit));
  }
}
