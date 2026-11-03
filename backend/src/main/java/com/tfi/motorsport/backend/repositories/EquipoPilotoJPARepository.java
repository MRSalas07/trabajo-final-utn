package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.EquipoPiloto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipoPilotoJPARepository extends JpaRepository<EquipoPiloto, Long> {
}
