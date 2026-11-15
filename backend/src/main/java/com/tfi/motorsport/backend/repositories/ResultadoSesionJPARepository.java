package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.ResultadoEtapa;
import com.tfi.motorsport.backend.model.ResultadoSesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultadoSesionJPARepository extends JpaRepository<ResultadoSesion, Long> {

    Optional<ResultadoSesion> findBySesionIdAndPilotoId(Long sesionId, Long pilotoId);

    /** Resultados existentes de una sesión (la sincronización los carga de una sola vez). */
    List<ResultadoSesion> findBySesionId(Long sesionId);

    @Query("""
            select r from ResultadoSesion r join fetch r.piloto join fetch r.equipo
            where r.sesion.id = :sesionId and r.eliminado = false
            order by case when r.posicion is null then 1 else 0 end, r.posicion
            """)
    List<ResultadoSesion> tablaDeSesion(Long sesionId);
    
    long countByPilotoIdAndEliminadoFalse(Long pilotoId);

    long countByEquipoIdAndEliminadoFalse(Long equipoId);

}
