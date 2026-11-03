package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.ResultadoEtapa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultadoEtapaJPARepository extends JpaRepository<ResultadoEtapa, Long> {
}
