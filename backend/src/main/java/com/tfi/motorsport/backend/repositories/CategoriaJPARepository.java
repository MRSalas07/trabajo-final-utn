package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaJPARepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByActivo(Boolean activo);

    List<Categoria> findAllByEliminadoFalseOrderByIdAsc();

    List<Categoria> findAllByActivoAndEliminadoFalseOrderByIdAsc(Boolean activo);

    Optional<Categoria> findBySiglasIgnoreCase(String siglas);

    boolean existsBySiglasIgnoreCase(String siglas);
}
