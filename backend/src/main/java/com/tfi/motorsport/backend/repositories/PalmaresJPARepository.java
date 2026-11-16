package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Palmares;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PalmaresJPARepository extends JpaRepository<Palmares, Long> {
    Optional<Palmares> findByPilotoIdAndCategoriaId(Long pilotoId, Long categoriaId);

    @Query("select p from Palmares p join fetch p.categoria where p.piloto.id = :pilotoId and p.eliminado = false")
    List<Palmares> dePiloto(Long pilotoId);

    @Query("""
            select p from Palmares p join fetch p.piloto join fetch p.categoria c
            where p.eliminado = false and c.id = :categoriaId
            order by p.campeonatos desc, p.victorias desc, p.puntosTotales desc
            """)
    List<Palmares> deCategoria(Long categoriaId);
}
