package com.carbonstitch.footprint.service;

import com.carbonstitch.footprint.model.entity.EmissionFactor;
import com.carbonstitch.footprint.model.enums.FactorType;
import com.carbonstitch.footprint.model.enums.TransportMode;
import com.carbonstitch.footprint.repository.EmissionFactorRepository;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmissionFactorService {

  private final EmissionFactorRepository repository;

  // Fallback defaults for missing codes
  private static final BigDecimal DEFAULT_MATERIAL_FACTOR = new BigDecimal("8.0000");
  private static final BigDecimal DEFAULT_GRID_FACTOR = new BigDecimal("4.5000");
  private static final BigDecimal DEFAULT_TRANSPORT_FACTOR =
      new BigDecimal("0.0150"); // Sea freight default

  @Cacheable(value = "emissionFactors", key = "#material.toUpperCase()")
  @Transactional(readOnly = true)
  public BigDecimal getMaterialFactor(String material) {
    log.debug("Cache miss for material emission factor: {}", material);
    String key = material != null ? material.trim().toUpperCase() : "OTHER";
    Optional<EmissionFactor> factor =
        repository.findByFactorTypeAndFactorKey(FactorType.MATERIAL, key);
    return factor
        .map(EmissionFactor::getFactorValue)
        .orElseGet(
            () -> {
              log.warn("Material factor not found for '{}', applying generic default", key);
              return DEFAULT_MATERIAL_FACTOR;
            });
  }

  @Cacheable(value = "gridFactors", key = "#country.toUpperCase()")
  @Transactional(readOnly = true)
  public BigDecimal getGridFactor(String country) {
    log.debug("Cache miss for grid emission factor: {}", country);
    String key = country != null ? country.trim().toUpperCase() : "DEFAULT";
    Optional<EmissionFactor> factor =
        repository.findByFactorTypeAndFactorKey(FactorType.COUNTRY_GRID, key);
    return factor
        .map(EmissionFactor::getFactorValue)
        .orElseGet(
            () -> {
              log.warn("Grid factor not found for country '{}', falling back to DEFAULT", key);
              return repository
                  .findByFactorTypeAndFactorKey(FactorType.COUNTRY_GRID, "DEFAULT")
                  .map(EmissionFactor::getFactorValue)
                  .orElse(DEFAULT_GRID_FACTOR);
            });
  }

  @Cacheable(value = "transportFactors", key = "#transportMode.name()")
  @Transactional(readOnly = true)
  public BigDecimal getTransportFactor(TransportMode transportMode) {
    log.debug("Cache miss for transport emission factor: {}", transportMode);
    String key = transportMode != null ? transportMode.name() : "SEA";
    Optional<EmissionFactor> factor =
        repository.findByFactorTypeAndFactorKey(FactorType.TRANSPORT, key);
    return factor.map(EmissionFactor::getFactorValue).orElse(DEFAULT_TRANSPORT_FACTOR);
  }
}
