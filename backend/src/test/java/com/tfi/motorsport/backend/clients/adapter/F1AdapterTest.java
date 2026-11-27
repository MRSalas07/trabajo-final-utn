package com.tfi.motorsport.backend.clients.adapter;

import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.EventoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.ResultadoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.SesionNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.TemporadaNorm;
import com.tfi.motorsport.backend.config.AppProperties;
import com.tfi.motorsport.backend.handlers.exceptions.IntegrationExternalException;
import com.tfi.motorsport.backend.model.Catalogo.EstadoEvento;
import com.tfi.motorsport.backend.model.Catalogo.EstadoResultado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El adaptador de F1 traduce las respuestas de Jolpica (formato Ergast) al modelo normalizado.
 * Datos recortados del GP de Baréin 2024: horarios, resultados en dos páginas, clasificación y campeón.
 */
class F1AdapterTest {

    private ProveedorFalso jolpica;
    private F1Adapter adaptador;
    private final List<String> avisos = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        jolpica = new ProveedorFalso();
        AppProperties.Integracion integracion = new AppProperties.Integracion(2000, 2000, 0,
                new AppProperties.Proveedor(jolpica.urlBase(), 0), null, false, null);
        adaptador = new F1Adapter(new AppProperties(null, null, null, integracion, null), JsonMapper.builder().build());
    }

    @AfterEach
    void tearDown() {
        jolpica.close();
    }

    /** Baréin 2024 con sus horarios en UTC (como los publica Jolpica). */
    private static final String CARRERA_BAREIN = """
            {'season': '2024', 'round': '1', 'raceName': 'Bahrain Grand Prix', 'date': '2024-03-02', 'time': '15:00:00Z',
             'Circuit': {'circuitId': 'bahrain', 'circuitName': 'Bahrain International Circuit',
                         'Location': {'locality': 'Sakhir', 'country': 'Bahrain'}},
             'FirstPractice': {'date': '2024-02-29', 'time': '11:30:00Z'},
             'SecondPractice': {'date': '2024-02-29', 'time': '15:00:00Z'},
             'ThirdPractice': {'date': '2024-03-01', 'time': '12:30:00Z'},
             'Qualifying': {'date': '2024-03-01', 'time': '16:00:00Z'}}""";

    private void temporadaTerminada() {
        jolpica.responder("2024.json", "{'MRData': {'RaceTable': {'Races': [" + CARRERA_BAREIN + "]}}}");
        // Los resultados vienen paginados de a 100: la segunda página trae al que abandonó
        jolpica.responder("2024/results.json?limit=100&offset=0", """
                {'MRData': {'total': '150', 'RaceTable': {'Races': [{'round': '1', 'Results': [
                  {'number': '1', 'position': '1', 'positionText': '1', 'points': '26', 'laps': '57', 'status': 'Finished',
                   'Driver': {'driverId': 'max_verstappen', 'code': 'VER', 'permanentNumber': '33', 'givenName': 'Max',
                              'familyName': 'Verstappen', 'dateOfBirth': '1997-09-30', 'nationality': 'Dutch'},
                   'Constructor': {'constructorId': 'red_bull', 'name': 'Red Bull', 'nationality': 'Austrian'},
                   'Time': {'millis': '5504742', 'time': '1:31:44.742'}, 'FastestLap': {'Time': {'time': '1:32.608'}}},
                  {'number': '11', 'position': '2', 'positionText': '2', 'points': '18', 'laps': '57', 'status': 'Finished',
                   'Driver': {'driverId': 'perez', 'code': 'PER', 'givenName': 'Sergio', 'familyName': 'Pérez'},
                   'Constructor': {'constructorId': 'red_bull', 'name': 'Red Bull'},
                   'Time': {'millis': '5527199', 'time': '+22.457'}}]}]}}}""");
        jolpica.responder("2024/results.json?limit=100&offset=100", """
                {'MRData': {'total': '150', 'RaceTable': {'Races': [{'round': '1', 'Results': [
                  {'number': '2', 'position': '20', 'positionText': 'R', 'points': '0', 'laps': '0', 'status': 'Retired',
                   'Driver': {'driverId': 'sargeant', 'code': 'SAR', 'givenName': 'Logan', 'familyName': 'Sargeant'},
                   'Constructor': {'constructorId': 'williams', 'name': 'Williams'}}]}]}}}""");
        jolpica.responder("2024/qualifying.json?limit=100&offset=0", """
                {'MRData': {'total': '3', 'RaceTable': {'Races': [{'round': '1', 'QualifyingResults': [
                  {'number': '16', 'position': '2', 'Q1': '1:30.851', 'Q2': '1:30.243', 'Q3': '1:29.407',
                   'Driver': {'driverId': 'leclerc', 'code': 'LEC', 'givenName': 'Charles', 'familyName': 'Leclerc'},
                   'Constructor': {'constructorId': 'ferrari', 'name': 'Ferrari'}},
                  {'number': '1', 'position': '1', 'Q1': '1:30.031', 'Q2': '1:29.374', 'Q3': '1:29.179',
                   'Driver': {'driverId': 'max_verstappen', 'code': 'VER', 'givenName': 'Max', 'familyName': 'Verstappen'},
                   'Constructor': {'constructorId': 'red_bull', 'name': 'Red Bull'}},
                  {'number': '24', 'position': '20', 'Q1': '1:31.578',
                   'Driver': {'driverId': 'zhou', 'code': 'ZHO', 'givenName': 'Guanyu', 'familyName': 'Zhou'},
                   'Constructor': {'constructorId': 'sauber', 'name': 'Sauber'}}]}]}}}""");
        jolpica.responder("2024/sprint.json?limit=100&offset=0", "{'MRData': {'total': '0', 'RaceTable': {'Races': []}}}");
        jolpica.responder("2024/driverStandings.json?limit=1", """
                {'MRData': {'StandingsTable': {'StandingsLists': [{'DriverStandings': [
                  {'position': '1', 'Driver': {'driverId': 'max_verstappen'}}]}]}}}""");
    }

    @Test
    void elEventoSeNormalizaConSuCircuitoZonaHorariaYSesionesEnUtc() {
        temporadaTerminada();

        TemporadaNorm temporada = adaptador.obtenerTemporada(2024, avisos);

        assertThat(temporada.siglasCategoria()).isEqualTo("F1");
        assertThat(temporada.fuente()).isEqualTo("JOLPICA");
        EventoNorm barein = temporada.eventos().get(0);
        assertThat(barein.nombreCorto()).isEqualTo("Bahrain GP");
        assertThat(barein.numeroFecha()).isEqualTo(1);
        assertThat(barein.ciudad()).isEqualTo("Sakhir");
        assertThat(barein.zonaHoraria()).isEqualTo("Asia/Bahrain");
        assertThat(barein.estado()).isEqualTo(EstadoEvento.FINALIZADO);
        assertThat(barein.sesiones()).extracting(SesionNorm::idExterno).containsExactly("fp1", "fp2", "fp3", "q", "race");
        SesionNorm carrera = barein.sesiones().get(4);
        assertThat(carrera.inicio()).isEqualTo(Instant.parse("2024-03-02T15:00:00Z"));
        assertThat(carrera.fin()).isEqualTo(Instant.parse("2024-03-02T17:00:00Z"));
        assertThat(barein.inicio()).isEqualTo(Instant.parse("2024-02-29T11:30:00Z"));
    }

    @Test
    void losResultadosDeVariasPaginasSeJuntanConTiemposYDiferenciasEnSegundos() {
        temporadaTerminada();

        List<ResultadoNorm> carrera = adaptador.obtenerTemporada(2024, avisos).eventos().get(0).sesiones().get(4).resultados();

        assertThat(carrera).extracting(r -> r.piloto().idExterno())
                .containsExactly("F1:max_verstappen", "F1:perez", "F1:sargeant");
        ResultadoNorm verstappen = carrera.get(0);
        assertThat(verstappen.tiempo()).isEqualByComparingTo("5504.742");
        assertThat(verstappen.mejorVuelta()).isEqualByComparingTo("92.608");
        assertThat(verstappen.puntos()).isEqualByComparingTo("26");
        assertThat(carrera.get(1).diferencia()).isEqualByComparingTo("22.457");
        ResultadoNorm sargeant = carrera.get(2);
        assertThat(sargeant.estado()).isEqualTo(EstadoResultado.ABANDONO);
        assertThat(sargeant.motivoAbandono()).isEqualTo("Retired");
        assertThat(jolpica.pedidos()).contains("2024/results.json?limit=100&offset=100");
    }

    @Test
    void laClasificacionUsaElMejorTiempoDeQ3Q2OQ1YLaDiferenciaConLaPole() {
        temporadaTerminada();

        List<ResultadoNorm> clasificacion = adaptador.obtenerTemporada(2024, avisos).eventos().get(0).sesiones().get(3).resultados();

        ResultadoNorm leclerc = clasificacion.get(0);
        assertThat(leclerc.tiempo()).isEqualByComparingTo("89.407");
        // La pole (Verstappen 1:29.179) viene segunda en la lista: la diferencia igual se calcula bien
        assertThat(leclerc.diferencia()).isEqualByComparingTo("0.228");
        ResultadoNorm zhou = clasificacion.get(2);
        assertThat(zhou.tiempo()).isEqualByComparingTo("91.578");
    }

    @Test
    void conTodasLasCarrerasCorridas_seInformaElCampeonOficial() {
        temporadaTerminada();

        assertThat(adaptador.obtenerTemporada(2024, avisos).idExternoCampeon()).isEqualTo("F1:max_verstappen");
    }

    @Test
    void temporadaEnCurso_noPideElCampeonNiInventaResultados() {
        // Todas las sesiones (prácticas, clasificación y carrera) en el futuro
        String futura = CARRERA_BAREIN.replace("2024-", "2099-").replace("'round': '1'", "'round': '2'")
                .replace("Bahrain Grand Prix", "Gran Premio Futuro");
        temporadaTerminada();
        jolpica.responder("2024.json", "{'MRData': {'RaceTable': {'Races': [" + CARRERA_BAREIN + "," + futura + "]}}}");

        TemporadaNorm temporada = adaptador.obtenerTemporada(2024, avisos);

        assertThat(temporada.idExternoCampeon()).isNull();
        assertThat(temporada.eventos().get(1).estado()).isEqualTo(EstadoEvento.PROGRAMADO);
        assertThat(temporada.eventos().get(1).sesiones().get(4).resultados()).isEmpty();
        assertThat(jolpica.pedidos()).noneMatch(pedido -> pedido.startsWith("2024/driverStandings"));
    }

    @Test
    void sinCalendarioPublicado_lanzaRespuestaVacia() {
        jolpica.responder("2030.json", "{'MRData': {'RaceTable': {'Races': []}}}");

        assertThatThrownBy(() -> adaptador.obtenerTemporada(2030, avisos))
                .isInstanceOf(IntegrationExternalException.class)
                .extracting(ex -> ((IntegrationExternalException) ex).getTipo())
                .isEqualTo(IntegrationExternalException.Tipo.RESPUESTA_VACIA);
    }

    @Test
    void traduccionesDeJolpica() {
        assertThat(F1Adapter.tiempoVuelta("1:22.167")).isEqualByComparingTo("82.167");
        assertThat(F1Adapter.tiempoVuelta("58.1")).isEqualByComparingTo("58.100");
        assertThat(F1Adapter.tiempoVuelta("sin tiempo")).isNull();
        assertThat(F1Adapter.estadoResultado("D", "Disqualified")).isEqualTo(EstadoResultado.DESCALIFICADO);
        assertThat(F1Adapter.estadoResultado("W", "Withdrew")).isEqualTo(EstadoResultado.NO_LARGO);
        assertThat(F1Adapter.estadoResultado("12", "+1 Lap")).isEqualTo(EstadoResultado.FINALIZADO);
        assertThat(F1Adapter.estadoResultado("15", "Engine")).isEqualTo(EstadoResultado.ABANDONO);
    }
}
