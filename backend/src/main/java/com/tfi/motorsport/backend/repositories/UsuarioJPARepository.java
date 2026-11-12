package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioJPARepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCaseAndEliminadoFalse(String email);

    boolean existsByEmailIgnoreCase(String email);
}
