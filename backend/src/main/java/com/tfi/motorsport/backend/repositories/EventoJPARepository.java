package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Evento;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventoJPARepository extends JpaRepository<Evento, Long> {

    Optional<Evento> findByTemporadaIdAndIdExterno(Long temporadaId, String idExterno);

    /** Eventos de una temporada con su temporada y categoría ya cargadas (evita consultas N+1). */
    @Query("""
            select e from Evento e join fetch e.temporada t join fetch t.categoria
            where t.id = :temporadaId and e.eliminado = false
            order by e.numeroFecha, e.fechaInicio
            """)
    List<Evento> listarPorTemporada(Long temporadaId);

    /**
     * Calendario unificado: eventos que se superponen con el rango [desde, hasta]
     * de las categorías indicadas.
     */
    @Query("""
            select e from Evento e join fetch e.temporada t join fetch t.categoria c
            where e.eliminado = false and t.eliminado = false and c.id in :categoriaIds
              and e.fechaInicio <= :hasta and coalesce(e.fechaFin, e.fechaInicio) >= :desde
            order by e.fechaInicio
            """)
    List<Evento> calendario(Instant desde, Instant hasta, Collection<Long> categoriaIds);

    /** Próximos eventos (o en curso) a partir de un instante. */
    @Query("""
            select e from Evento e join fetch e.temporada t join fetch t.categoria c
            where e.eliminado = false and t.eliminado = false and c.activo = true
              and coalesce(e.fechaFin, e.fechaInicio) >= :desde
            order by e.fechaInicio
            """)
    List<Evento> proximos(Instant desde, Pageable pageable);

    /** Últimos eventos finalizados (para la portada estilo "resultados recientes"). */
    @Query("""
            select e from Evento e join fetch e.temporada t join fetch t.categoria c
            where e.eliminado = false and t.eliminado = false and c.activo = true and e.estado = 'FINALIZADO'
            order by e.fechaInicio desc
            """)
    List<Evento> recientes(Pageable pageable);

    @Query("select e from Evento e join fetch e.temporada t join fetch t.categoria where e.id = :id and e.eliminado = false")
    Optional<Evento> buscarConCategoria(Long id);
}
