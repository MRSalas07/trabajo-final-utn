package com.tfi.motorsport.backend.services.estadisticas;

import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.*;
import com.tfi.motorsport.backend.services.estadisticas.MotorEstadistico.Participacion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.tfi.motorsport.backend.services.estadisticas.CampeonatoDePrueba.*;
import static org.assertj.core.api.Assertions.assertThat;

class AnalisisEquiposYMarcasTest {

    private static List<Participacion> participaciones2025() {
        return MotorEstadistico.consolidar(temporada2025());
    }

    @Test
    void rankingDeMarcas_sumaLosPuntosDeTodosSusPilotos() {
        List<FilaMarca> marcas = AnalisisEquiposYMarcas.rankingMarcas(participaciones2025());

        assertThat(marcas).extracting(FilaMarca::marca).containsExactly("Ford", "Toyota");
        assertThat(marcas.get(0).posicion()).isEqualTo(1);
        assertThat(marcas.get(0).puntos()).as("Alonso 58 + Colapinto 55").isEqualByComparingTo("113");
        assertThat(marcas.get(0).victorias()).isEqualTo(2);
        assertThat(marcas.get(0).pilotos()).isEqualTo(2);
        assertThat(marcas.get(0).equipos()).containsExactly("Rojo");
        assertThat(marcas.get(1).puntos()).isEqualByComparingTo("51");
    }

    @Test
    void tendenciaDeEquipo_calculaPuntosPorFechaAcumuladoYPromedio() {
        TendenciaGrupo tendencia = AnalisisEquiposYMarcas.tendencia(participaciones2025(),
                AnalisisEquiposYMarcas.TIPO_EQUIPO, String.valueOf(EQUIPO_ROJO));

        assertThat(tendencia.fechas()).extracting(punto -> punto.puntos().intValue()).containsExactly(40, 33, 40);
        assertThat(tendencia.fechas().get(2).puntosAcumulados()).isEqualByComparingTo("113");
        assertThat(tendencia.fechas().get(0).mejorPosicion()).isEqualTo(1);
        assertThat(tendencia.promedioPuntos()).isEqualByComparingTo("37.67");
        assertThat(tendencia.interpretacion()).isEqualTo("Estable");
        assertThat(tendencia.nombre()).isEqualTo("Rojo");
        assertThat(tendencia.pilotos()).containsExactlyInAnyOrder("Fernando Alonso", "Franco Colapinto");
    }

    @Test
    void tendenciaDeMarca_noDistingueMayusculasYCuentaElAbandonoConCeroPuntos() {
        TendenciaGrupo tendencia = AnalisisEquiposYMarcas.tendencia(participaciones2025(),
                AnalisisEquiposYMarcas.TIPO_MARCA, "toyota");

        assertThat(tendencia.nombre()).isEqualTo("Toyota");
        assertThat(tendencia.fechas()).extracting(punto -> punto.puntos().intValue()).containsExactly(18, 33, 0);
        assertThat(tendencia.fechas().get(2).mejorPosicion()).isNull();
    }

    @Test
    void evolucionDeMarcas_ordenaPorPuntosFinales() {
        EvolucionGrupos evolucion = AnalisisEquiposYMarcas.evolucion(participaciones2025(),
                AnalisisEquiposYMarcas.TIPO_MARCA, 5);

        assertThat(evolucion.rondas()).hasSize(3);
        assertThat(evolucion.series()).extracting(SerieGrupo::nombre).containsExactly("Ford", "Toyota");
        assertThat(evolucion.series().get(1).puntosAcumulados())
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("18"), new BigDecimal("51"), new BigDecimal("51"));
    }

    @Test
    void evolucion_limitaLaCantidadDeSeries() {
        EvolucionGrupos evolucion = AnalisisEquiposYMarcas.evolucion(participaciones2025(),
                AnalisisEquiposYMarcas.TIPO_EQUIPO, 1);

        assertThat(evolucion.series()).extracting(SerieGrupo::nombre).containsExactly("Rojo");
    }
}
