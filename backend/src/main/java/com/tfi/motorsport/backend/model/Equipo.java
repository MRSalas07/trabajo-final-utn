package com.tfi.motorsport.backend.model;

import com.tfi.motorsport.backend.model.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "equipo")
public class Equipo extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_equipo")
    private Long id;

    @Column(name = "id_externo", columnDefinition = "varchar")
    private String idExterno;

    @Column(columnDefinition = "varchar")
    private String nombre;

    @Column(name = "nombre_corto", columnDefinition = "varchar")
    private String nombreCorto;

    @Column(columnDefinition = "varchar")
    private String nacionalidad;

    @Column(name = "logo_url", columnDefinition = "varchar")
    private String logoUrl;

    @Column(name = "fecha_fundacion")
    private LocalDate fechaFundacion;

    @Column(columnDefinition = "varchar")
    private String sede;

    @OneToMany(mappedBy = "equipo")
    private List<EquipoPiloto> equiposPilotos = new ArrayList<>();

    @OneToMany(mappedBy = "equipo")
    private List<Tripulacion> tripulaciones = new ArrayList<>();

    @OneToMany(mappedBy = "equipo")
    private List<ResultadoSesion> resultadosSesion = new ArrayList<>();
}
