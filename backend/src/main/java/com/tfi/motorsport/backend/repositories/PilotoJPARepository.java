package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Piloto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PilotoJPARepository extends JpaRepository<Piloto, Long> {
}
