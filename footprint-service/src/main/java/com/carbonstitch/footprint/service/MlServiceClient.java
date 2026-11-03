package com.carbonstitch.footprint.service;

import com.carbonstitch.footprint.dto.client.MlPredictRequest;
import com.carbonstitch.footprint.dto.client.MlPredictResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MlServiceClient {

  private final RestClient mlRestClient;

  @Value("${app.ml-service.max-retries:2}")
  private int maxRetries;

  /**
   * Request predicted footprint from Python ML service with retry and graceful fallback.
   *
   * @param request Garment features
   * @return Predicted estimate, or empty if ML service is unreachable/errored
   */
  public Optional<MlPredictResponse> predictFootprint(MlPredictRequest request) {
    int attempts = 0;
    while (attempts <= maxRetries) {
      attempts++;
      try {
        log.debug(
            "Sending prediction request to ML service (attempt {}/{}): {}",
            attempts,
            maxRetries + 1,
            request);
        MlPredictResponse response =
            mlRestClient
                .post()
                .uri("/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(MlPredictResponse.class);

        if (response != null && response.getEstimatedKgCO2e() != null) {
          log.info(
              "ML service prediction successful: {} kg CO2e (model: {}, v{})",
              response.getEstimatedKgCO2e(),
              response.getModelName(),
              response.getModelVersion());
          return Optional.of(response);
        }
      } catch (RestClientException ex) {
        log.warn(
            "ML service communication failed on attempt {}/{}: {}",
            attempts,
            maxRetries + 1,
            ex.getMessage());
        if (attempts <= maxRetries) {
          try {
            Thread.sleep(100L * attempts);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      } catch (Exception ex) {
        log.error("Unexpected error contacting ML service: {}", ex.getMessage(), ex);
        break;
      }
    }

    log.warn(
        "ML service unavailable or exhausted retries; falling back gracefully to rule-based estimation.");
    return Optional.empty();
  }
}
