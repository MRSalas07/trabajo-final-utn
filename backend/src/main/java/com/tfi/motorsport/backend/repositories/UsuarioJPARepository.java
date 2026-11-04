package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioJPARepository extends JpaRepository<Usuario, Long> {
}
