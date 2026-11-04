package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SesionJPARepository extends JpaRepository<Sesion, Long> {
}
