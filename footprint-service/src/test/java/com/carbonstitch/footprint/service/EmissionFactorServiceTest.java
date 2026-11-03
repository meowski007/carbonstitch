package com.carbonstitch.footprint.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carbonstitch.footprint.model.entity.EmissionFactor;
import com.carbonstitch.footprint.model.enums.FactorType;
import com.carbonstitch.footprint.model.enums.TransportMode;
import com.carbonstitch.footprint.repository.EmissionFactorRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmissionFactorServiceTest {

  @Mock private EmissionFactorRepository repository;
  @InjectMocks private EmissionFactorService service;

  @Test
  @DisplayName("Should return known material emission factor")
  void testGetMaterialFactor_Known() {
    EmissionFactor cottonFactor =
        EmissionFactor.builder()
            .factorType(FactorType.MATERIAL)
            .factorKey("COTTON")
            .factorValue(new BigDecimal("8.5000"))
            .unit("kg CO2e/kg")
            .build();

    when(repository.findByFactorTypeAndFactorKey(FactorType.MATERIAL, "COTTON"))
        .thenReturn(Optional.of(cottonFactor));

    BigDecimal factor = service.getMaterialFactor("cotton");

    assertThat(factor).isEqualByComparingTo("8.5000");
    verify(repository).findByFactorTypeAndFactorKey(FactorType.MATERIAL, "COTTON");
  }

  @Test
  @DisplayName("Should return default factor for unknown material")
  void testGetMaterialFactor_Unknown() {
    when(repository.findByFactorTypeAndFactorKey(FactorType.MATERIAL, "UNKNOWN_FIBER"))
        .thenReturn(Optional.empty());

    BigDecimal factor = service.getMaterialFactor("UNKNOWN_FIBER");

    assertThat(factor).isEqualByComparingTo("8.0000"); // Default OTHER factor
  }

  @Test
  @DisplayName("Should return grid factor for known country code")
  void testGetGridFactor_Known() {
    EmissionFactor gridFactor =
        EmissionFactor.builder()
            .factorType(FactorType.COUNTRY_GRID)
            .factorKey("VN")
            .factorValue(new BigDecimal("4.4000"))
            .unit("kg CO2e/kg")
            .build();

    when(repository.findByFactorTypeAndFactorKey(FactorType.COUNTRY_GRID, "VN"))
        .thenReturn(Optional.of(gridFactor));

    BigDecimal factor = service.getGridFactor("VN");

    assertThat(factor).isEqualByComparingTo("4.4000");
  }

  @Test
  @DisplayName("Should return transport mode factor")
  void testGetTransportFactor() {
    EmissionFactor airFactor =
        EmissionFactor.builder()
            .factorType(FactorType.TRANSPORT)
            .factorKey("AIR")
            .factorValue(new BigDecimal("0.6020"))
            .unit("kg CO2e/t-km")
            .build();

    when(repository.findByFactorTypeAndFactorKey(FactorType.TRANSPORT, "AIR"))
        .thenReturn(Optional.of(airFactor));

    BigDecimal factor = service.getTransportFactor(TransportMode.AIR);

    assertThat(factor).isEqualByComparingTo("0.6020");
  }
}
