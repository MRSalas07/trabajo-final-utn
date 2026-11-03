package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Palmares;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PalmaresJPARepository extends JpaRepository<Palmares, Long> {
}
