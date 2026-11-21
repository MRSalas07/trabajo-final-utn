package com.tfi.motorsport.backend.clients.normalization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * MODELO NORMALIZADO de datos deportivos.
 * Es el "idioma común" de la capa de integración
 * Cada adaptador traduce el formato de su proveedor a estos records; luego un único
 * servicio valida y persiste. Agregar una nueva fuente solo requiere un adaptador nuevo.
 */

public final class ModeloNormalizado {

    private ModeloNormalizado() {
    }

    public record TemporadaNorm(
            @NotBlank @Size(max = 10) String siglasCategoria,
            @NotNull @Min(1950) @Max(2100) Integer anio,
            @Size(max = 120) String nombre,
            @Size(max = 40) String fuente, // Origen de los datos
            @NotNull List<@Valid EventoNorm> eventos,
            @Size(max = 80) String idExternoCampeon
    ) {
        public TemporadaNorm(String siglasCategoria, Integer anio, String nombre, String fuente, List<EventoNorm> eventos) {
            this(siglasCategoria, anio, nombre, fuente, eventos, null);
        }
    }

    public record EventoNorm(
            @NotBlank @Size(max = 60) String idExterno,
            @NotBlank @Size(max = 150) String nombre,
            @Size(max = 60) String nombreCorto,
            @Min(1) Integer numeroFecha,
            @Size(max = 80) String pais,
            @Size(max = 80) String ciudad,
            @Size(max = 150) String circuito,
            @Size(max = 64) String zonaHoraria,
            @NotNull Instant inicio,
            Instant fin,
            @Pattern(regexp = "PROGRAMADO|EN_CURSO|FINALIZADO|CANCELADO") String estado,
            List<@Valid SesionNorm> sesiones,
            List<@Valid EtapaNorm> etapas
    ) {
    }

    public record SesionNorm(
            @NotBlank @Size(max = 60) String idExterno,
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Pattern(regexp = "PRACTICA|CLASIFICACION|SPRINT_CLASIFICACION|SPRINT|SERIE|CARRERA") String tipo,
            Integer numero,
            Instant inicio,
            Instant fin,
            @Min(0) Integer vueltasProgramadas,
            @DecimalMin("0") BigDecimal distancia,
            @Pattern(regexp = "PROGRAMADO|EN_CURSO|FINALIZADO|CANCELADO") String estado,
            List<@Valid ResultadoNorm> resultados
    ) {
    }

    public record ResultadoNorm(
            @NotNull @Valid PilotoNorm piloto,
            @NotNull @Valid EquipoNorm equipo,
            @Min(1) Integer posicion,
            Integer numero,
            @DecimalMin("0") BigDecimal puntos,
            @DecimalMin("0") BigDecimal tiempo, // Tiempo total en segundos
            @DecimalMin("0") BigDecimal diferencia, //Diferencia con el ganador en segundos
            @Min(0) Integer vueltas,
            @DecimalMin("0") BigDecimal mejorVuelta, //Mejor vuelta en segundos
            @Pattern(regexp = "FINALIZADO|ABANDONO|DESCALIFICADO|NO_LARGO") String estado,
            @Size(max = 120) String motivoAbandono
    ) {
    }

    public record EtapaNorm(
            @NotBlank @Size(max = 60) String idExterno,
            Integer numero,
            @NotBlank @Size(max = 150) String nombre,
            Integer dia,
            @DecimalMin("0") BigDecimal distancia,
            Instant inicio,
            Instant fin,
            @Pattern(regexp = "PROGRAMADO|EN_CURSO|FINALIZADO|CANCELADO") String estado,
            List<@Valid ResultadoEtapaNorm> resultados
    ) {
    }

    public record ResultadoEtapaNorm(
            @NotNull @Valid PilotoNorm piloto,
            @NotNull @Valid PilotoNorm copiloto,
            @NotNull @Valid EquipoNorm equipo,
            Integer numero,
            @Min(1) Integer posicion,
            @DecimalMin("0") BigDecimal tiempo,
            @DecimalMin("0") BigDecimal diferencia,
            @DecimalMin("0") BigDecimal puntos,
            @Pattern(regexp = "FINALIZADO|ABANDONO|DESCALIFICADO|NO_LARGO") String estado,
            @Size(max = 120) String motivoAbandono
    ) {
    }

    public record PilotoNorm(
            @NotBlank @Size(max = 80) String idExterno, //Único global
            @Size(max = 80) String nombre,
            @NotBlank @Size(max = 80) String apellido,
            LocalDate fechaNacimiento,
            @Size(max = 60) String nacionalidad,
            @Size(max = 60) String pais,
            @Size(max = 10) String codigo,
            Integer numero
    ) {
    }

    public record EquipoNorm(
            @NotBlank @Size(max = 80) String idExterno,
            @NotBlank @Size(max = 120) String nombre,
            @Size(max = 60) String nombreCorto,
            @Size(max = 60) String nacionalidad
    ) {
    }
}

