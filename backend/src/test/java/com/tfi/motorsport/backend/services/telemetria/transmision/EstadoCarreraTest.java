package com.tfi.motorsport.backend.services.telemetria.transmision;

import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Control;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Intervalo;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.MuestraAuto;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Pit;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Piloto;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Posicion;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Stint;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Vuelta;
import com.tfi.motorsport.backend.dtos.telemetria.TransmisionDtos.FilaTorre;
import com.tfi.motorsport.backend.dtos.telemetria.TransmisionDtos.Foco;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** El estado de la carrera se arma igual con datos en vivo o de repetición: se prueba aplicando eventos. */
class EstadoCarreraTest {

    private static final Instant T0 = Instant.parse("2025-12-07T13:00:00Z");

    private EstadoCarrera estadoBase() {
        EstadoCarrera e = new EstadoCarrera();
        e.setPilotos(List.of(new Piloto(1, "VER", "Max Verstappen", "Red Bull", "#3671C6"),
                new Piloto(4, "NOR", "Lando Norris", "McLaren", "#FF8000")));
        e.setStints(List.of(new Stint(4, 1, 20, "MEDIUM", 0), new Stint(4, 21, null, "HARD", 0)));
        return e;
    }

    @Test
    void torreOrdenadaConDiferenciasYVueltaRapida() {
        EstadoCarrera e = estadoBase();
        e.aplicar(new Posicion(T0, 4, 1));
        e.aplicar(new Posicion(T0, 1, 2));
        e.aplicar(new Intervalo(T0, 1, "+2.345", "+2.345"));
        e.aplicar(new Vuelta(T0.plusSeconds(90), 4, 21, new BigDecimal("84.500"), false));
        e.aplicar(new Vuelta(T0.plusSeconds(91), 1, 21, new BigDecimal("84.100"), false));
        e.setReloj(T0.plusSeconds(95));

        List<FilaTorre> torre = e.torre();
        assertThat(torre).extracting(FilaTorre::acronimo).containsExactly("NOR", "VER");
        assertThat(torre.get(0).gap()).isEqualTo("Líder");
        assertThat(torre.get(1).gap()).isEqualTo("+2.345");
        assertThat(torre.get(1).vueltaRapida()).isTrue();     // 84.100 es la mejor
        assertThat(torre.get(0).neumatico()).isEqualTo("HARD"); // vuelta 22 -> segundo stint
        assertThat(torre.get(0).edadNeumatico()).isEqualTo(1);
        assertThat(e.vueltaLider()).isEqualTo(22);   // completó la 21: está en la 22
    }

    @Test
    void alLargarElLiderEstaEnLaVuelta1() {
        // Al largar, OpenF1 publica posiciones antes que la primera vuelta del líder
        EstadoCarrera e = estadoBase();
        assertThat(e.vueltaLider()).isNull();             // sin líder todavía

        e.aplicar(new Posicion(T0, 4, 1));
        e.aplicar(new Posicion(T0, 1, 2));
        e.setReloj(T0.plusSeconds(5));
        assertThat(e.vueltaLider()).isEqualTo(1);
        assertThat(e.torre()).extracting(FilaTorre::acronimo).containsExactly("NOR", "VER");

        // Cuando completa la vuelta 1, pasa a la 2
        e.aplicar(new Vuelta(T0.plusSeconds(100), 4, 1, new BigDecimal("95.000"), false));
        assertThat(e.vueltaLider()).isEqualTo(2);
    }

    @Test
    void laVueltaNoSuperaElTotalYConCuadrosMuestraLaUltima() {
        EstadoCarrera e = estadoBase();
        e.setTotalVueltas(58);
        e.aplicar(new Posicion(T0, 4, 1));
        e.aplicar(new Vuelta(T0.plusSeconds(10), 4, 57, new BigDecimal("85.0"), false));
        assertThat(e.vueltaLider()).isEqualTo(58);        // última vuelta en curso

        e.aplicar(new Vuelta(T0.plusSeconds(95), 4, 58, new BigDecimal("85.0"), false));
        assertThat(e.vueltaLider()).isEqualTo(58);        // no muestra 59

        e.aplicar(new Control(T0.plusSeconds(96), 58, "CHEQUERED", "Flag", "CHEQUERED FLAG", "Track"));
        assertThat(e.vueltaLider()).isEqualTo(58);
    }

    @Test
    void sinTotalConocidoLaBanderaACuadrosMuestraLaUltimaCompletada() {
        // En vivo OpenF1 no informa el total de vueltas
        EstadoCarrera e = estadoBase();
        e.aplicar(new Posicion(T0, 4, 1));
        e.aplicar(new Vuelta(T0.plusSeconds(95), 4, 58, new BigDecimal("85.0"), false));
        assertThat(e.vueltaLider()).isEqualTo(59);
        e.aplicar(new Control(T0.plusSeconds(96), 58, "CHEQUERED", "Flag", "CHEQUERED FLAG", "Track"));
        assertThat(e.vueltaLider()).isEqualTo(58);
    }

    @Test
    void boxesSeMuestranDuranteTreintaSegundos() {
        EstadoCarrera e = estadoBase();
        e.aplicar(new Pit(T0, 1, 10));
        e.setReloj(T0.plusSeconds(10));
        assertThat(e.torre().stream().filter(f -> f.numero() == 1).findFirst().orElseThrow().enBoxes()).isTrue();
        e.setReloj(T0.plusSeconds(40));
        assertThat(e.torre().stream().filter(f -> f.numero() == 1).findFirst().orElseThrow().enBoxes()).isFalse();
    }

    @Test
    void estadoDePistaSegunControlDeCarrera() {
        EstadoCarrera e = estadoBase();
        e.aplicar(new Control(T0, 5, null, "SafetyCar", "SAFETY CAR DEPLOYED", "Track"));
        assertThat(e.estadoPista()).isEqualTo("SC");
        e.aplicar(new Control(T0.plusSeconds(60), 8, "GREEN", "Flag", "GREEN LIGHT - PIT EXIT OPEN", "Track"));
        assertThat(e.estadoPista()).isEqualTo("VERDE");
        e.aplicar(new Control(T0.plusSeconds(90), 9, "RED", "Flag", "RED FLAG", "Track"));
        assertThat(e.estadoPista()).isEqualTo("ROJA");
        e.aplicar(new Control(T0.plusSeconds(99), 60, "CHEQUERED", "Flag", "CHEQUERED FLAG", "Track"));
        assertThat(e.estadoPista()).isEqualTo("CUADROS");
        assertThat(e.mensajes().get(0).mensaje()).isEqualTo("CHEQUERED FLAG");
    }

    @Test
    void focoConUltimaMuestraYTraza() {
        EstadoCarrera e = estadoBase();
        e.aplicar(new MuestraAuto(T0, 1, 280, 11000, 7, 100, 0, 12));
        e.aplicar(new MuestraAuto(T0.plusMillis(270), 1, 120, 9000, 3, 0, 100, 0));
        Foco f = e.foco(1);
        assertThat(f.velocidad()).isEqualTo(120);
        assertThat(f.freno()).isEqualTo(100);
        assertThat(f.drs()).isFalse();
        assertThat(f.trazaVelocidad()).containsExactly(280, 120);
    }
}
