package com.biosphere.backend.repository;

import com.biosphere.backend.domain.TelemetryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelemetryRepository extends JpaRepository<TelemetryEntity, Long> {
    List<TelemetryEntity> findTop15ByOrderByTimestampDesc();
}