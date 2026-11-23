package com.tfi.motorsport.backend.services.telemetria.transmision;

import com.tfi.motorsport.backend.clients.openf1.OpenF1Cliente;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SesionTelemetria;
import com.tfi.motorsport.backend.dtos.telemetria.TransmisionDtos.FilaTorre;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.ConFecha;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Piloto;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Posicion;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Vuelta;
import com.tfi.motorsport.backend.services.telemetria.transmision.Repeticion.DatosSesion;
import com.tfi.motorsport.backend.services.telemetria.transmision.Transmision.Suscripcion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Repetición de una sesión: el reloj virtual (velocidad, pausa, espera de la telemetría y fin)
 * y el punto de partida por número de vuelta.
 */
class RepeticionTest {

    private static final Instant T0 = Instant.parse("2025-12-07T13:00:00Z");

    private OpenF1Cliente openF1;

    /** Ejecuta las descargas en el mismo hilo, así el test no depende de tiempos. */
    private static final class EjecutorDirecto extends AbstractExecutorService {
        private boolean apagado;

        @Override
        public void execute(Runnable tarea) {
            tarea.run();
        }

        @Override
        public void shutdown() {
            apagado = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            apagado = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return apagado;
        }

        @Override
        public boolean isTerminated() {
            return apagado;
        }

        @Override
        public boolean awaitTermination(long tiempo, TimeUnit unidad) {
            return true;
        }
    }

    @BeforeEach
    void setUp() {
        openF1 = mock(OpenF1Cliente.class);
        // Los tramos de telemetría (ubicaciones y datos del auto) llegan vacíos
        when(openF1.get(anyString())).thenReturn(JsonMapper.builder().build().readTree("[]"));
    }

    /** Vuelta que empieza en T0 + inicioSeg y dura duracionSeg (OpenF1 la informa con su hora de fin). */
    private static Vuelta vuelta(int piloto, int numero, long inicioSeg, String duracionSeg) {
        BigDecimal duracion = new BigDecimal(duracionSeg);
        Instant fin = T0.plusSeconds(inicioSeg).plusMillis(duracion.multiply(BigDecimal.valueOf(1000)).longValue());
        return new Vuelta(fin, piloto, numero, duracion, false);
    }

    /** Carrera de 2 horas: Norris lidera desde el segundo 1 y completa dos vueltas. */
    private static DatosSesion carrera() {
        SesionTelemetria sesion = new SesionTelemetria(9839, "Race", "Race", T0, T0.plus(Duration.ofHours(2)),
                "United Arab Emirates", "Yas Marina Circuit", "Yas Island");
        List<ConFecha> eventos = List.of(
                new Posicion(T0.plusSeconds(1), 4, 1),
                new Posicion(T0.plusSeconds(1), 1, 2),
                vuelta(4, 1, 0, "95.0"),
                vuelta(4, 2, 95, "94.0"));
        return new DatosSesion(sesion, List.of(new Piloto(4, "NOR", "Lando Norris", "McLaren", "#FF8000"),
                new Piloto(1, "VER", "Max Verstappen", "Red Bull Racing", "#3671C6")), List.of(), eventos);
    }

    private Repeticion repeticion(double velocidad, int inicioMin, Integer inicioVuelta) {
        return new Repeticion(openF1, new EjecutorDirecto(), sessionKey -> carrera(), new ConcurrentHashMap<>(),
                9839, velocidad, inicioMin, inicioVuelta);
    }

    // ---------------------------------------------------------------- reloj virtual

    @Test
    void elRelojAvanzaSegunLaVelocidadCuandoLlegaLaTelemetria() {
        Repeticion repeticion = repeticion(5, 0, null);
        repeticion.agregar(new Suscripcion("espectador", new SseEmitter(), null));
        repeticion.iniciar();
        assertThat(repeticion.fase()).isEqualTo(Transmision.EN_CURSO);
        assertThat(repeticion.estado().reloj()).isEqualTo(T0);

        // Primer paso: pide el tramo de ubicaciones y espera a tenerlo para no desincronizarse
        repeticion.avanzar();
        assertThat(repeticion.fase()).isEqualTo(Transmision.ESPERANDO_DATOS);
        assertThat(repeticion.estado().reloj()).isEqualTo(T0);

        // Con el tramo disponible avanza 5 segundos (x5) y aplica lo que pasó en ese lapso
        repeticion.avanzar();
        assertThat(repeticion.fase()).isEqualTo(Transmision.EN_CURSO);
        assertThat(repeticion.estado().reloj()).isEqualTo(T0.plusSeconds(5));
        assertThat(repeticion.estado().torre()).extracting(FilaTorre::acronimo).containsExactly("NOR", "VER");
    }

    @Test
    void pausadaNoAvanza() {
        Repeticion repeticion = repeticion(1, 0, null);
        Suscripcion espectador = new Suscripcion("espectador", new SseEmitter(), null);
        repeticion.agregar(espectador);
        repeticion.iniciar();
        repeticion.setPausada(true);

        repeticion.avanzar();
        repeticion.avanzar();

        assertThat(repeticion.estado().reloj()).isEqualTo(T0);
        assertThat(repeticion.foto(espectador).estado()).isEqualTo(Transmision.PAUSADA);
    }

    @Test
    void terminaDosMinutosDespuesDelFinal() {
        Repeticion repeticion = repeticion(1, 123, null);
        repeticion.agregar(new Suscripcion("espectador", new SseEmitter(), null));
        repeticion.iniciar();

        repeticion.avanzar(); // pide el tramo de ubicaciones
        repeticion.avanzar();

        assertThat(repeticion.fase()).isEqualTo(Transmision.FINALIZADA);
        assertThat(repeticion.terminada()).isTrue();
    }

    // ---------------------------------------------------------------- inicio por vuelta

    @Test
    void empezarEnUnaVueltaArrancaCincoSegundosAntesDeQueLaEmpieceElLider() {
        Repeticion repeticion = repeticion(1, 0, 2);

        repeticion.iniciar();

        // La vuelta 2 empieza a los 95 s: se arranca a los 90 s, con el líder terminando la vuelta 1
        assertThat(repeticion.estado().reloj()).isEqualTo(T0.plusSeconds(90));
        assertThat(repeticion.estado().vueltaLider()).isEqualTo(1);
    }

    @Test
    void empezarEnUnaVueltaQueNoExisteEsUnError() {
        Repeticion repeticion = repeticion(1, 0, 9);

        repeticion.iniciar();

        assertThat(repeticion.fase()).isEqualTo(Transmision.ERROR);
        assertThat(repeticion.aviso()).contains("elegí una entre 1 y 2");
    }

    @Test
    void elInicioDeLaVueltaSeCalculaDesdeSuFinYDuracion() {
        assertThat(vuelta(4, 2, 100, "88.250").inicio()).isEqualTo(T0.plusSeconds(100));
    }

    @Test
    void laVueltaEmpiezaCuandoLaEmpiezaElLider() {
        List<ConFecha> eventos = List.of(
                new Posicion(T0, 4, 1),
                vuelta(4, 2, 100, "88.0"),   // líder: empieza la vuelta 2 primero
                vuelta(1, 2, 103, "87.5"),   // segundo, 3 s después
                vuelta(4, 3, 188, "87.9"),
                vuelta(1, 3, 190, "88.1"));

        assertThat(Repeticion.inicioDeVuelta(eventos, 2)).contains(T0.plusSeconds(100));
        assertThat(Repeticion.inicioDeVuelta(eventos, 3)).contains(T0.plusSeconds(188));
        assertThat(Repeticion.totalVueltas(eventos)).isEqualTo(3);
    }

    @Test
    void vueltaSinDatosDevuelveVacio() {
        List<ConFecha> eventos = List.of(vuelta(4, 2, 100, "88.0"));

        assertThat(Repeticion.inicioDeVuelta(eventos, 1)).isEmpty();
        assertThat(Repeticion.inicioDeVuelta(eventos, 9)).isEmpty();
        assertThat(Repeticion.totalVueltas(List.of())).isZero();
    }
}
