package com.tfi.motorsport.backend.services.telemetria.Impl;

import com.tfi.motorsport.backend.clients.openf1.OpenF1Cliente;
import com.tfi.motorsport.backend.config.AppProperties;
import com.tfi.motorsport.backend.dtos.telemetria.TransmisionDtos.InfoVivo;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.services.telemetria.transmision.Repeticion.DatosSesion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Transmisiones: controles, estado del vivo y descarga de los datos de una sesión para repetirla
 * (con OpenF1 simulado).
 */
@ExtendWith(MockitoExtension.class)
class TransmisionServiceImplTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Mock
    private OpenF1Cliente openF1;

    private TransmisionServiceImpl service;

    @BeforeEach
    void setUp() {
        AppProperties.Telemetria cfg = new AppProperties.Telemetria(true, "https://api.openf1.org/v1/", 400, 28,
                "https://api.openf1.org/token", "", "", 55, 5, 8000, 30000);
        service = new TransmisionServiceImpl(openF1, new AppProperties(null, null, null, null, cfg));
    }

    @AfterEach
    void tearDown() {
        service.detener();
    }

    private static JsonNode json(String texto) {
        return MAPPER.readTree(texto.replace('\'', '"'));
    }

    /** Sesión que empezó hace "haceMinutos" y dura "duracionMinutos". */
    private static JsonNode sesion(int sessionKey, long haceMinutos, long duracionMinutos) {
        Instant inicio = Instant.now().minus(Duration.ofMinutes(haceMinutos));
        return json("""
                [{'session_key': %d, 'session_name': 'Race', 'session_type': 'Race',
                  'date_start': '%s', 'date_end': '%s', 'circuit_short_name': 'Interlagos'}]
                """.formatted(sessionKey, inicio, inicio.plus(Duration.ofMinutes(duracionMinutos))));
    }

    // ---------------------------------------------------------------- controles

    @Test
    void ajustar_velocidadFueraDeRango_lanza400() {
        assertThatThrownBy(() -> service.ajustar("cualquiera", null, 20.0, null))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessageContaining("entre 0.5x y 10x");
        assertThatThrownBy(() -> service.ajustar("cualquiera", null, 0.1, null))
                .isInstanceOf(RuleBusinessException.class);
    }

    @Test
    void ajustar_transmisionQueNoExiste_lanza404() {
        assertThatThrownBy(() -> service.ajustar("no-existe", 44, null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no-existe");
    }

    @Test
    void laVelocidadPedidaSeAcotaAlRangoPermitido() {
        assertThat(TransmisionServiceImpl.acotar(20)).isEqualTo(10.0);
        assertThat(TransmisionServiceImpl.acotar(0.1)).isEqualTo(0.5);
        assertThat(TransmisionServiceImpl.acotar(Double.NaN)).isEqualTo(1.0);
        assertThat(TransmisionServiceImpl.acotar(2)).isEqualTo(2.0);
    }

    // ---------------------------------------------------------------- modo en vivo

    @Test
    void vivo_sinSuscripcion_noConsultaOpenF1() {
        when(openF1.credencialesConfiguradas()).thenReturn(false);

        service.vivo("latest", null);

        verify(openF1, never()).getVivo(anyString());
    }

    @Test
    void infoVivo_sinSuscripcion_loInformaYQuedaGuardadoUnMinuto() {
        when(openF1.credencialesConfiguradas()).thenReturn(false);
        when(openF1.get("sessions?session_key=latest")).thenReturn(sesion(9839, 600, 120));

        InfoVivo info = service.infoVivo();
        service.infoVivo();

        assertThat(info.credenciales()).isFalse();
        assertThat(info.ultimaSesion().sessionKey()).isEqualTo(9839);
        assertThat(info.enVentanaVivo()).isFalse();
        assertThat(info.mensaje()).contains("requiere la suscripción");
        verify(openF1, times(1)).get("sessions?session_key=latest");
    }

    @Test
    void infoVivo_conSuscripcionYUnaSesionEnCurso() {
        when(openF1.credencialesConfiguradas()).thenReturn(true);
        when(openF1.getVivo("sessions?session_key=latest")).thenReturn(sesion(9900, 20, 120));

        InfoVivo info = service.infoVivo();

        assertThat(info.credenciales()).isTrue();
        assertThat(info.enVentanaVivo()).isTrue();
        assertThat(info.mensaje()).isEqualTo("¡Hay una sesión en curso!");
    }

    // ---------------------------------------------------------------- datos para la repetición

    @Test
    void datosSesion_deUnaSesionQueNoTermino_lanzaErrorSinDescargarMas() {
        when(openF1.get("sessions?session_key=9900")).thenReturn(sesion(9900, 20, 120));

        assertThatThrownBy(() -> service.datosSesion(9900))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessageContaining("todavía no terminó");
        verify(openF1, never()).get("position?session_key=9900");
    }

    @Test
    void datosSesion_seDescarganUnaVezYQuedanOrdenadosEnElTiempo() {
        when(openF1.get(anyString())).thenAnswer(invocacion -> {
            String ruta = invocacion.getArgument(0);
            if (ruta.startsWith("sessions")) {
                return sesion(9839, 600, 120);
            }
            if (ruta.startsWith("position")) {
                return json("""
                        [{'driver_number': 4, 'date': '2025-12-07T13:00:05+00:00', 'position': 1},
                         {'driver_number': 1, 'date': '2025-12-07T13:00:01+00:00', 'position': 1}]
                        """);
            }
            if (ruta.startsWith("drivers")) {
                return json("[{'driver_number': 4, 'name_acronym': 'NOR'}, {'driver_number': 1, 'name_acronym': 'VER'}]");
            }
            return json("[]");
        });

        DatosSesion datos = service.datosSesion(9839);
        service.datosSesion(9839);

        assertThat(datos.pilotos()).hasSize(2);
        assertThat(datos.eventos()).hasSize(2);
        assertThat(datos.eventos().get(0).fecha()).isEqualTo(Instant.parse("2025-12-07T13:00:01Z"));
        verify(openF1, times(1)).get("sessions?session_key=9839");
        verify(openF1, times(1)).get("position?session_key=9839");
    }
}
