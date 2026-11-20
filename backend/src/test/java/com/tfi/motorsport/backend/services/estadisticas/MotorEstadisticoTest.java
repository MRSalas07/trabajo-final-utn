package com.tfi.motorsport.backend.services.estadisticas;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.*;
import com.tfi.motorsport.backend.repositories.unificacion.FilaResultado;
import com.tfi.motorsport.backend.services.estadisticas.MotorEstadistico.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.jupiter.api.Assertions.*;

class MotorEstadisticoTest {
    private static final long EQUIPO_ROJO = 10L;
    private static final long EQUIPO_AZUL = 20L;

    static FilaResultado fila(long eventoId, int numeroFecha, long pilotoId, String apellido, long equipoId,
                              String tipoSesion, Integer posicion, int puntos, String estado) {
        return new FilaResultado(
                eventoId, numeroFecha, "GP " + numeroFecha,
                Instant.parse("2025-0" + numeroFecha + "-10T15:00:00Z"),
                1L, 2025,
                "F1",
                pilotoId, "Piloto", apellido, apellido.substring(0, 3).toUpperCase(),
                equipoId, equipoId == EQUIPO_ROJO ? "Rojo" : "Azul",
                tipoSesion, posicion, BigDecimal.valueOf(puntos), estado);
    }

    static List<FilaResultado> campeonato() {
        List<FilaResultado> filas = new ArrayList<>();
        // Fecha 1
        filas.add(fila(1, 1, 1, "Mateo", EQUIPO_ROJO, "CARRERA", 1, 25, "FINALIZADO"));
        filas.add(fila(1, 1, 2, "Gerardo", EQUIPO_AZUL, "CARRERA", 2, 18, "FINALIZADO"));
        filas.add(fila(1, 1, 3, "Salas", EQUIPO_ROJO, "CARRERA", 3, 15, "FINALIZADO"));
        filas.add(fila(1, 1, 2, "Gerardo", EQUIPO_AZUL, "CLASIFICACION", 1, 0, "FINALIZADO"));
        filas.add(fila(1, 1, 1, "Mateo", EQUIPO_ROJO, "CLASIFICACION", 2, 0, "FINALIZADO"));
        // Fecha 2 (con sprint)
        filas.add(fila(2, 2, 2, "Gerardo", EQUIPO_AZUL, "CARRERA", 1, 25, "FINALIZADO"));
        filas.add(fila(2, 2, 1, "Mateo", EQUIPO_ROJO, "CARRERA", 2, 18, "FINALIZADO"));
        filas.add(fila(2, 2, 3, "Salas", EQUIPO_ROJO, "CARRERA", 3, 15, "FINALIZADO"));
        filas.add(fila(2, 2, 2, "Gerardo", EQUIPO_AZUL, "SPRINT", 1, 8, "FINALIZADO"));
        // Fecha 3
        filas.add(fila(3, 3, 3, "Salas", EQUIPO_ROJO, "CARRERA", 1, 25, "FINALIZADO"));
        filas.add(fila(3, 3, 1, "Mateo", EQUIPO_ROJO, "CARRERA", 3, 15, "FINALIZADO"));
        filas.add(fila(3, 3, 2, "Gerardo", EQUIPO_AZUL, "CARRERA", 3, 0, "ABANDONO"));
        return filas;
    }

    @Test
    void consolidaPuntosDeCarreraYSprintEnUnaParticipacion() {
        List<Participacion> participaciones = MotorEstadistico.consolidar(campeonato());

        Participacion betoFecha2 = participaciones.stream()
                .filter(participacion -> participacion.pilotoId() == 2 && participacion.eventoId() == 2)
                .findFirst().orElseThrow();
        assertThat(betoFecha2.puntos()).isEqualByComparingTo("33"); // 25 de la carrera + 8 del sprint
        assertThat(betoFecha2.posicionFinal()).isEqualTo(1);

        Participacion betoFecha1 = participaciones.stream()
                .filter(participacion -> participacion.pilotoId() == 2 && participacion.eventoId() == 1)
                .findFirst().orElseThrow();
        assertThat(betoFecha1.posicionClasificacion()).isEqualTo(1); // pole
    }

    @Test
    void campeonatoDePilotosOrdenadoPorPuntos() {
        List<FilaCampeonatoPiloto> tabla =
                MotorEstadistico.campeonatoPilotos(MotorEstadistico.consolidar(campeonato()));

        assertThat(tabla).extracting(FilaCampeonatoPiloto::pilotoId).containsExactly(1L, 3L, 2L);

        FilaCampeonatoPiloto ana = tabla.get(0);
        assertThat(ana.puntos()).isEqualByComparingTo("58");
        assertThat(ana.victorias()).isEqualTo(1);
        assertThat(ana.podios()).isEqualTo(3);
        assertThat(ana.promedioPosicion()).isEqualByComparingTo("2.00");

        EstadisticaDto.FilaCampeonatoPiloto beto = tabla.get(2);
        assertThat(beto.puntos()).isEqualByComparingTo("51");
        assertThat(beto.abandonos()).isEqualTo(1);
        assertThat(beto.poles()).isEqualTo(1);
        // El abandono no cuenta como podio aunque figure "3º" en la tabla
        assertThat(beto.podios()).isEqualTo(2);
    }

    @Test
    void campeonatoDeEquiposSumaPuntosDeSusPilotos() {
        List<EstadisticaDto.FilaCampeonatoEquipo> tabla =
                MotorEstadistico.campeonatoEquipos(MotorEstadistico.consolidar(campeonato()));

        assertThat(tabla.get(0).equipo()).isEqualTo("Rojo");
        assertThat(tabla.get(0).puntos()).isEqualByComparingTo("113"); // Gerardo 58 + Salas 55
        assertThat(tabla.get(1).puntos()).isEqualByComparingTo("51");  // Mateo
    }

    @Test
    void evolucionAcumulaPuntosYPosicionRondaARonda() {
        EvolucionCampeonato evolucion = MotorEstadistico.evolucion(
                MotorEstadistico.consolidar(campeonato()), List.of(2L), 5);

        assertThat(evolucion.rondas()).hasSize(3);
        assertThat(evolucion.series().get(0).puntosAcumulados())
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("18"), new BigDecimal("51"), new BigDecimal("51"));
        // Tras la fecha 2 Mateo (51) lidera sobre Gerardo (43)
        assertThat(evolucion.series().get(0).posicionCampeonato()).containsExactly(2, 1, 3);
    }

    @Test
    void tendenciaDetectaEmpeoramiento() {
        TendenciaPiloto tendencia = MotorEstadistico.tendenciadePiloto(MotorEstadistico.consolidar(campeonato()), 1L);

        // Mateo: 1º, 2º, 3º -> pendiente +1 (posiciones cada vez más altas = empeora)
        assertThat(tendencia.pendiente()).isEqualByComparingTo("1.000");
        assertThat(tendencia.interpretacion()).isEqualTo("Empeorando");
        assertThat(tendencia.tasaFinalizacion()).isEqualByComparingTo("100.0");
    }

    @Test
    void tendenciaConPocosDatosNoSeInterpreta() {
        TendenciaPiloto tendencia = MotorEstadistico.tendenciadePiloto(MotorEstadistico.consolidar(campeonato()), 2L);

        // Gerardo solo terminó 2 carreras (abandonó la 3ra)
        assertThat(tendencia.pendiente()).isNull();
        assertThat(tendencia.interpretacion()).startsWith("Datos insuficientes");
    }

    @Test
    void comparativaCuentaDuelosDirectos() {
        ComparativaPilotos comparativa = MotorEstadistico.comparativaPilotos(
                MotorEstadistico.consolidar(campeonato()), 1L, 3L);

        assertThat(comparativa.carrerasDelanteA()).isEqualTo(2);
        assertThat(comparativa.carrerasDelanteB()).isEqualTo(1);
        assertThat(comparativa.diferenciaPuntos().get(2)).isEqualByComparingTo("3"); // 58 - 55
    }

    @Test
    void regresionLinealYDesviacion() {
        assertThat(MotorEstadistico.pendienteDeRegresionLineal(List.of(
                new double[]{1, 10}, new double[]{2, 8}, new double[]{3, 6}))).isEqualByComparingTo("-2.000");
        assertThat(MotorEstadistico.desviacionEstandar(List.of(2, 4, 4, 4, 5, 5, 7, 9))).isEqualByComparingTo("2.00");
    }


}