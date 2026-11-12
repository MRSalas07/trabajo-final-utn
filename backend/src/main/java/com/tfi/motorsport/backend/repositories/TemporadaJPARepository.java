package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Temporada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public interface TemporadaJPARepository extends JpaRepository<Temporada, Long> {

    List<Temporada> findByCategoriaIdAndEliminadoFalseOrderByAnioDesc(Long categoriaId);

    Optional<Temporada> findByCategoriaIdAndAnio(Long categoriaId, Integer anio);

    @Query("select t from Temporada t join fetch t.categoria c where upper(c.siglas) = upper(:siglas) and t.anio = :anio and t.eliminado = false")
    Optional<Temporada> buscarPorSiglasYAnio(String siglas, Integer anio);

}
