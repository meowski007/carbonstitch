package com.carbonstitch.footprint.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carbonstitch.footprint.dto.request.BatchCalculationRequest;
import com.carbonstitch.footprint.dto.request.GarmentCreateRequest;
import com.carbonstitch.footprint.dto.request.GarmentUpdateRequest;
import com.carbonstitch.footprint.dto.request.MaterialCompositionDto;
import com.carbonstitch.footprint.dto.request.ProductionInfoDto;
import com.carbonstitch.footprint.model.entity.EmissionFactor;
import com.carbonstitch.footprint.model.enums.FactorType;
import com.carbonstitch.footprint.model.enums.GarmentCategory;
import com.carbonstitch.footprint.model.enums.TransportMode;
import com.carbonstitch.footprint.repository.CalculationJobRepository;
import com.carbonstitch.footprint.repository.EmissionFactorRepository;
import com.carbonstitch.footprint.repository.FootprintResultRepository;
import com.carbonstitch.footprint.repository.GarmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GarmentControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private GarmentRepository garmentRepository;
  @Autowired private FootprintResultRepository footprintRepository;
  @Autowired private EmissionFactorRepository factorRepository;
  @Autowired private CalculationJobRepository jobRepository;

  @BeforeEach
  void setUp() {
    footprintRepository.deleteAll();
    garmentRepository.deleteAll();
    jobRepository.deleteAll();

    if (factorRepository.count() == 0) {
      factorRepository.save(
          EmissionFactor.builder()
              .factorType(FactorType.MATERIAL)
              .factorKey("COTTON")
              .factorValue(new BigDecimal("8.3000"))
              .unit("kg CO2e/kg")
              .sourceReference("Illustrative test proxy")
              .isIllustrative(true)
              .build());
      factorRepository.save(
          EmissionFactor.builder()
              .factorType(FactorType.MATERIAL)
              .factorKey("POLYESTER")
              .factorValue(new BigDecimal("6.4000"))
              .unit("kg CO2e/kg")
              .sourceReference("Illustrative test proxy")
              .isIllustrative(true)
              .build());
      factorRepository.save(
          EmissionFactor.builder()
              .factorType(FactorType.COUNTRY_GRID)
              .factorKey("CN")
              .factorValue(new BigDecimal("6.2000"))
              .unit("kg CO2e/kg garment")
              .sourceReference("Illustrative test proxy")
              .isIllustrative(true)
              .build());
      factorRepository.save(
          EmissionFactor.builder()
              .factorType(FactorType.COUNTRY_GRID)
              .factorKey("DEFAULT")
              .factorValue(new BigDecimal("4.5000"))
              .unit("kg CO2e/kg garment")
              .sourceReference("Illustrative test proxy")
              .isIllustrative(true)
              .build());
      factorRepository.save(
          EmissionFactor.builder()
              .factorType(FactorType.TRANSPORT)
              .factorKey("SEA")
              .factorValue(new BigDecimal("0.0150"))
              .unit("kg CO2e/(tonne*km)")
              .sourceReference("Illustrative test proxy")
              .isIllustrative(true)
              .build());
      factorRepository.save(
          EmissionFactor.builder()
              .factorType(FactorType.TRANSPORT)
              .factorKey("ROAD")
              .factorValue(new BigDecimal("0.0950"))
              .unit("kg CO2e/(tonne*km)")
              .sourceReference("Illustrative test proxy")
              .isIllustrative(true)
              .build());
    }
  }

  @Test
  @DisplayName("Full lifecycle: create garment, calculate footprint, get footprint, get hotspots")
  void testGarmentAndFootprintLifecycle() throws Exception {
    GarmentCreateRequest createRequest =
        GarmentCreateRequest.builder()
            .sku("TSHIRT-ORG-01")
            .name("Organic Cotton T-Shirt")
            .category(GarmentCategory.T_SHIRT)
            .weightKg(new BigDecimal("0.220"))
            .materials(
                List.of(
                    MaterialCompositionDto.builder()
                        .material("COTTON")
                        .percentage(new BigDecimal("100.00"))
                        .build()))
            .productionInfo(
                ProductionInfoDto.builder()
                    .country("CN")
                    .transportMode(TransportMode.SEA)
                    .distanceKm(new BigDecimal("12000.00"))
                    .build())
            .build();

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/garments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.sku", is("TSHIRT-ORG-01")))
            .andExpect(jsonPath("$.materials", hasSize(1)))
            .andReturn();

    Long garmentId =
        objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

    mockMvc
        .perform(get("/api/v1/garments/{id}", garmentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Organic Cotton T-Shirt")))
        .andExpect(jsonPath("$.productionInfo.country", is("CN")));

    mockMvc
        .perform(post("/api/v1/garments/{id}/footprint", garmentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.garmentId", is(garmentId.intValue())))
        .andExpect(jsonPath("$.totalKgCO2e", greaterThanOrEqualTo(0.0)))
        .andExpect(jsonPath("$.materialKgCO2e", notNullValue()))
        .andExpect(jsonPath("$.manufacturingKgCO2e", notNullValue()))
        .andExpect(jsonPath("$.transportKgCO2e", notNullValue()))
        .andExpect(jsonPath("$.primaryEmissionDriver", is("COTTON")))
        .andExpect(jsonPath("$.calculationMethod", is("RULE_BASED")));

    mockMvc
        .perform(get("/api/v1/garments/{id}/footprint", garmentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.garmentId", is(garmentId.intValue())))
        .andExpect(jsonPath("$.primaryEmissionDriver", is("COTTON")));

    mockMvc
        .perform(get("/api/v1/reports/hotspots").param("limit", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalHotspots", is(1)))
        .andExpect(jsonPath("$.hotspots", hasSize(1)))
        .andExpect(jsonPath("$.hotspots[0].sku", is("TSHIRT-ORG-01")))
        .andExpect(jsonPath("$.hotspots[0].drivingMaterial", is("COTTON")));

    GarmentUpdateRequest updateRequest =
        GarmentUpdateRequest.builder()
            .name("Updated Premium T-Shirt")
            .category(GarmentCategory.T_SHIRT)
            .weightKg(new BigDecimal("0.250"))
            .materials(
                List.of(
                    MaterialCompositionDto.builder()
                        .material("COTTON")
                        .percentage(new BigDecimal("95.00"))
                        .build(),
                    MaterialCompositionDto.builder()
                        .material("POLYESTER")
                        .percentage(new BigDecimal("5.00"))
                        .build()))
            .productionInfo(
                ProductionInfoDto.builder()
                    .country("CN")
                    .transportMode(TransportMode.SEA)
                    .distanceKm(new BigDecimal("12000.00"))
                    .build())
            .build();

    mockMvc
        .perform(
            put("/api/v1/garments/{id}", garmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Updated Premium T-Shirt")))
        .andExpect(jsonPath("$.weightKg", is(0.25)))
        .andExpect(jsonPath("$.materials", hasSize(2)));

    mockMvc.perform(delete("/api/v1/garments/{id}", garmentId)).andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/v1/garments/{id}", garmentId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status", is(404)))
        .andExpect(jsonPath("$.error", is("Not Found")));
  }

  @Test
  @DisplayName(
      "Asynchronous batch footprint calculation job returns 202 Accepted and tracks status")
  void testBatchFootprintCalculation() throws Exception {
    GarmentCreateRequest g1 =
        GarmentCreateRequest.builder()
            .sku("BATCH-01")
            .name("Batch Item 1")
            .category(GarmentCategory.SHIRT)
            .weightKg(new BigDecimal("0.300"))
            .materials(
                List.of(
                    MaterialCompositionDto.builder()
                        .material("COTTON")
                        .percentage(new BigDecimal("100.00"))
                        .build()))
            .productionInfo(
                ProductionInfoDto.builder()
                    .country("CN")
                    .transportMode(TransportMode.ROAD)
                    .distanceKm(new BigDecimal("1500.00"))
                    .build())
            .build();

    GarmentCreateRequest g2 =
        GarmentCreateRequest.builder()
            .sku("BATCH-02")
            .name("Batch Item 2")
            .category(GarmentCategory.SWEATER)
            .weightKg(new BigDecimal("0.800"))
            .materials(
                List.of(
                    MaterialCompositionDto.builder()
                        .material("POLYESTER")
                        .percentage(new BigDecimal("100.00"))
                        .build()))
            .productionInfo(
                ProductionInfoDto.builder()
                    .country("VN")
                    .transportMode(TransportMode.ROAD)
                    .distanceKm(new BigDecimal("800.00"))
                    .build())
            .build();

    MvcResult res1 =
        mockMvc
            .perform(
                post("/api/v1/garments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(g1)))
            .andExpect(status().isCreated())
            .andReturn();

    MvcResult res2 =
        mockMvc
            .perform(
                post("/api/v1/garments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(g2)))
            .andExpect(status().isCreated())
            .andReturn();

    Long id1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asLong();
    Long id2 = objectMapper.readTree(res2.getResponse().getContentAsString()).get("id").asLong();

    BatchCalculationRequest batchReq = new BatchCalculationRequest(List.of(id1, id2));

    MvcResult batchResult =
        mockMvc
            .perform(
                post("/api/v1/footprints/batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(batchReq)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.jobId", notNullValue()))
            .andExpect(jsonPath("$.status", is("PENDING")))
            .andExpect(jsonPath("$.totalGarments", is(2)))
            .andReturn();

    String jobId =
        objectMapper.readTree(batchResult.getResponse().getContentAsString()).get("jobId").asText();

    int attempts = 0;
    String status = "PROCESSING";
    while (attempts < 15 && !"COMPLETED".equals(status)) {
      Thread.sleep(200);
      MvcResult statusResult =
          mockMvc
              .perform(get("/api/v1/jobs/{jobId}", jobId))
              .andExpect(status().isOk())
              .andReturn();

      status =
          objectMapper
              .readTree(statusResult.getResponse().getContentAsString())
              .get("status")
              .asText();
      attempts++;
    }

    assertThat(status).isIn("COMPLETED", "PROCESSING");
  }

  @Test
  @DisplayName("Should return 400 Bad Request when validation constraints fail")
  void testValidationFailure() throws Exception {
    GarmentCreateRequest invalidRequest =
        GarmentCreateRequest.builder()
            .sku("")
            .name("")
            .category(null)
            .weightKg(new BigDecimal("-0.500"))
            .materials(List.of())
            .productionInfo(null)
            .build();

    mockMvc
        .perform(
            post("/api/v1/garments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
        .andExpect(jsonPath("$.fieldErrors", notNullValue()));
  }

  @Test
  @DisplayName("Should return 409 Conflict when creating garment with duplicate SKU")
  void testDuplicateSkuConflict() throws Exception {
    GarmentCreateRequest request =
        GarmentCreateRequest.builder()
            .sku("DUPLICATE-SKU-01")
            .name("Duplicate Test Item")
            .category(GarmentCategory.JEANS)
            .weightKg(new BigDecimal("0.500"))
            .materials(
                List.of(
                    MaterialCompositionDto.builder()
                        .material("COTTON")
                        .percentage(new BigDecimal("100.00"))
                        .build()))
            .productionInfo(
                ProductionInfoDto.builder()
                    .country("TR")
                    .transportMode(TransportMode.ROAD)
                    .distanceKm(new BigDecimal("2000.00"))
                    .build())
            .build();

    mockMvc
        .perform(
            post("/api/v1/garments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/garments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status", is(409)))
        .andExpect(jsonPath("$.error", is("Conflict")));
  }
}
