package com.carbonstitch.footprint.model.entity;

import com.carbonstitch.footprint.model.enums.CalculationMethod;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "footprint_results")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FootprintResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "garment_id", nullable = false, unique = true)
  @JsonBackReference
  private Garment garment;

  @Column(name = "total_kg_co2e", nullable = false, precision = 10, scale = 4)
  private BigDecimal totalKgCO2e;

  @Column(name = "material_kg_co2e", nullable = false, precision = 10, scale = 4)
  private BigDecimal materialKgCO2e;

  @Column(name = "manufacturing_kg_co2e", nullable = false, precision = 10, scale = 4)
  private BigDecimal manufacturingKgCO2e;

  @Column(name = "transport_kg_co2e", nullable = false, precision = 10, scale = 4)
  private BigDecimal transportKgCO2e;

  @Column(name = "primary_emission_driver", length = 64)
  private String primaryEmissionDriver;

  @Enumerated(EnumType.STRING)
  @Column(name = "calculation_method", nullable = false, length = 32)
  private CalculationMethod calculationMethod;

  @Column(name = "calculated_at", nullable = false)
  @Builder.Default
  private Instant calculatedAt = Instant.now();
}
