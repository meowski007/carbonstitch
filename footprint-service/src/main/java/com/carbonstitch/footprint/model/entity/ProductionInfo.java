package com.carbonstitch.footprint.model.entity;

import com.carbonstitch.footprint.model.enums.TransportMode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionInfo {

  @Column(name = "country", nullable = false, length = 2)
  private String country;

  @Enumerated(EnumType.STRING)
  @Column(name = "transport_mode", nullable = false, length = 32)
  private TransportMode transportMode;

  @Column(name = "distance_km", nullable = false, precision = 10, scale = 2)
  private BigDecimal distanceKm;
}
