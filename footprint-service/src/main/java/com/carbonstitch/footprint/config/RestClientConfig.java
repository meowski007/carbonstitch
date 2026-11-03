package com.carbonstitch.footprint.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  @Value("${app.ml-service.url:http://localhost:8000}")
  private String mlServiceUrl;

  @Value("${app.ml-service.connect-timeout-ms:2000}")
  private int connectTimeoutMs;

  @Value("${app.ml-service.read-timeout-ms:3000}")
  private int readTimeoutMs;

  @Bean
  public RestClient mlRestClient() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
    factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

    return RestClient.builder().baseUrl(mlServiceUrl).requestFactory(factory).build();
  }
}
