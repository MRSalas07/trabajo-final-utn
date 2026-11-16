package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SesionJPARepository extends JpaRepository<Sesion, Long> {
    Optional<Sesion> findByEventoIdAndIdExterno(Long eventoId, String idExterno);

    @Query("select s from Sesion s where s.evento.id in :eventoIds and s.eliminado = false order by s.fechaInicio, s.numero")
    List<Sesion> listarPorEventos(Collection<Long> eventoIds);

    @Query("select s from Sesion s join fetch s.evento e join fetch e.temporada t join fetch t.categoria where s.id = :id and s.eliminado = false")
    Optional<Sesion> buscarConEvento(Long id);

    /** Cantidad de resultados por sesión (para saber qué sesiones ya tienen datos). */
    @Query("select r.sesion.id, count(r) from ResultadoSesion r where r.sesion.id in :ids and r.eliminado = false group by r.sesion.id")
    List<Object[]> contarResultados(Collection<Long> ids);
}
