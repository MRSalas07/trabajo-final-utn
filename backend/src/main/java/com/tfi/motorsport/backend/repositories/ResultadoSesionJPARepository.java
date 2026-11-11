package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.ResultadoSesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultadoSesionJPARepository extends JpaRepository<ResultadoSesion, Long> {
}
