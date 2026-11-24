package com.tfi.motorsport.backend.services.telemetria;

import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.FilaFoto;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.MensajeCarrera;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.Versus;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.VueltaComparada;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.VueltaEnCurso;
import com.tfi.motorsport.backend.services.telemetria.CalculadoraFoto.DatosSesion;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.ClimaMuestra;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Control;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Intervalo;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Pit;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Piloto;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Posicion;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Stint;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Vuelta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La foto de la sesión en un instante: cada dato es el último informado hasta ese momento.
 * Incluye el versus de dos pilotos y las tandas de la clasificación.
 * Carrera de prueba: Verstappen (1) lidera hasta que Norris (4) lo pasa a los 200 segundos.
 */
class CalculadoraFotoTest {

    private static final Instant LARGADA = Instant.parse("2025-03-16T04:00:00Z");

    private static Instant segundo(int segundos) {
        return LARGADA.plusSeconds(segundos);
    }

    private static Vuelta vuelta(int numero, int vuelta, int terminaEnSegundo, String duracion) {
        return new Vuelta(segundo(terminaEnSegundo), numero, vuelta, duracion == null ? null : new BigDecimal(duracion), false);
    }

    private static final List<Piloto> PILOTOS = List.of(
            new Piloto(1, "VER", "Max VERSTAPPEN", "Red Bull Racing", "#3671C6"),
            new Piloto(4, "NOR", "Lando NORRIS", "McLaren", "#FF8000"));

    private static DatosSesion carrera() {
        return new DatosSesion(PILOTOS,
                List.of(new Posicion(segundo(0), 1, 1), new Posicion(segundo(0), 4, 2),
                        new Posicion(segundo(200), 4, 1), new Posicion(segundo(200), 1, 2)),
                List.of(new Intervalo(segundo(95), 1, "+0.000", "+0.000"),
                        new Intervalo(segundo(95), 4, "+1.500", "+1.500"),
                        new Intervalo(segundo(205), 1, "+0.800", "+0.800")),
                List.of(vuelta(1, 1, 95, "95.000"), vuelta(4, 1, 96, "96.500"),
                        vuelta(4, 2, 185, "89.000"), vuelta(1, 2, 186, "91.000"),
                        vuelta(4, 3, 275, "90.000"), vuelta(1, 3, 276, null)),
                List.of(new Stint(1, 1, 2, "MEDIUM", 0), new Stint(4, 1, null, "SOFT", 3), new Stint(1, 3, null, "HARD", 0)),
                List.of(new Pit(segundo(190), 1, 2)),
                List.of(new Control(segundo(10), 1, "GREEN", "Flag", "GREEN LIGHT - PIT EXIT OPEN", "Track"),
                        new Control(segundo(150), 2, "YELLOW", "Flag", "YELLOW IN TRACK SECTOR 4", "Sector")),
                List.of(new ClimaMuestra(segundo(0), new BigDecimal("24.1"), new BigDecimal("36.0"), 50, false, BigDecimal.ONE),
                        new ClimaMuestra(segundo(120), new BigDecimal("24.5"), new BigDecimal("37.2"), 48, true, BigDecimal.TWO)));
    }

    @Test
    void laVueltaNTerminaCuandoLaCompletaElPrimero() {
        List<Vuelta> vueltas = carrera().vueltas();

        assertThat(CalculadoraFoto.totalVueltas(vueltas)).isEqualTo(3);
        assertThat(CalculadoraFoto.finDeVuelta(vueltas, 2)).isEqualTo(segundo(185));
        assertThat(CalculadoraFoto.finDeVuelta(vueltas, 9)).isNull();
        assertThat(CalculadoraFoto.vueltasHasta(vueltas, segundo(190))).isEqualTo(2);
        assertThat(CalculadoraFoto.ultimoInstante(carrera())).isEqualTo(segundo(276));
    }

    @Test
    void antesDelSobrepasoLideraVerstappen() {
        List<FilaFoto> filas = CalculadoraFoto.filas(carrera(), segundo(100));

        assertThat(filas).extracting(FilaFoto::acronimo).containsExactly("VER", "NOR");
        assertThat(filas.get(0).diferencia()).isNull();
        FilaFoto norris = filas.get(1);
        assertThat(norris.diferencia()).isEqualTo("+1.500");
        assertThat(norris.vueltas()).isEqualTo(1);
        assertThat(norris.ultimaVuelta()).isEqualByComparingTo("96.5");
        assertThat(norris.neumatico()).isEqualTo("SOFT");
        // Empezó con un neumático de 3 vueltas y está en su vuelta 2: 3 + (2 - 1)
        assertThat(norris.edadNeumatico()).isEqualTo(4);
        assertThat(filas.get(0).vueltaRapida()).isTrue();
    }

    @Test
    void despuesDelSobrepasoLideraNorrisYVerstappenYaParo() {
        List<FilaFoto> filas = CalculadoraFoto.filas(carrera(), segundo(210));

        assertThat(filas).extracting(FilaFoto::acronimo).containsExactly("NOR", "VER");
        FilaFoto verstappen = filas.get(1);
        assertThat(verstappen.paradas()).isEqualTo(1);
        assertThat(verstappen.neumatico()).isEqualTo("HARD");
        assertThat(verstappen.mejorVuelta()).isEqualByComparingTo("91.0");
        assertThat(filas.get(0).vueltaRapida()).isTrue();
        assertThat(verstappen.vueltaRapida()).isFalse();
    }

    @Test
    void sinIntervalosLaDiferenciaEsPorMejorVuelta() {
        DatosSesion datos = carrera();
        DatosSesion clasificacion = new DatosSesion(datos.pilotos(), datos.posiciones(), List.of(), datos.vueltas(),
                datos.stints(), datos.pits(), datos.controles(), datos.clima());

        List<FilaFoto> filas = CalculadoraFoto.filas(clasificacion, segundo(300));

        assertThat(filas.get(0).diferencia()).isNull();
        // Norris 89.000 contra Verstappen 91.000
        assertThat(filas.get(1).diferencia()).isEqualTo("+2.000");
    }

    @Test
    void versus_comparaLasUltimasVueltasYSusSectores() {
        DatosSesion datos = conSectores();
        List<FilaFoto> filas = CalculadoraFoto.filas(datos, segundo(300));

        Versus versus = CalculadoraFoto.versus(datos, segundo(300), filas, 1, 4, true);

        assertThat(versus.a().acronimo()).isEqualTo("VER");
        assertThat(versus.a().ultimaVuelta().sector1()).isEqualByComparingTo("30.5");
        assertThat(versus.a().mejoresSectores()).containsExactly(new BigDecimal("30.0"), new BigDecimal("41.0"), new BigDecimal("19.5"));
        assertThat(versus.a().velocidadMaxima()).isEqualTo(305);
        assertThat(versus.a().paradas()).isEqualTo(1);
        assertThat(versus.a().tiempoEnBoxes()).isEqualByComparingTo("24.5");
        // El mejor sector 2 de la sesión es de Norris (40.5)
        assertThat(versus.mejoresSectoresSesion().get(1)).isEqualByComparingTo("40.5");
        // Vuelta 3: solo la completó Norris, no se compara. Vuelta 2: Norris 89.000 contra Verstappen 91.000
        assertThat(versus.vueltas()).extracting(VueltaComparada::vuelta).containsExactly(2, 1);
        assertThat(versus.vueltas().get(0).masRapido()).isEqualTo(4);
        assertThat(versus.vueltas().get(0).diferencia()).isEqualTo("2.000");
        // Fuera de carrera no se comparan vueltas por número
        assertThat(CalculadoraFoto.versus(datos, segundo(300), filas, 1, 4, false).vueltas()).isEmpty();
    }

    @Test
    void enClasificacionCadaTandaEmpiezaDeCero() {
        DatosSesion datos = carrera();
        List<Control> tandas = List.of(
                new Control(segundo(0), null, "GREEN", "Flag", "GREEN LIGHT - PIT EXIT OPEN", "Track", 1),
                new Control(segundo(180), null, "GREEN", "Flag", "GREEN LIGHT - PIT EXIT OPEN", "Track", 2));
        DatosSesion clasificacion = new DatosSesion(datos.pilotos(), datos.posiciones(), List.of(), datos.vueltas(),
                datos.stints(), datos.pits(), tandas, datos.clima());

        assertThat(CalculadoraFoto.tanda(tandas, segundo(100)).numero()).isEqualTo(1);
        assertThat(CalculadoraFoto.tanda(tandas, segundo(300)).inicio()).isEqualTo(segundo(180));
        List<FilaFoto> filas = CalculadoraFoto.filas(clasificacion, segundo(300));
        // En la Q2 Norris marcó 89.000 y 90.000; Verstappen 91.000 (su 95.000 fue en la Q1)
        assertThat(filas.get(0).mejorVuelta()).isEqualByComparingTo("89.0");
        assertThat(filas.get(1).mejorVuelta()).isEqualByComparingTo("91.0");
        assertThat(filas.get(1).diferencia()).isEqualTo("+2.000");
    }

    /** La misma carrera con sectores, velocidades y la duración de la parada. */
    private static DatosSesion conSectores() {
        DatosSesion datos = carrera();
        List<Vuelta> vueltas = List.of(
                new Vuelta(segundo(95), 1, 1, new BigDecimal("95.000"), false, new BigDecimal("30.0"),
                        new BigDecimal("42.0"), new BigDecimal("23.0"), 230, 250, 300),
                new Vuelta(segundo(96), 4, 1, new BigDecimal("96.500"), false, new BigDecimal("31.0"),
                        new BigDecimal("42.0"), new BigDecimal("23.5"), 228, 249, 298),
                new Vuelta(segundo(185), 4, 2, new BigDecimal("89.000"), false, new BigDecimal("30.2"),
                        new BigDecimal("40.5"), new BigDecimal("18.3"), 231, 251, 302),
                new Vuelta(segundo(186), 1, 2, new BigDecimal("91.000"), false, new BigDecimal("30.5"),
                        new BigDecimal("41.0"), new BigDecimal("19.5"), 232, 252, 305),
                new Vuelta(segundo(275), 4, 3, new BigDecimal("90.000"), false, null, null, null, null, null, null));
        return new DatosSesion(datos.pilotos(), datos.posiciones(), datos.intervalos(), vueltas, datos.stints(),
                List.of(new Pit(segundo(190), 1, 2, new BigDecimal("24.5"))), datos.controles(), datos.clima());
    }

    @Test
    void mensajesYClimaHastaElInstante() {
        List<MensajeCarrera> mensajes = CalculadoraFoto.mensajes(carrera().controles(), segundo(160));

        assertThat(mensajes).extracting(MensajeCarrera::bandera).containsExactly("YELLOW", "GREEN");
        assertThat(CalculadoraFoto.mensajes(carrera().controles(), segundo(5))).isEmpty();
        assertThat(CalculadoraFoto.clima(carrera().clima(), segundo(60)).lluvia()).isFalse();
        assertThat(CalculadoraFoto.clima(carrera().clima(), segundo(130)).lluvia()).isTrue();
    }

    @Test
    void vueltaEnCurso_muestraSoloLosSectoresYaCompletados() {
        List<Vuelta> vueltas = conSectores().vueltas();

        // Norris empezó la vuelta 2 en el segundo 96: el S1 (30.2) termina en el 126.2 y el S2 (40.5) en el 166.7
        VueltaEnCurso antesDelSector1 = CalculadoraFoto.vueltaEnCurso(vueltas, 4, segundo(120));
        VueltaEnCurso conSector1 = CalculadoraFoto.vueltaEnCurso(vueltas, 4, segundo(150));
        VueltaEnCurso conSector2 = CalculadoraFoto.vueltaEnCurso(vueltas, 4, segundo(170));

        assertThat(antesDelSector1.vuelta()).isEqualTo(2);
        assertThat(antesDelSector1.segundos()).isEqualTo(24);
        assertThat(antesDelSector1.sector1()).isNull();
        assertThat(conSector1.sector1()).isEqualByComparingTo("30.2");
        assertThat(conSector1.velocidadI1()).isEqualTo(231);
        assertThat(conSector1.sector2()).isNull();
        assertThat(conSector2.sector2()).isEqualByComparingTo("40.5");
        assertThat(conSector2.velocidadI2()).isEqualTo(251);
    }

    @Test
    void vueltaSinTiempo_noCuentaComoHechaYDejaDeEstarEnCursoALosCuatroMinutos() {
        // Verstappen empezó la vuelta 3 en el segundo 276 y OpenF1 todavía no le puso tiempo (como en vivo)
        DatosSesion datos = carrera();

        FilaFoto verstappen = CalculadoraFoto.filas(datos, segundo(300)).stream()
                .filter(fila -> fila.numero() == 1).findFirst().orElseThrow();

        assertThat(verstappen.vueltas()).isEqualTo(2);
        assertThat(CalculadoraFoto.vueltaEnCurso(datos.vueltas(), 1, segundo(300)).vuelta()).isEqualTo(3);
        // Cinco minutos después sigue sin tiempo: abandonó o entró a boxes
        assertThat(CalculadoraFoto.vueltaEnCurso(datos.vueltas(), 1, segundo(576))).isNull();
    }
}
