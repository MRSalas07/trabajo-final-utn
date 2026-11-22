package com.tfi.motorsport.backend.services.estadisticas;

import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.*;
import com.tfi.motorsport.backend.services.estadisticas.MotorEstadistico.Participacion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.tfi.motorsport.backend.services.estadisticas.CampeonatoDePrueba.*;
import static org.assertj.core.api.Assertions.assertThat;

class MotorEstadisticoTest {

    private static List<Participacion> participaciones2025() {
        return MotorEstadistico.consolidar(temporada2025());
    }

    private static Participacion participacionDe(long pilotoId, long eventoId) {
        return participaciones2025().stream()
                .filter(participacion -> participacion.pilotoId() == pilotoId && participacion.eventoId() == eventoId)
                .findFirst().orElseThrow();
    }

    @Test
    void consolidaPuntosDeCarreraYSprintEnUnaParticipacion() {
        Participacion hamiltonFecha2 = participacionDe(HAMILTON, 2);
        Participacion hamiltonFecha1 = participacionDe(HAMILTON, 1);

        assertThat(hamiltonFecha2.puntos()).isEqualByComparingTo("33");
        assertThat(hamiltonFecha2.posicionFinal()).isEqualTo(1);
        assertThat(hamiltonFecha1.posicionClasificacion()).isEqualTo(1);
    }

    @Test
    void campeonatoDePilotosOrdenadoPorPuntos() {
        List<FilaCampeonatoPiloto> tabla = MotorEstadistico.campeonatoPilotos(participaciones2025());

        assertThat(tabla).extracting(FilaCampeonatoPiloto::pilotoId).containsExactly(ALONSO, COLAPINTO, HAMILTON);

        FilaCampeonatoPiloto alonso = tabla.get(0);
        assertThat(alonso.piloto()).isEqualTo("Fernando Alonso");
        assertThat(alonso.puntos()).isEqualByComparingTo("58");
        assertThat(alonso.victorias()).isEqualTo(1);
        assertThat(alonso.podios()).isEqualTo(3);
        assertThat(alonso.promedioPosicion()).isEqualByComparingTo("2.00");

        FilaCampeonatoPiloto hamilton = tabla.get(2);
        assertThat(hamilton.puntos()).isEqualByComparingTo("51");
        assertThat(hamilton.abandonos()).isEqualTo(1);
        assertThat(hamilton.poles()).isEqualTo(1);
        assertThat(hamilton.podios()).as("el abandono no cuenta como podio aunque figure 3º").isEqualTo(2);
    }

    @Test
    void campeonatoDeEquiposSumaPuntosDeSusPilotos() {
        List<FilaCampeonatoEquipo> tabla = MotorEstadistico.campeonatoEquipos(participaciones2025());

        assertThat(tabla.get(0).equipo()).isEqualTo("Rojo");
        assertThat(tabla.get(0).puntos()).as("Alonso 58 + Colapinto 55").isEqualByComparingTo("113");
        assertThat(tabla.get(1).puntos()).as("Hamilton").isEqualByComparingTo("51");
    }

    @Test
    void evolucionAcumulaPuntosYPosicionRondaARonda() {
        EvolucionCampeonato evolucion = MotorEstadistico.evolucion(participaciones2025(), List.of(HAMILTON), 5);

        assertThat(evolucion.rondas()).hasSize(3);
        assertThat(evolucion.series().get(0).puntosAcumulados())
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("18"), new BigDecimal("51"), new BigDecimal("51"));
        assertThat(evolucion.series().get(0).posicionCampeonato()).containsExactly(2, 1, 3);
    }

    @Test
    void tendenciaDetectaEmpeoramiento() {
        TendenciaPiloto tendencia = MotorEstadistico.tendenciadePiloto(participaciones2025(), ALONSO);

        assertThat(tendencia.pendiente()).as("1º, 2º, 3º: cada vez peor").isEqualByComparingTo("1.000");
        assertThat(tendencia.interpretacion()).isEqualTo("Empeorando");
        assertThat(tendencia.tasaFinalizacion()).isEqualByComparingTo("100.0");
    }

    @Test
    void tendenciaConPocosDatosNoSeInterpreta() {
        TendenciaPiloto tendencia = MotorEstadistico.tendenciadePiloto(participaciones2025(), HAMILTON);

        assertThat(tendencia.pendiente()).as("solo terminó 2 carreras").isNull();
        assertThat(tendencia.interpretacion()).startsWith("Datos insuficientes");
    }

    @Test
    void comparativaCuentaDuelosDirectos() {
        ComparativaPilotos comparativa = MotorEstadistico.comparativaPilotos(participaciones2025(), ALONSO, COLAPINTO);

        assertThat(comparativa.carrerasDelanteA()).isEqualTo(2);
        assertThat(comparativa.carrerasDelanteB()).isEqualTo(1);
        assertThat(comparativa.diferenciaPuntos().get(2)).as("58 - 55").isEqualByComparingTo("3");
    }

    @Test
    void elQueTerminaLaCarreraQuedaDelanteDelQueAbandona() {
        Participacion alonsoFecha3 = participacionDe(ALONSO, 3);
        Participacion hamiltonFecha3 = participacionDe(HAMILTON, 3);

        assertThat(MotorEstadistico.calculoTerminoPorDelante(alonsoFecha3, hamiltonFecha3)).isPositive();
        assertThat(MotorEstadistico.calculoTerminoPorDelante(hamiltonFecha3, alonsoFecha3)).isNegative();
    }

    @Test
    void regresionLinealYDesviacion() {
        assertThat(MotorEstadistico.pendienteDeRegresionLineal(List.of(
                new double[]{1, 10}, new double[]{2, 8}, new double[]{3, 6}))).isEqualByComparingTo("-2.000");
        assertThat(MotorEstadistico.desviacionEstandar(List.of(2, 4, 4, 4, 5, 5, 7, 9))).isEqualByComparingTo("2.00");
    }
}
