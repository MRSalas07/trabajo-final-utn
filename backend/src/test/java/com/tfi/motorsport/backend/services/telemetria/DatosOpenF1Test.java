package com.tfi.motorsport.backend.services.telemetria;

import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SesionTelemetria;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Piloto;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Vuelta;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Lectura tolerante de los datos de OpenF1 y la regla de cuándo quedan libres los datos de una sesión. */
class DatosOpenF1Test {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static JsonNode json(String texto) {
        return MAPPER.readTree(texto.replace('\'', '"'));
    }

    @Test
    void diferenciasDeOpenF1SeTraducen() {
        assertThat(DatosOpenF1.diferencia(json("1.5"))).isEqualTo("+1.500");
        assertThat(DatosOpenF1.diferencia(json("'+1 LAP'"))).isEqualTo("+1 VUELTA");
        assertThat(DatosOpenF1.diferencia(json("'+2 LAPS'"))).isEqualTo("+2 VUELTAS");
        assertThat(DatosOpenF1.diferencia(json("null"))).isNull();
    }

    @Test
    void laVueltaSeAplicaCuandoTermina() {
        List<Vuelta> vueltas = DatosOpenF1.vueltas(json("""
                [{'driver_number': 4, 'lap_number': 2, 'date_start': '2025-12-07T13:01:40+00:00', 'lap_duration': 88.25},
                 {'driver_number': 4, 'lap_number': 3, 'lap_duration': 87.9}]
                """));

        assertThat(vueltas).hasSize(1); // sin hora de inicio no se puede ubicar en el tiempo
        assertThat(vueltas.get(0).fecha()).isEqualTo(Instant.parse("2025-12-07T13:03:08.250Z"));
        assertThat(vueltas.get(0).inicio()).isEqualTo(Instant.parse("2025-12-07T13:01:40Z"));
    }

    @Test
    void laVueltaTraeSectoresYVelocidades() {
        List<Vuelta> vueltas = DatosOpenF1.vueltas(json("""
                [{'driver_number': 1, 'lap_number': 10, 'date_start': '2024-03-02T15:18:15.404000+00:00',
                  'lap_duration': 96.96, 'duration_sector_1': 30.986, 'duration_sector_2': 42.009,
                  'duration_sector_3': 23.965, 'i1_speed': 232, 'i2_speed': 244, 'st_speed': 293,
                  'is_pit_out_lap': false}]
                """));

        Vuelta vuelta = vueltas.get(0);
        assertThat(vuelta.sector2()).isEqualByComparingTo("42.009");
        assertThat(vuelta.velocidadTrampa()).isEqualTo(293);
    }

    @Test
    void pilotoSinColorUsaElColorPorDefecto() {
        List<Piloto> pilotos = DatosOpenF1.pilotos(json("""
                [{'driver_number': 1, 'name_acronym': 'VER', 'team_colour': '3671C6'},
                 {'driver_number': 99, 'name_acronym': 'XXX'},
                 {'name_acronym': 'SIN'}]
                """));

        assertThat(pilotos).extracting(Piloto::color).containsExactly("#3671C6", DatosOpenF1.COLOR_POR_DEFECTO);
    }

    @Test
    void losDatosQuedanLibresMediaHoraDespuesDelFinal() {
        Instant fin = Instant.parse("2025-12-07T15:00:00Z");
        SesionTelemetria carrera = new SesionTelemetria(9839, "Race", "Race", fin.minus(Duration.ofHours(2)), fin,
                "United Arab Emirates", "Yas Marina Circuit", "Yas Island");

        assertThat(DatosOpenF1.motivoSinDatosLibres(carrera, fin.minus(Duration.ofMinutes(10))))
                .contains("todavía no terminó");
        assertThat(DatosOpenF1.motivoSinDatosLibres(carrera, fin.plus(Duration.ofMinutes(10))))
                .contains("menos de 30 minutos");
        assertThat(DatosOpenF1.motivoSinDatosLibres(carrera, fin.plus(Duration.ofMinutes(31)))).isNull();
    }

    @Test
    void sesionSeLeeConSusHorarios() {
        SesionTelemetria sesion = DatosOpenF1.sesion(json("""
                {'session_key': 9839, 'session_name': 'Race', 'session_type': 'Race',
                 'date_start': '2025-12-07T13:00:00+00:00', 'date_end': '2025-12-07T15:00:00+00:00',
                 'country_name': 'United Arab Emirates', 'circuit_short_name': 'Yas Marina Circuit'}
                """));

        assertThat(sesion.sessionKey()).isEqualTo(9839);
        assertThat(sesion.fin()).isEqualTo(Instant.parse("2025-12-07T15:00:00Z"));
        assertThat(sesion.circuito()).isEqualTo("Yas Marina Circuit");
        assertThat(sesion.ubicacion()).isNull();
    }

    @Test
    void duracionesConDecimalesSeRespetan() {
        assertThat(DatosOpenF1.vueltas(json("""
                [{'driver_number': 4, 'lap_number': 1, 'date_start': '2025-12-07T13:00:00+00:00', 'lap_duration': 95.5}]
                """)).get(0).duracion()).isEqualByComparingTo(new BigDecimal("95.5"));
    }

    @Test
    void elPilotoTraeElEnlaceASuFoto() {
        List<Piloto> pilotos = DatosOpenF1.pilotos(json(
                "[{'driver_number': 1, 'name_acronym': 'VER', 'headshot_url': 'https://media.formula1.com/ver.png'}]"));

        assertThat(pilotos.get(0).foto()).isEqualTo("https://media.formula1.com/ver.png");
    }
}
