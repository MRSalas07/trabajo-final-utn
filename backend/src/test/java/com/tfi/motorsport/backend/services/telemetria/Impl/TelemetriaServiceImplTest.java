package com.tfi.motorsport.backend.services.telemetria.Impl;

import com.tfi.motorsport.backend.clients.openf1.OpenF1Cliente;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SeriePosiciones;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SerieVueltas;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SesionTelemetria;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.TelemetriaSesion;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Análisis de sesiones de F1 con OpenF1 simulado: filtros, armado de series, pilotos que se muestran,
 * sesiones sin terminar y la caché que cuida el cupo de consultas.
 */
@ExtendWith(MockitoExtension.class)
class TelemetriaServiceImplTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final int CARRERA = 9839;

    @Mock
    private OpenF1Cliente openF1;

    private TelemetriaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TelemetriaServiceImpl(openF1);
    }

    /** JSON escrito con comillas simples para que se lea mejor. */
    private static JsonNode json(String texto) {
        return MAPPER.readTree(texto.replace('\'', '"'));
    }

    /** Una carrera terminada con 7 pilotos: Norris pasa a Verstappen a los 30 minutos. */
    private void carreraTerminada() {
        when(openF1.get("sessions?session_key=" + CARRERA)).thenReturn(json("""
                [{'session_key': 9839, 'session_name': 'Race', 'session_type': 'Race',
                  'date_start': '2025-11-22T04:00:00+00:00', 'date_end': '2025-11-22T06:00:00+00:00',
                  'country_name': 'United States', 'circuit_short_name': 'Las Vegas', 'location': 'Las Vegas'}]
                """));
        when(openF1.get("drivers?session_key=" + CARRERA)).thenReturn(json("""
                [{'driver_number': 1, 'full_name': 'Max VERSTAPPEN', 'name_acronym': 'VER', 'team_name': 'Red Bull Racing', 'team_colour': '3671C6'},
                 {'driver_number': 4, 'full_name': 'Lando NORRIS', 'name_acronym': 'NOR', 'team_name': 'McLaren', 'team_colour': 'FF8000'},
                 {'driver_number': 81, 'name_acronym': 'PIA', 'team_name': 'McLaren', 'team_colour': 'FF8000'},
                 {'driver_number': 16, 'name_acronym': 'LEC', 'team_name': 'Ferrari', 'team_colour': 'E8002D'},
                 {'driver_number': 44, 'name_acronym': 'HAM', 'team_name': 'Ferrari', 'team_colour': 'E8002D'},
                 {'driver_number': 63, 'name_acronym': 'RUS', 'team_name': 'Mercedes', 'team_colour': '27F4D2'},
                 {'driver_number': 12, 'name_acronym': 'ANT', 'team_name': 'Mercedes', 'team_colour': '27F4D2'}]
                """));
        when(openF1.get("position?session_key=" + CARRERA)).thenReturn(json("""
                [{'driver_number': 4, 'date': '2025-11-22T04:30:00.000000+00:00', 'position': 1},
                 {'driver_number': 1, 'date': '2025-11-22T04:03:00+00:00', 'position': 1},
                 {'driver_number': 4, 'date': '2025-11-22T04:03:00+00:00', 'position': 2},
                 {'driver_number': 81, 'date': '2025-11-22T04:03:00+00:00', 'position': 3},
                 {'driver_number': 16, 'date': '2025-11-22T04:03:00+00:00', 'position': 4},
                 {'driver_number': 44, 'date': '2025-11-22T04:03:00+00:00', 'position': 5},
                 {'driver_number': 63, 'date': '2025-11-22T04:03:00+00:00', 'position': 6},
                 {'driver_number': 12, 'date': '2025-11-22T04:03:00+00:00', 'position': 7},
                 {'driver_number': 1, 'date': '2025-11-22T04:30:00.000000+00:00', 'position': 2}]
                """));
        when(openF1.get("laps?session_key=" + CARRERA)).thenReturn(json("""
                [{'driver_number': 1, 'lap_number': 1, 'lap_duration': 95.123, 'is_pit_out_lap': false},
                 {'driver_number': 1, 'lap_number': 2, 'lap_duration': 120.5, 'is_pit_out_lap': true},
                 {'driver_number': 1, 'lap_number': 3, 'lap_duration': 93.4567, 'is_pit_out_lap': false},
                 {'driver_number': 1, 'lap_number': 4, 'lap_duration': null, 'is_pit_out_lap': false},
                 {'driver_number': 4, 'lap_number': 1, 'lap_duration': 94.2, 'is_pit_out_lap': false},
                 {'driver_number': 12, 'lap_number': 1, 'lap_duration': 96.0, 'is_pit_out_lap': false}]
                """));
    }

    // ---------------------------------------------------------------- sesiones de un año

    @Test
    void sesiones_omiteLasQueNoEmpezaronYOrdenaDeLaMasRecienteALaMasVieja() {
        String enUnMes = Instant.now().plus(Duration.ofDays(30)).toString();
        when(openF1.get("sessions?year=2025")).thenReturn(json("""
                [{'session_key': 1, 'session_name': 'Practice 1', 'date_start': '2025-03-14T01:30:00+00:00'},
                 {'session_key': 2, 'session_name': 'Race', 'date_start': '2025-03-16T04:00:00+00:00'},
                 {'session_key': 3, 'session_name': 'Race', 'date_start': '%s'}]
                """.formatted(enUnMes)));

        List<SesionTelemetria> sesiones = service.sesiones(2025);

        assertThat(sesiones).extracting(SesionTelemetria::sessionKey).containsExactly(2, 1);
    }

    @Test
    void sesiones_anioSinDatosEnOpenF1_lanza400SinConsultar() {
        int proximoAnio = Year.now(ZoneOffset.UTC).getValue() + 1;

        assertThatThrownBy(() -> service.sesiones(2022))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessageContaining("desde 2023");
        assertThatThrownBy(() -> service.sesiones(proximoAnio)).isInstanceOf(RuleBusinessException.class);
        verifyNoInteractions(openF1);
    }

    @Test
    void sesiones_quedanEnCacheParaNoGastarCupo() {
        when(openF1.get("sessions?year=2024")).thenReturn(json("[]"));

        service.sesiones(2024);
        service.sesiones(2024);

        verify(openF1, times(1)).get("sessions?year=2024");
    }

    // ---------------------------------------------------------------- análisis de una sesión

    @Test
    void sesion_terminada_armaPosicionesYVueltasDeLosMejorUbicados() {
        carreraTerminada();

        TelemetriaSesion analisis = service.sesion(CARRERA, List.of());

        assertThat(analisis.aviso()).isNull();
        assertThat(analisis.sesion().circuito()).isEqualTo("Las Vegas");
        assertThat(analisis.pilotos()).hasSize(7);
        assertThat(analisis.pilotos().get(0).color()).isEqualTo("#3671C6");

        SeriePosiciones verstappen = analisis.posiciones().stream().filter(s -> s.numero() == 1).findFirst().orElseThrow();
        assertThat(verstappen.acronimo()).isEqualTo("VER");
        assertThat(verstappen.minutos()).containsExactly(new BigDecimal("3.0"), new BigDecimal("30.0"));
        assertThat(verstappen.posiciones()).containsExactly(1, 2);

        // Se muestran los 6 mejor ubicados al final (Antonelli, 7.º, queda afuera aunque tenga vueltas)
        // y solo los que tienen vueltas con tiempo: Norris (1.º) y Verstappen (2.º)
        assertThat(analisis.vueltas()).extracting(SerieVueltas::acronimo).containsExactly("NOR", "VER");
        SerieVueltas vueltasVer = analisis.vueltas().get(1);
        assertThat(vueltasVer.vueltas()).containsExactly(1, 3);
        assertThat(vueltasVer.tiempos()).containsExactly(new BigDecimal("95.123"), new BigDecimal("93.457"));
    }

    @Test
    void sesion_pilotosElegidos_respetaElOrdenYElTopeDeSeis() {
        carreraTerminada();

        TelemetriaSesion analisis = service.sesion(CARRERA, List.of(12, 63, 44, 16, 81, 1, 4));

        // Norris es el 7.º elegido: queda afuera por el tope de 6
        assertThat(analisis.vueltas()).extracting(SerieVueltas::acronimo).containsExactly("ANT", "VER");
    }

    @Test
    void sesion_consultadaDeNuevo_noVuelveAPedirLosDatos() {
        carreraTerminada();

        service.sesion(CARRERA, List.of());
        service.sesion(CARRERA, List.of(4));

        verify(openF1, times(1)).get("sessions?session_key=" + CARRERA);
        verify(openF1, times(1)).get("drivers?session_key=" + CARRERA);
        verify(openF1, times(1)).get("position?session_key=" + CARRERA);
        verify(openF1, times(1)).get("laps?session_key=" + CARRERA);
    }

    @Test
    void sesion_queTodaviaNoTermino_devuelveUnAvisoSinPedirMasDatos() {
        String inicio = Instant.now().minus(Duration.ofMinutes(30)).toString();
        String fin = Instant.now().plus(Duration.ofMinutes(90)).toString();
        when(openF1.get("sessions?session_key=9900")).thenReturn(json("""
                [{'session_key': 9900, 'session_name': 'Race', 'date_start': '%s', 'date_end': '%s'}]
                """.formatted(inicio, fin)));

        TelemetriaSesion analisis = service.sesion(9900, null);

        assertThat(analisis.aviso()).contains("todavía no terminó");
        assertThat(analisis.vueltas()).isEmpty();
        assertThat(analisis.posiciones()).isEmpty();
        verify(openF1, never()).get("laps?session_key=9900");
    }

    @Test
    void sesion_inexistenteEnOpenF1_lanza404() {
        when(openF1.get("sessions?session_key=1")).thenReturn(json("[]"));

        assertThatThrownBy(() -> service.sesion(1, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No existe la sesión 1 en OpenF1");
    }

    @Test
    void color_agregaElNumeralQueOpenF1NoManda() {
        assertThat(TelemetriaServiceImpl.color("3671C6")).isEqualTo("#3671C6");
        assertThat(TelemetriaServiceImpl.color("#FF8000")).isEqualTo("#FF8000");
        assertThat(TelemetriaServiceImpl.color(" ")).isNull();
        assertThat(TelemetriaServiceImpl.color(null)).isNull();
    }
}
