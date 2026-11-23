package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.ResultadoEtapa;
import com.tfi.motorsport.backend.repositories.unificacion.FilaResultado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultadoEtapaJPARepository extends JpaRepository<ResultadoEtapa, Long> {

    Optional<ResultadoEtapa> findByEtapaIdAndTripulacionId(Long etapaId, Long tripulacionId);

    List<ResultadoEtapa> findByEtapaId(Long etapaId);

    @Query("""
            select r from ResultadoEtapa r join fetch r.tripulacion tr join fetch tr.piloto
                 join fetch tr.copiloto join fetch tr.equipo
            where r.etapa.id = :etapaId and r.eliminado = false
            order by case when r.posicion is null then 1 else 0 end, r.posicion
            """)
    List<ResultadoEtapa> tablaDeEtapa(Long etapaId);

    /*
     * Manejo identico a Resultado Sesion
     * Para estadísticas solo interesan la clasificación GENERAL del rally
     * (posición final y puntos) y las victorias de tramo (posición 1).
     * Así se evita traer miles de filas de tiempos de tramo.
     */
    String SELECT_FILA = """
            select new com.tfi.motorsport.backend.repositories.unificacion.FilaResultado(
                e.id, e.numeroFecha, coalesce(e.nombreCorto, e.nombre), e.fechaInicio, t.id, t.anio, c.siglas,
                p.id,p.nombre, p.apellido, p.codigoPiloto, eq.id, eq.nombre,eq.nombreCorto,
                case when et.idExterno = 'GENERAL' then 'GENERAL' else 'ETAPA' end,
                r.posicion, r.puntos, r.estado)
                from ResultadoEtapa r join r.etapa et join et.evento e join e.temporada t join t.categoria c join r.tripulacion tr 
                join tr.piloto p join tr.equipo eq
            """;

    String FILTRO_RELEVANTE = " and r.eliminado = false and et.eliminado = false and e.eliminado = false"
            + " and t.eliminado = false and tr.eliminado = false and (et.idExterno = 'GENERAL' or r.posicion = 1)";

    @Query(SELECT_FILA + "where t.id = :temporadaId" + FILTRO_RELEVANTE)
    List<FilaResultado> filasDeTemporada(Long temporadaId);

    @Query(SELECT_FILA + "where t.categoria.id = :categoriaId" + FILTRO_RELEVANTE)
    List<FilaResultado> filasDeCategoria(Long categoriaId);

    @Query(SELECT_FILA + "where p.id= :pilotoId" + FILTRO_RELEVANTE)
    List<FilaResultado> filasDePiloto(Long pilotoId);

    @Query(SELECT_FILA + " where eq.id = :equipoId" + FILTRO_RELEVANTE)
    List<FilaResultado> filasDeEquipo(Long equipoId);

    long countByTripulacionIdAndEliminadoFalse(Long tripulacionId);


}
