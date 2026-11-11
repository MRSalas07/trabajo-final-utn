package com.tfi.motorsport.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@MappedSuperclass
public abstract class AuditableEntity {

    @Column(name = "\"fechaCreacion\"", nullable = false, insertable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "\"fechaActualizacion\"", nullable = false, insertable = false, updatable = false)
    private Instant fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creadopor")
    private Usuario creadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizadopor")
    private Usuario actualizadoPor;

    @Column(nullable = false)
    private boolean eliminado = false;
}
