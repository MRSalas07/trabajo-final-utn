package com.tfi.motorsport.backend.clients.openf1;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Límite de consultas por minuto a OpenF1 con ventana deslizante (sin relojes reales). */
class VentanaDeslizanteTest {

    private static final long UN_MINUTO = 60_000;

    @Test
    void permiteHastaElLimiteYDespuesIndicaCuantoEsperar() {
        VentanaDeslizante ventana = new VentanaDeslizante(3, UN_MINUTO);

        assertThat(ventana.reservar(0)).isZero();
        assertThat(ventana.reservar(10)).isZero();
        assertThat(ventana.reservar(20)).isZero();
        // La más vieja (instante 0) sale de la ventana a los 60.000 ms
        assertThat(ventana.reservar(30)).isEqualTo(59_970L);
    }

    @Test
    void alSalirLaConsultaMasViejaSeLiberaUnLugar() {
        VentanaDeslizante ventana = new VentanaDeslizante(2, UN_MINUTO);
        ventana.reservar(0);
        ventana.reservar(1_000);

        assertThat(ventana.reservar(59_999)).isEqualTo(1L);
        assertThat(ventana.reservar(60_000)).isZero();
        // Ahora la más vieja es la del instante 1.000
        assertThat(ventana.reservar(60_500)).isEqualTo(500L);
    }

    @Test
    void noPermiteElDobleDeConsultasAlrededorDelCambioDeMinuto() {
        // Contando por "minuto calendario" se podrían hacer 2 consultas a los 59,9 s y otras 2 a los 60,1 s
        VentanaDeslizante ventana = new VentanaDeslizante(2, UN_MINUTO);
        ventana.reservar(59_900);
        ventana.reservar(59_950);

        assertThat(ventana.reservar(60_100)).isPositive();
    }
}
