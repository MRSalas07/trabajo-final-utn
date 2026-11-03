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
@Table(name = "resultado_sesion")
public class ResultadoSesion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resultado")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sesion", nullable = false)
    private Sesion sesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_piloto", nullable = false)
    private Piloto piloto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_equipo", nullable = false)
    private Equipo equipo;

    private Integer posicion;

    private Integer numero;

    private BigDecimal puntos;

    private BigDecimal tiempo;

    private BigDecimal diferencia;

    private Integer vueltas;

    @Column(name = "mejor_vuelta")
    private BigDecimal mejorVuelta;

    @Column(columnDefinition = "varchar")
    private String estado;

    @Column(name = "motivo_abandono", columnDefinition = "varchar")
    private String motivoAbandono;
}
