package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipoJPARepository extends JpaRepository<Equipo, Long> {

    /** Equipos con al menos una participación en la categoría (y en el año, si se indica). */
    @Query("""
            select e from Equipo e
            where e.eliminado = false and exists (
                select 1 from EquipoPiloto ep
                where ep.equipo = e and ep.eliminado = false
                  and ep.categoria.id = :categoriaId
                  and (:anio is null or ep.temporada = :anio))
            order by e.nombre
            """)
    List<Equipo> deCategoria(Long categoriaId, Integer anio);
}
