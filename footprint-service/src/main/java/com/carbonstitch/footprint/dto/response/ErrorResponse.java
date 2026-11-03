package com.carbonstitch.footprint.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized error response payload")
public class ErrorResponse {

  @Schema(example = "2026-11-03T05:30:00Z")
  private Instant timestamp;

  @Schema(example = "400")
  private int status;

  @Schema(example = "BAD_REQUEST")
  private String error;

  @Schema(example = "Invalid request payload")
  private String message;

  @Schema(example = "/api/v1/garments")
  private String path;

  @Schema(description = "Map of field name to validation error detail")
  private Map<String, String> fieldErrors;
}
