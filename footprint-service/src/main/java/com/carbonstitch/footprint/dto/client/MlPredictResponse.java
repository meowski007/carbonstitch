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
public class MlPredictResponse {

  @JsonProperty("estimated_kg_co2e")
  private BigDecimal estimatedKgCO2e;

  @JsonProperty("model_version")
  private String modelVersion;

  @JsonProperty("model_name")
  private String modelName;
}
