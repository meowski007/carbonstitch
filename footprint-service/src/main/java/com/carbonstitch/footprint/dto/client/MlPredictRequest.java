package com.carbonstitch.footprint.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlPredictRequest {

  private String category;

  @JsonProperty("weight_kg")
  private BigDecimal weightKg;

  @JsonProperty("main_material")
  private String mainMaterial;

  private String country;

  @JsonProperty("transport_mode")
  private String transportMode;

  @JsonProperty("distance_km")
  private BigDecimal distanceKm;
}
