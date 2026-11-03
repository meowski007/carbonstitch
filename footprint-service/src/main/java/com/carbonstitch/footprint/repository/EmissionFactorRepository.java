package com.carbonstitch.footprint.repository;

import com.carbonstitch.footprint.model.entity.EmissionFactor;
import com.carbonstitch.footprint.model.enums.FactorType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmissionFactorRepository extends JpaRepository<EmissionFactor, Long> {

  Optional<EmissionFactor> findByFactorTypeAndFactorKey(FactorType factorType, String factorKey);

  List<EmissionFactor> findByFactorType(FactorType factorType);
}
