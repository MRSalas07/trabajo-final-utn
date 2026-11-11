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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "evento")
public class Evento extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_temporada", nullable = false)
    private Temporada temporada;

    @Column(name = "id_externo", columnDefinition = "varchar")
    private String idExterno;

    @Column(columnDefinition = "varchar")
    private String nombre;

    @Column(name = "nombre_corto", columnDefinition = "varchar")
    private String nombreCorto;

    @Column(name = "numero_fecha")
    private Integer numeroFecha;

    @Column(columnDefinition = "varchar")
    private String pais;

    @Column(columnDefinition = "varchar")
    private String ciudad;

    @Column(columnDefinition = "varchar")
    private String circuito;

    @Column(name = "zona_horaria", nullable = false, length = 64)
    private String zonaHoraria;

    @Column(name = "fecha_inicio")
    private Instant fechaInicio;

    @Column(name = "fecha_fin")
    private Instant fechaFin;

    @Column(columnDefinition = "varchar")
    private String estado;

    @OneToMany(mappedBy = "evento")
    private List<Sesion> sesiones = new ArrayList<>();

    @OneToMany(mappedBy = "evento")
    private List<Etapa> etapas = new ArrayList<>();
}
