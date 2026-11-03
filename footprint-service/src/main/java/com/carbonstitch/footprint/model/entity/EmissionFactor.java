package com.carbonstitch.footprint.model.entity;

import com.carbonstitch.footprint.model.enums.FactorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "emission_factors",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_factor_type_key",
          columnNames = {"factor_type", "factor_key"})
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmissionFactor {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "factor_type", nullable = false, length = 32)
  private FactorType factorType;

  @Column(name = "factor_key", nullable = false, length = 64)
  private String factorKey;

  @Column(name = "factor_value", nullable = false, precision = 10, scale = 4)
  private BigDecimal factorValue;

  @Column(name = "unit", nullable = false, length = 32)
  private String unit;

  @Column(name = "source_reference", length = 255)
  private String sourceReference;

  @Column(name = "is_illustrative", nullable = false)
  @Builder.Default
  private Boolean isIllustrative = true;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  @PreUpdate
  protected void onSave() {
    updatedAt = Instant.now();
  }
}
