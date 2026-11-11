package com.tfi.motorsport.backend.model;


import com.tfi.motorsport.backend.model.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "usuario")
public class Usuario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(columnDefinition = "varchar")
    private String nombre;

    @Column(columnDefinition = "varchar")
    private String apellido;

    @Column(columnDefinition = "varchar")
    private String email;

    @Column(name = "password_hash", columnDefinition = "varchar")
    private String passwordHash;

    @Column(name = "zona_horaria", columnDefinition = "varchar")
    private String zonaHoraria;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    private Boolean activo;
}
