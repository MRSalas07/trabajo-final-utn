package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoJPARepository extends JpaRepository<Evento, Long> {
}
