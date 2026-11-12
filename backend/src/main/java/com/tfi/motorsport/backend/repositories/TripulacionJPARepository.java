package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Tripulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripulacionJPARepository extends JpaRepository<Tripulacion, Long> {

    long countByPilotoIdAndEliminadoFalse(Long pilotoId);

    long countByCopilotoIdAndEliminadoFalse(Long pilotoId);

    long countByEquipoIdAndEliminadoFalse(Long equipoId);
}


