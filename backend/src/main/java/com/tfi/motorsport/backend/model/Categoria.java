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

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "categoria")
public class Categoria extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long id;

    @Column(columnDefinition = "varchar")
    private String nombre;

    @Column(columnDefinition = "varchar")
    private String siglas;

    @Column(columnDefinition = "varchar")
    private String descripcion;

    @Column(name = "pais_origen", columnDefinition = "varchar")
    private String paisOrigen;

    @Column(name = "logo_url", columnDefinition = "varchar")
    private String logoUrl;

    private Boolean activo;

    @OneToMany(mappedBy = "categoria")
    private List<Temporada> temporadas = new ArrayList<>();

    @OneToMany(mappedBy = "categoria")
    private List<EquipoPiloto> equiposPilotos = new ArrayList<>();

    @OneToMany(mappedBy = "categoria")
    private List<Tripulacion> tripulaciones = new ArrayList<>();

    @OneToMany(mappedBy = "categoria")
    private List<Palmares> palmares = new ArrayList<>();
}
