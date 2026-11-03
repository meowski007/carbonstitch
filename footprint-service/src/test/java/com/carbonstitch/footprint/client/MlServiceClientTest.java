package com.carbonstitch.footprint.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.carbonstitch.footprint.dto.client.MlPredictRequest;
import com.carbonstitch.footprint.dto.client.MlPredictResponse;
import com.carbonstitch.footprint.service.MlServiceClient;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class MlServiceClientTest {

  private MockWebServer mockWebServer;
  private MlServiceClient mlServiceClient;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    RestClient restClient = RestClient.builder().baseUrl(mockWebServer.url("/").toString()).build();
    mlServiceClient = new MlServiceClient(restClient);
    ReflectionTestUtils.setField(mlServiceClient, "maxRetries", 1);
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  @DisplayName("Should parse successful prediction response from ML service")
  void testPredictFootprint_Success() throws InterruptedException {
    String jsonResponse =
        """
        {
          "estimated_kg_co2e": 4.1250,
          "model_version": "1.0.0",
          "model_name": "RandomForestRegressor"
        }
        """;

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(jsonResponse));

    MlPredictRequest request =
        MlPredictRequest.builder()
            .category("JEANS")
            .weightKg(new BigDecimal("0.750"))
            .mainMaterial("COTTON")
            .country("TR")
            .transportMode("SEA")
            .distanceKm(new BigDecimal("3500.00"))
            .build();

    Optional<MlPredictResponse> result = mlServiceClient.predictFootprint(request);

    assertThat(result).isPresent();
    assertThat(result.get().getEstimatedKgCO2e()).isEqualByComparingTo("4.1250");
    assertThat(result.get().getModelName()).isEqualTo("RandomForestRegressor");
    assertThat(result.get().getModelVersion()).isEqualTo("1.0.0");

    RecordedRequest recorded = mockWebServer.takeRequest();
    assertThat(recorded.getPath()).isEqualTo("/predict");
    assertThat(recorded.getMethod()).isEqualTo("POST");
    assertThat(recorded.getBody().readUtf8()).contains("JEANS").contains("0.750");
  }

  @Test
  @DisplayName("Should gracefully return empty optional on 500 server error after retries")
  void testPredictFootprint_ServerError_Fallback() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));

    MlPredictRequest request =
        MlPredictRequest.builder()
            .category("T_SHIRT")
            .weightKg(new BigDecimal("0.200"))
            .mainMaterial("COTTON")
            .country("BD")
            .transportMode("SEA")
            .distanceKm(new BigDecimal("8000.00"))
            .build();

    Optional<MlPredictResponse> result = mlServiceClient.predictFootprint(request);

    assertThat(result).isEmpty();
    assertThat(mockWebServer.getRequestCount()).isEqualTo(2); // 1 initial + 1 retry
  }
}
