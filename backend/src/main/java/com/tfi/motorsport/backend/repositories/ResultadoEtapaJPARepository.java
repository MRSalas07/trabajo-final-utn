package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.ResultadoEtapa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultadoEtapaJPARepository extends JpaRepository<ResultadoEtapa, Long> {

    Optional<ResultadoEtapa> findByEtapaIdAndTripulacionId(Long etapaId, Long tripulacionId);

    /** Resultados existentes de una etapa (la sincronización los carga de una sola vez). */
    List<ResultadoEtapa> findByEtapaId(Long etapaId);

    @Query("""
            select r from ResultadoEtapa r join fetch r.tripulacion tr join fetch tr.piloto
                 join fetch tr.copiloto join fetch tr.equipo
            where r.etapa.id = :etapaId and r.eliminado = false
            order by case when r.posicion is null then 1 else 0 end, r.posicion
            """)
    List<ResultadoEtapa> tablaDeEtapa(Long etapaId);

}
