package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Etapa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EtapaJPARepository extends JpaRepository<Etapa, Long> {
}
