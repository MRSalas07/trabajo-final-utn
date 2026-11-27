package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Piloto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PilotoJPARepository extends JpaRepository<Piloto, Long> {

    /*
     * Búsqueda del listado de pilotos.
     * - patron: texto en minúsculas entre comodines ("%ham%"; "%%" = sin filtro de texto).
     *   Se compara contra nombre, apellido, "nombre apellido" y código.
     * - filtrarParticipacion: true cuando hay filtro de categoría o de año. En ese caso el piloto
     *   tiene que tener al menos una participación (equipo_x_piloto) que los cumpla.
     *   Con false no se exige participación, así también aparecen pilotos cargados sin equipo.
     * El orden lo define el Pageable que arma el servicio.
     */
    String FILTRO_BUSQUEDA = """
            where p.eliminado = false
              and (lower(p.apellido) like :patron
                   or lower(coalesce(p.nombre, '')) like :patron
                   or lower(concat(coalesce(p.nombre, ''), ' ', p.apellido)) like :patron
                   or lower(coalesce(p.codigoPiloto, '')) like :patron)
              and (:filtrarParticipacion = false or exists (
                    select 1 from EquipoPiloto ep
                    where ep.piloto = p and ep.eliminado = false
                      and (:categoriaId is null or ep.categoria.id = :categoriaId)
                      and (:anio is null or ep.temporada = :anio)))
            """;

    @Query(value = "select p from Piloto p " + FILTRO_BUSQUEDA,
            countQuery = "select count(p) from Piloto p " + FILTRO_BUSQUEDA)
    Page<Piloto> buscar(String patron, boolean filtrarParticipacion, Long categoriaId, Integer anio,
                        Pageable pageable);

    Optional<Piloto> findByIdExterno(String idExterno);
}
