package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Etapa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EtapaJPARepository extends JpaRepository<Etapa, Long> {
    Optional<Etapa> findByEventoIdAndIdExterno(Long eventoId, String idExterno);

    @Query("select et from Etapa et where et.evento.id in :eventoIds and et.eliminado = false order by et.numero")
    List<Etapa> listarPorEventos(Collection<Long> eventoIds);

    @Query("select et from Etapa et join fetch et.evento e join fetch e.temporada t join fetch t.categoria where et.id = :id and et.eliminado = false")
    Optional<Etapa> buscarConEvento(Long id);

    @Query("select r.etapa.id, count(r) from ResultadoEtapa r where r.etapa.id in :ids and r.eliminado = false group by r.etapa.id")
    List<Object[]> contarResultados(Collection<Long> ids);

}
