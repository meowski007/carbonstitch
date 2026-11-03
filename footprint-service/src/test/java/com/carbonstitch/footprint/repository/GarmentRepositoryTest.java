package com.carbonstitch.footprint.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.carbonstitch.footprint.model.entity.FootprintResult;
import com.carbonstitch.footprint.model.entity.Garment;
import com.carbonstitch.footprint.model.entity.MaterialComposition;
import com.carbonstitch.footprint.model.entity.ProductionInfo;
import com.carbonstitch.footprint.model.enums.CalculationMethod;
import com.carbonstitch.footprint.model.enums.GarmentCategory;
import com.carbonstitch.footprint.model.enums.TransportMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class GarmentRepositoryTest {

  @Autowired private GarmentRepository garmentRepository;
  @Autowired private FootprintResultRepository footprintRepository;

  @Test
  @DisplayName("Should persist garment with materials and fetch with details graph")
  void testSaveAndFindGarment() {
    Garment garment =
        Garment.builder()
            .sku("REPO-TEST-01")
            .name("Denim Jeans")
            .category(GarmentCategory.JEANS)
            .weightKg(new BigDecimal("0.750"))
            .productionInfo(
                ProductionInfo.builder()
                    .country("TR")
                    .transportMode(TransportMode.ROAD)
                    .distanceKm(new BigDecimal("2500.00"))
                    .build())
            .build();

    garment.addMaterial(
        MaterialComposition.builder()
            .material("COTTON")
            .percentage(new BigDecimal("98.00"))
            .build());
    garment.addMaterial(
        MaterialComposition.builder()
            .material("ELASTANE")
            .percentage(new BigDecimal("2.00"))
            .build());

    Garment saved = garmentRepository.save(garment);
    assertThat(saved.getId()).isNotNull();

    Optional<Garment> fetched = garmentRepository.findByIdWithDetails(saved.getId());
    assertThat(fetched).isPresent();
    assertThat(fetched.get().getMaterials()).hasSize(2);
    assertThat(fetched.get().getSku()).isEqualTo("REPO-TEST-01");
  }

  @Test
  @DisplayName("Should query emission hotspots ordered by total CO2e descending")
  void testFindTopEmissionHotspots() {
    Garment g1 =
        garmentRepository.save(
            Garment.builder()
                .sku("HOTSPOT-LOW")
                .name("Low Impact Tee")
                .category(GarmentCategory.T_SHIRT)
                .weightKg(new BigDecimal("0.150"))
                .productionInfo(
                    ProductionInfo.builder()
                        .country("PT")
                        .transportMode(TransportMode.ROAD)
                        .distanceKm(new BigDecimal("500.00"))
                        .build())
                .build());

    Garment g2 =
        garmentRepository.save(
            Garment.builder()
                .sku("HOTSPOT-HIGH")
                .name("High Impact Heavy Parka")
                .category(GarmentCategory.JACKET)
                .weightKg(new BigDecimal("1.800"))
                .productionInfo(
                    ProductionInfo.builder()
                        .country("CN")
                        .transportMode(TransportMode.AIR)
                        .distanceKm(new BigDecimal("9000.00"))
                        .build())
                .build());

    footprintRepository.save(
        FootprintResult.builder()
            .garment(g1)
            .totalKgCO2e(new BigDecimal("1.8500"))
            .materialKgCO2e(new BigDecimal("0.8000"))
            .manufacturingKgCO2e(new BigDecimal("0.9000"))
            .transportKgCO2e(new BigDecimal("0.1500"))
            .primaryEmissionDriver("COTTON")
            .calculationMethod(CalculationMethod.RULE_BASED)
            .calculatedAt(Instant.now())
            .build());

    footprintRepository.save(
        FootprintResult.builder()
            .garment(g2)
            .totalKgCO2e(new BigDecimal("28.4500"))
            .materialKgCO2e(new BigDecimal("14.0000"))
            .manufacturingKgCO2e(new BigDecimal("9.5000"))
            .transportKgCO2e(new BigDecimal("4.9500"))
            .primaryEmissionDriver("WOOL")
            .calculationMethod(CalculationMethod.RULE_BASED)
            .calculatedAt(Instant.now())
            .build());

    List<FootprintResult> hotspots =
        footprintRepository.findTopEmissionHotspots(PageRequest.of(0, 10));

    assertThat(hotspots).hasSize(2);
    // Highest emission first
    assertThat(hotspots.get(0).getGarment().getSku()).isEqualTo("HOTSPOT-HIGH");
    assertThat(hotspots.get(0).getTotalKgCO2e()).isEqualByComparingTo("28.4500");
    assertThat(hotspots.get(1).getGarment().getSku()).isEqualTo("HOTSPOT-LOW");
  }
}
