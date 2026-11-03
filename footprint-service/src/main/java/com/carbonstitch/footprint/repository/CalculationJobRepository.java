package com.carbonstitch.footprint.repository;

import com.carbonstitch.footprint.model.entity.CalculationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalculationJobRepository extends JpaRepository<CalculationJob, String> {}
