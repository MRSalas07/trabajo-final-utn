package com.tfi.motorsport.backend.services.telemetria.Impl;

import com.tfi.motorsport.backend.clients.openf1.OpenF1Cliente;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.FilaFoto;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.FotoSesion;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Foto de la sesión con OpenF1 simulado: qué instante se elige, el versus, qué se consulta y la caché.
 * Carrera de 2 vueltas: Verstappen termina primero la vuelta 1, Norris lo pasa y gana.
 */
@ExtendWith(MockitoExtension.class)
class TelemetriaFotoTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final int CARRERA = 9472;
    private static final String FILTRO = "?session_key=" + CARRERA;
    private static final Instant FIN_VUELTA_1 = Instant.parse("2024-03-02T15:01:35Z");

    @Mock
    private OpenF1Cliente openF1;

    private TelemetriaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TelemetriaServiceImpl(openF1);
    }

    private static JsonNode json(String texto) {
        return MAPPER.readTree(texto.replace('\'', '"'));
    }

    private void sesionTerminada() {
        when(openF1.get("sessions" + FILTRO)).thenReturn(json("""
                [{'session_key': 9472, 'session_name': 'Race', 'session_type': 'Race',
                  'date_start': '2024-03-02T15:00:00+00:00', 'date_end': '2024-03-02T17:00:00+00:00',
                  'country_name': 'Bahrain', 'circuit_short_name': 'Sakhir', 'location': 'Sakhir'}]
                """));
    }

    private void datosDeLaCarrera() {
        when(openF1.get("drivers" + FILTRO)).thenReturn(json("""
                [{'driver_number': 1, 'full_name': 'Max VERSTAPPEN', 'name_acronym': 'VER', 'team_name': 'Red Bull Racing', 'team_colour': '3671C6', 'headshot_url': 'https://media.formula1.com/ver.png'},
                 {'driver_number': 4, 'full_name': 'Lando NORRIS', 'name_acronym': 'NOR', 'team_name': 'McLaren', 'team_colour': 'FF8000'}]
                """));
        when(openF1.get("position" + FILTRO)).thenReturn(json("""
                [{'driver_number': 1, 'date': '2024-03-02T14:05:00+00:00', 'position': 1},
                 {'driver_number': 1, 'date': '2024-03-02T15:00:00+00:00', 'position': 1},
                 {'driver_number': 4, 'date': '2024-03-02T15:00:00+00:00', 'position': 2},
                 {'driver_number': 4, 'date': '2024-03-02T15:02:30+00:00', 'position': 1},
                 {'driver_number': 1, 'date': '2024-03-02T15:02:30+00:00', 'position': 2}]
                """));
        when(openF1.get("intervals" + FILTRO)).thenReturn(json("[]"));
        when(openF1.get("laps" + FILTRO)).thenReturn(json("""
                [{'driver_number': 1, 'lap_number': 1, 'date_start': '2024-03-02T15:00:00+00:00', 'lap_duration': 95.0},
                 {'driver_number': 4, 'lap_number': 1, 'date_start': '2024-03-02T15:00:00+00:00', 'lap_duration': 96.0},
                 {'driver_number': 4, 'lap_number': 2, 'date_start': '2024-03-02T15:01:36+00:00', 'lap_duration': 89.0},
                 {'driver_number': 1, 'lap_number': 2, 'date_start': '2024-03-02T15:01:35+00:00', 'lap_duration': 92.0}]
                """));
        when(openF1.get("stints" + FILTRO)).thenReturn(json("[]"));
        when(openF1.get("pit" + FILTRO)).thenReturn(json("[]"));
        when(openF1.get("race_control" + FILTRO)).thenReturn(json("[]"));
        when(openF1.get("weather" + FILTRO)).thenReturn(json("[]"));
    }

    @Test
    void fotoEnLaVuelta1_muestraComoEstabaCuandoElLiderLaTermino() {
        sesionTerminada();
        datosDeLaCarrera();

        FotoSesion foto = service.foto(CARRERA, 1, null, List.of());

        assertThat(foto.enVivo()).isFalse();
        assertThat(foto.instante()).isEqualTo(FIN_VUELTA_1);
        assertThat(foto.vuelta()).isEqualTo(1);
        assertThat(foto.totalVueltas()).isEqualTo(2);
        assertThat(foto.tanda()).isNull();
        assertThat(foto.filas()).extracting(FilaFoto::acronimo).containsExactly("VER", "NOR");
        assertThat(foto.aviso()).isNull();
    }

    @Test
    void fotoSinVuelta_esElFinalDeLaSesion() {
        sesionTerminada();
        datosDeLaCarrera();
        Instant ultimoDato = Instant.parse("2024-03-02T15:03:07Z");

        FotoSesion foto = service.foto(CARRERA, null, null, null);

        assertThat(foto.instante()).isEqualTo(ultimoDato);
        assertThat(foto.finDatos()).isEqualTo(ultimoDato);
        // Hay posiciones desde las 14:05 (la previa), pero la línea de tiempo empieza a la hora oficial
        assertThat(foto.inicioDatos()).isEqualTo(Instant.parse("2024-03-02T15:00:00Z"));
        assertThat(foto.vuelta()).isEqualTo(2);
        assertThat(foto.filas()).extracting(FilaFoto::acronimo).containsExactly("NOR", "VER");
    }

    @Test
    void repeticion_cadaSegundoSeCalculaEnMemoriaSinVolverAConsultar() {
        sesionTerminada();
        datosDeLaCarrera();
        Instant inicio = Instant.parse("2024-03-02T15:02:00Z");

        for (int segundo = 0; segundo < 60; segundo++) {
            service.foto(CARRERA, null, inicio.plusSeconds(segundo), null);
        }
        FotoSesion despues = service.foto(CARRERA, null, Instant.parse("2024-03-02T15:02:31Z"), null);

        assertThat(despues.filas()).extracting(FilaFoto::acronimo).containsExactly("NOR", "VER");
        verify(openF1, times(1)).get("laps" + FILTRO);
        verify(openF1, times(1)).get("position" + FILTRO);
    }

    @Test
    void instanteFueraDeLaSesion_seAcotaALosDatos() {
        sesionTerminada();
        datosDeLaCarrera();

        FotoSesion foto = service.foto(CARRERA, null, Instant.parse("2030-01-01T00:00:00Z"), null);

        assertThat(foto.instante()).isEqualTo(foto.finDatos());
    }

    @Test
    void versus_conLosPilotosElegidosOLosMejorUbicados() {
        sesionTerminada();
        datosDeLaCarrera();

        FotoSesion elegidos = service.foto(CARRERA, null, null, List.of(1, 4));
        FotoSesion porDefecto = service.foto(CARRERA, null, null, null);

        assertThat(elegidos.versus().a().acronimo()).isEqualTo("VER");
        assertThat(elegidos.versus().a().foto()).isEqualTo("https://media.formula1.com/ver.png");
        assertThat(elegidos.versus().vueltas()).hasSize(2);
        assertThat(porDefecto.versus().a().acronimo()).isEqualTo("NOR");
        assertThat(porDefecto.versus().b().acronimo()).isEqualTo("VER");
    }

    @Test
    void versusDeTresPilotos_lanza400SinConsultar() {
        assertThatThrownBy(() -> service.foto(CARRERA, null, null, List.of(1, 4, 16)))
                .isInstanceOf(RuleBusinessException.class);
        verifyNoInteractions(openF1);
    }

    @Test
    void sesionEnCursoSinSuscripcion_avisaSinDescargarDatos() {
        Instant inicio = Instant.now().minus(20, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        when(openF1.get("sessions" + FILTRO)).thenReturn(json("[{'session_key': 9472, 'session_name': 'Race', "
                + "'date_start': '" + inicio + "', 'date_end': '" + inicio.plus(2, ChronoUnit.HOURS) + "'}]"));
        when(openF1.credencialesConfiguradas()).thenReturn(false);

        FotoSesion foto = service.foto(CARRERA, null, null, null);

        assertThat(foto.enVivo()).isTrue();
        assertThat(foto.aviso()).contains("suscriptores");
        assertThat(foto.filas()).isEmpty();
        verify(openF1, never()).getVivo(anyString());
        verify(openF1, never()).get("drivers" + FILTRO);
    }
}
