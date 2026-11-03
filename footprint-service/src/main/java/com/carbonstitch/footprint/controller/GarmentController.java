package com.carbonstitch.footprint.controller;

import com.carbonstitch.footprint.dto.request.GarmentCreateRequest;
import com.carbonstitch.footprint.dto.request.GarmentUpdateRequest;
import com.carbonstitch.footprint.dto.response.GarmentResponse;
import com.carbonstitch.footprint.service.GarmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/garments")
@RequiredArgsConstructor
@Tag(name = "Garments", description = "Apparel product catalog CRUD management")
public class GarmentController {

  private final GarmentService garmentService;

  @PostMapping
  @Operation(
      summary = "Register new garment",
      description = "Create a new garment with materials and production parameters")
  @ApiResponse(responseCode = "201", description = "Garment registered successfully")
  @ApiResponse(responseCode = "400", description = "Validation constraint violation")
  @ApiResponse(responseCode = "409", description = "Garment with SKU already exists")
  public ResponseEntity<GarmentResponse> createGarment(
      @Valid @RequestBody GarmentCreateRequest request) {
    GarmentResponse response = garmentService.createGarment(request);
    return ResponseEntity.created(URI.create("/api/v1/garments/" + response.getId()))
        .body(response);
  }

  @GetMapping
  @Operation(
      summary = "List all garments",
      description = "Retrieve list of all registered garments in the catalog")
  public ResponseEntity<List<GarmentResponse>> listGarments() {
    return ResponseEntity.ok(garmentService.listGarments());
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get garment by ID",
      description = "Retrieve details and material breakdown for a specific garment")
  @ApiResponse(responseCode = "200", description = "Garment found")
  @ApiResponse(responseCode = "404", description = "Garment not found")
  public ResponseEntity<GarmentResponse> getGarment(@PathVariable Long id) {
    return ResponseEntity.ok(garmentService.getGarment(id));
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Update garment",
      description = "Update specification, materials or logistics for an existing garment")
  @ApiResponse(responseCode = "200", description = "Garment updated")
  @ApiResponse(responseCode = "404", description = "Garment not found")
  public ResponseEntity<GarmentResponse> updateGarment(
      @PathVariable Long id, @Valid @RequestBody GarmentUpdateRequest request) {
    return ResponseEntity.ok(garmentService.updateGarment(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Delete garment",
      description = "Remove garment and its associated footprint calculations")
  @ApiResponse(responseCode = "204", description = "Garment deleted")
  @ApiResponse(responseCode = "404", description = "Garment not found")
  public ResponseEntity<Void> deleteGarment(@PathVariable Long id) {
    garmentService.deleteGarment(id);
    return ResponseEntity.noContent().build();
  }
}
