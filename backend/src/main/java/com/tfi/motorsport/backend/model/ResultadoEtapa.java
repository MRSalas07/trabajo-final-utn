package com.tfi.motorsport.backend.model;


import com.tfi.motorsport.backend.model.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "resultado_etapa")
public class ResultadoEtapa extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resultado_etapa")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_etapa", nullable = false)
    private Etapa etapa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tripulacion", nullable = false)
    private Tripulacion tripulacion;

    private Integer posicion;

    private BigDecimal tiempo;

    private BigDecimal diferencia;

    private BigDecimal puntos;

    @Column(columnDefinition = "varchar")
    private String estado;

    @Column(name = "motivo_abandono", columnDefinition = "varchar")
    private String motivoAbandono;
}
