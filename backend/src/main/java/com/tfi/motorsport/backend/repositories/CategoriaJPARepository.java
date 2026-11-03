package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaJPARepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByActivo(Boolean activo);
}
