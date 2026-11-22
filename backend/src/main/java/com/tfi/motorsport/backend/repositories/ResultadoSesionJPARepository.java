package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.ResultadoEtapa;
import com.tfi.motorsport.backend.model.ResultadoSesion;
import com.tfi.motorsport.backend.repositories.unificacion.FilaResultado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResultadoSesionJPARepository extends JpaRepository<ResultadoSesion, Long> {

    Optional<ResultadoSesion> findBySesionIdAndPilotoId(Long sesionId, Long pilotoId);

    @Query("""
            select r from ResultadoSesion r join fetch r.piloto join fetch r.equipo
            where r.sesion.id = :sesionId and r.eliminado = false
            order by case when r.posicion is null then 1 else 0 end, r.posicion
            """)
    List<ResultadoSesion> tablaDeSesion(Long sesionId);

    String SELECT_FILA = """
            select new com.tfi.motorsport.backend.repositories.unificacion.FilaResultado(
                e.id, e.numeroFecha, coalesce(e.nombreCorto, e.nombre), e.fechaInicio, t.id, t.anio, c.siglas,
                p.id, p.nombre, p.apellido, p.codigoPiloto, eq.id, eq.nombre,
                s.tipo, r.posicion, r.puntos, r.estado)
            from ResultadoSesion r join r.sesion s join s.evento e join e.temporada t join t.categoria c
                 join r.piloto p join r.equipo eq
            """;

    @Query(SELECT_FILA + " where t.id = :temporadaId and r.eliminado = false")
    List<FilaResultado> filasDeTemporada(Long temporadaId);

    @Query(SELECT_FILA + " where t.categoria.id = :categoriaId and r.eliminado = false")
    List<FilaResultado> filasDeCategoria(Long categoriaId);

    @Query(SELECT_FILA + " where p.id = :pilotoId and r.eliminado = false")
    List<FilaResultado> filasDePiloto(Long pilotoId);

    @Query(SELECT_FILA + " where eq.id = :equipoId and r.eliminado = false")
    List<FilaResultado> filasDeEquipo(Long equipoId);

    List<ResultadoSesion> findBySesionId(Long sesionId);

    long countByPilotoIdAndEliminadoFalse(Long pilotoId);

    long countByEquipoIdAndEliminadoFalse(Long equipoId);
}

