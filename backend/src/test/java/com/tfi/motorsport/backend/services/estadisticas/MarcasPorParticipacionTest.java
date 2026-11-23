package com.tfi.motorsport.backend.services.estadisticas;

import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.FilaMarca;
import com.tfi.motorsport.backend.repositories.unificacion.FilaResultado;
import com.tfi.motorsport.backend.repositories.unificacion.MarcaParticipacion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** En TC la marca es la de cada piloto (equipo_x_piloto.marca), no la del equipo. */
class MarcasPorParticipacionTest {

    private static final long EQUIPO_TROTTA = 100L;
    private static final long EQUIPO_CANNING = 200L;

    private static FilaResultado fila(long eventoId, long pilotoId, String apellido, long equipoId, String equipo,
                                      int anio, int posicion, String puntos) {
        return new FilaResultado(eventoId, (int) eventoId, "Fecha " + eventoId, Instant.parse("2025-03-01T18:00:00Z"),
                10L, anio, "TC", pilotoId, "Piloto", apellido, null, equipoId, equipo,
                equipo, // hoy la marca sale de equipo.nombre_corto, que en TC guarda el nombre del equipo
                "CARRERA", posicion, new BigDecimal(puntos), "FINALIZADO");
    }

    /** Un mismo equipo (Trotta) con un Ford y un Chevrolet, y Canning con un Ford. */
    private static List<FilaResultado> temporada2025() {
        return List.of(
                fila(1, 1, "Werner", EQUIPO_TROTTA, "Trotta", 2025, 1, "40"),
                fila(1, 2, "Canapino", EQUIPO_TROTTA, "Trotta", 2025, 2, "35"),
                fila(1, 3, "Santero", EQUIPO_CANNING, "Canning", 2025, 3, "32"));
    }

    @Test
    void sinMarcasCargadas_lasFilasQuedanIgual() {
        List<FilaResultado> filas = temporada2025();

        assertThat(MarcasPorParticipacion.aplicar(filas, List.of())).isSameAs(filas);
    }

    @Test
    void conMarcasCargadas_usaLaDeCadaPilotoAunqueCompartanEquipo() {
        List<FilaResultado> conMarcas = MarcasPorParticipacion.aplicar(temporada2025(), List.of(
                new MarcaParticipacion(1L, EQUIPO_TROTTA, 2025, "Ford"),
                new MarcaParticipacion(2L, EQUIPO_TROTTA, 2025, " Chevrolet "),
                new MarcaParticipacion(3L, EQUIPO_CANNING, 2025, "Ford")));

        assertThat(conMarcas).extracting(FilaResultado::equipoMarca).containsExactly("Ford", "Chevrolet", "Ford");
        assertThat(conMarcas.get(1).equipoNombre()).as("el equipo no cambia").isEqualTo("Trotta");
        assertThat(conMarcas.get(1).puntos()).isEqualByComparingTo("35");
    }

    @Test
    void pilotoSinMarcaEnUnaTemporadaConMarcas_quedaSinMarcaEnVezDelNombreDelEquipo() {
        List<FilaResultado> conMarcas = MarcasPorParticipacion.aplicar(temporada2025(),
                List.of(new MarcaParticipacion(1L, EQUIPO_TROTTA, 2025, "Ford")));

        assertThat(conMarcas).extracting(FilaResultado::equipoMarca)
                .containsExactly("Ford", MarcasPorParticipacion.SIN_MARCA, MarcasPorParticipacion.SIN_MARCA);
    }

    @Test
    void lasMarcasDeOtraTemporadaNoAfectan() {
        List<FilaResultado> conMarcas = MarcasPorParticipacion.aplicar(temporada2025(),
                List.of(new MarcaParticipacion(1L, EQUIPO_TROTTA, 2024, "Chevrolet")));

        assertThat(conMarcas).extracting(FilaResultado::equipoMarca).containsExactly("Trotta", "Trotta", "Canning");
    }

    @Test
    void rankingDeMarcas_separaAlEquipoQueCorreConDosMarcas() {
        List<FilaResultado> conMarcas = MarcasPorParticipacion.aplicar(temporada2025(), List.of(
                new MarcaParticipacion(1L, EQUIPO_TROTTA, 2025, "Ford"),
                new MarcaParticipacion(2L, EQUIPO_TROTTA, 2025, "Chevrolet"),
                new MarcaParticipacion(3L, EQUIPO_CANNING, 2025, "Ford")));

        List<FilaMarca> ranking = AnalisisEquiposYMarcas.rankingMarcas(MotorEstadistico.consolidar(conMarcas));

        assertThat(ranking).extracting(FilaMarca::marca).containsExactly("Ford", "Chevrolet");
        assertThat(ranking.get(0).puntos()).as("Werner 40 + Santero 32").isEqualByComparingTo("72");
        assertThat(ranking.get(0).equipos()).containsExactlyInAnyOrder("Trotta", "Canning");
        assertThat(ranking.get(1).equipos()).containsExactly("Trotta");
    }
}
