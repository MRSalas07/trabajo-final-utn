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
@Table(name = "piloto")
public class Piloto extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_piloto")
    private Long id;

    @Column(name = "id_externo", columnDefinition = "varchar")
    private String idExterno;

    @Column(columnDefinition = "varchar")
    private String nombre;

    @Column(columnDefinition = "varchar")
    private String apellido;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(columnDefinition = "varchar")
    private String nacionalidad;

    @Column(columnDefinition = "varchar")
    private String pais;

    @Column(name = "codigo_piloto", columnDefinition = "varchar")
    private String codigoPiloto;

    @Column(name = "numero_actual")
    private Integer numeroActual;

    @Column(name = "foto_url", columnDefinition = "varchar")
    private String fotoUrl;

    @Column(name = "fecha_inicio_carrera")
    private LocalDate fechaInicioCarrera;

    @Column(name = "fecha_fin_carrera")
    private LocalDate fechaFinCarrera;

    @OneToMany(mappedBy = "piloto")
    private List<EquipoPiloto> equiposPilotos = new ArrayList<>();

    @OneToMany(mappedBy = "piloto")
    private List<Tripulacion> tripulacionesConducidas = new ArrayList<>();

    @OneToMany(mappedBy = "copiloto")
    private List<Tripulacion> tripulacionesCopilotadas = new ArrayList<>();

    @OneToMany(mappedBy = "piloto")
    private List<ResultadoSesion> resultadosSesion = new ArrayList<>();

    @OneToMany(mappedBy = "piloto")
    private List<Palmares> palmares = new ArrayList<>();
}
