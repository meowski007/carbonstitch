package com.carbonstitch.footprint.repository;

import com.carbonstitch.footprint.model.entity.FootprintResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FootprintResultRepository extends JpaRepository<FootprintResult, Long> {

  Optional<FootprintResult> findByGarmentId(Long garmentId);

  @Query(
      "SELECT r FROM FootprintResult r "
          + "JOIN FETCH r.garment g "
          + "LEFT JOIN FETCH g.materials "
          + "ORDER BY r.totalKgCO2e DESC")
  List<FootprintResult> findTopEmissionHotspots(Pageable pageable);
}
