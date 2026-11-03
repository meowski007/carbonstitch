package com.carbonstitch.footprint.repository;

import com.carbonstitch.footprint.model.entity.Garment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GarmentRepository extends JpaRepository<Garment, Long> {

  Optional<Garment> findBySku(String sku);

  boolean existsBySku(String sku);

  @EntityGraph(attributePaths = {"materials", "footprintResult"})
  @Query("SELECT g FROM Garment g WHERE g.id = :id")
  Optional<Garment> findByIdWithDetails(@Param("id") Long id);

  @EntityGraph(attributePaths = {"materials", "footprintResult"})
  List<Garment> findByIdIn(Collection<Long> ids);
}
