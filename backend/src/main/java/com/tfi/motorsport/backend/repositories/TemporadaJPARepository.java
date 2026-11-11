package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Temporada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemporadaJPARepository extends JpaRepository<Temporada, Long> {
}
