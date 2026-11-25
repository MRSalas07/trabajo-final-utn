package com.tfi.motorsport.backend.clients.adapter;

import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.EtapaNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.EventoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.ResultadoEtapaNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.TemporadaNorm;
import com.tfi.motorsport.backend.config.AppProperties;
import com.tfi.motorsport.backend.handlers.exceptions.IntegrationExternalException;
import com.tfi.motorsport.backend.model.Catalogo;
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
 * El adaptador de WRC traduce la API (no oficial) de wrc.com al modelo normalizado.
 * Rally de Montecarlo recortado: dos tramos, la clasificación general, puntos y un abandono.
 * Un segundo rally no responde, para comprobar que se omite con un aviso y no frena al resto.
 */
class WrcAdapterTest {

    private ProveedorFalso wrc;
    private WrcAdapter adaptador;
    private final List<String> avisos = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        wrc = new ProveedorFalso();
        AppProperties.Integracion integracion = new AppProperties.Integracion(2000, 2000, 0, null,
                new AppProperties.ProveedorWrc(wrc.urlBase(), 0, true), false, null);
        adaptador = new WrcAdapter(new AppProperties(null, null, null, integracion, null), JsonMapper.builder().build());
    }

    @AfterEach
    void tearDown() {
        wrc.close();
    }

    private static String persona(long id, String nombre, String apellido, String pais) {
        return "{'personId': " + id + ", 'firstName': '" + nombre + "', 'lastName': '" + apellido
                + "', 'country': {'iso3': '" + pais + "', 'name': '" + pais + "'}}";
    }

    private void temporada2024() {
        wrc.responder("seasons.json", """
                [{'seasonId': 31, 'year': 2024, 'name': 'Junior WRC'},
                 {'seasonId': 30, 'year': 2024, 'name': 'World Rally Championship'}]""");
        // El orden de las rondas viene desordenado: Suecia (2) antes que Montecarlo (1)
        wrc.responder("season-detail.json?seasonId=30", """
                {'championships': [{'type': 'Drivers', 'name': 'FIA World Rally Championship for Drivers', 'championshipId': 500}],
                 'seasonRounds': [
                   {'order': 2, 'event': {'eventId': 200, 'name': 'Rally Sweden', 'timeZoneId': 'Europe/Stockholm',
                     'startDate': '2024-02-15', 'finishDate': '2024-02-18', 'location': 'Umea, Sweden', 'country': {'name': 'Sweden'}}},
                   {'order': 1, 'event': {'eventId': 100, 'name': 'Rallye Monte-Carlo', 'timeZoneId': 'Europe/Monaco',
                     'startDate': '2024-01-25', 'finishDate': '2024-01-28', 'location': 'Gap, France', 'country': {'name': 'Monaco'}}}]}""");
        wrc.responder("championship-detail.json?championshipId=500&seasonId=30", """
                {'championshipEntries': [{'championshipEntryId': 1, 'personId': 11}, {'championshipEntryId': 2, 'personId': 12}]}""");
        wrc.responder("championship-overall-results.json?championshipId=500&seasonId=30", """
                {'entryResults': [{'championshipEntryId': 1, 'roundResults': [{'eventId': 100, 'totalPoints': 30}]},
                                  {'championshipEntryId': 2, 'roundResults': [{'eventId': 100, 'totalPoints': 18}]}]}""");

        wrc.responder("events/100.json", "{'rallies': [{'rallyId': 899, 'isMain': false}, {'rallyId': 900, 'isMain': true}]}");
        wrc.responder("events/100/rallies/900/entries.json", "["
                + "{'entryId': 1001, 'identifier': '17', 'group': {'name': 'Rally1'},"
                + " 'driver': " + persona(11, "Sébastien", "OGIER", "FRA") + ", 'codriver': " + persona(21, "Vincent", "LANDAIS", "FRA") + ","
                + " 'entrant': {'entrantId': 7, 'name': 'TOYOTA GAZOO RACING WRT'}, 'manufacturer': {'manufacturerId': 3, 'name': 'Toyota'}},"
                + "{'entryId': 1002, 'identifier': '11', 'group': {'name': 'Rally1'},"
                + " 'driver': " + persona(12, "Thierry", "NEUVILLE", "BEL") + ", 'codriver': " + persona(22, "Martijn", "WYDAEGHE", "BEL") + ","
                + " 'entrant': {'entrantId': 8, 'name': 'HYUNDAI SHELL MOBIS WRT'}, 'manufacturer': {'manufacturerId': 4, 'name': 'Hyundai'}},"
                // Rally2 sin puntos: no se guarda (solo la clase principal o quien sumó puntos)
                + "{'entryId': 1003, 'identifier': '20', 'group': {'name': 'Rally2'},"
                + " 'driver': " + persona(13, "Juan", "PEREZ", "ARG") + ", 'codriver': " + persona(23, "Luis", "GOMEZ", "ARG") + ","
                + " 'manufacturer': {'manufacturerId': 5, 'name': 'Skoda'}},"
                // Sin copiloto: no forma una tripulación válida
                + "{'entryId': 1004, 'identifier': '21', 'group': {'name': 'Rally1'},"
                + " 'driver': " + persona(14, "Solo", "PILOTO", "ESP") + ", 'manufacturer': {'manufacturerId': 6, 'name': 'Ford'}}]");
        wrc.responder("events/100/stages.json", """
                [{'stageId': 5002, 'number': 2, 'code': 'SS2', 'name': 'Saint-Clement', 'distance': 20.5, 'status': 'Completed',
                  'controls': [{'type': 'StageStart', 'firstCarDueDateTime': '2024-01-26T08:00:00'}]},
                 {'stageId': 5001, 'number': 1, 'code': 'SS1', 'name': 'La Breole', 'distance': 18.1, 'status': 'Completed',
                  'controls': [{'type': 'TimeControl'}, {'type': 'StageStart', 'firstCarDueDateTime': '2024-01-25T18:00:00'}]}]""");
        wrc.responder("events/100/stages/5001/stagetimes.json?rallyId=900", """
                [{'entryId': 1001, 'status': 'Completed', 'position': 1, 'elapsedDurationMs': 600000, 'diffFirstMs': 0},
                 {'entryId': 1002, 'status': 'Completed', 'position': 2, 'elapsedDurationMs': 603500, 'diffFirstMs': 3500},
                 {'entryId': 1003, 'status': 'Completed', 'position': 3, 'elapsedDurationMs': 650000, 'diffFirstMs': 50000}]""");
        wrc.responder("events/100/stages/5002/stagetimes.json?rallyId=900", """
                [{'entryId': 1001, 'status': 'Completed', 'position': 1, 'elapsedDurationMs': 610000, 'diffFirstMs': 0},
                 {'entryId': 1002, 'status': 'Retired'}]""");
        wrc.responder("events/100/stages/5002/results.json?rallyId=900", """
                [{'entryId': 1001, 'position': 1, 'totalTimeMs': 1210000, 'diffFirstMs': 0}]""");
        wrc.responder("events/100/retirements.json", "[{'entryId': 1002, 'reason': 'Accident'}]");
        // Suecia (eventId 200) no tiene respuestas cargadas: el proveedor responde 404
    }

    @Test
    void unRallyQueNoResponde_seOmiteConAvisoYElRestoSeGuarda() {
        temporada2024();

        TemporadaNorm temporada = adaptador.obtenerTemporada(2024, avisos);

        assertThat(temporada.siglasCategoria()).isEqualTo("WRC");
        assertThat(temporada.eventos()).extracting(EventoNorm::nombre).containsExactly("Rallye Monte-Carlo");
        assertThat(avisos).anyMatch(aviso -> aviso.contains("Rally Sweden"));
    }

    @Test
    void elEventoSeNormalizaConSuZonaHorariaYNombreCorto() {
        temporada2024();

        EventoNorm montecarlo = adaptador.obtenerTemporada(2024, avisos).eventos().get(0);

        assertThat(montecarlo.idExterno()).isEqualTo("100");
        assertThat(montecarlo.nombreCorto()).isEqualTo("Monte-Carlo");
        assertThat(montecarlo.numeroFecha()).isEqualTo(1);
        assertThat(montecarlo.ciudad()).isEqualTo("Gap");
        assertThat(montecarlo.zonaHoraria()).isEqualTo("Europe/Monaco");
        // Empieza a las 00:00 del 25/01 en Mónaco (UTC+1) = 23:00 UTC del día anterior
        assertThat(montecarlo.inicio()).isEqualTo(Instant.parse("2024-01-24T23:00:00Z"));
        assertThat(montecarlo.estado()).isEqualTo(Catalogo.EstadoEvento.FINALIZADO);
    }

    @Test
    void losTramosQuedanOrdenadosConSuDiaYSoloLasTripulacionesValidas() {
        temporada2024();

        List<EtapaNorm> etapas = adaptador.obtenerTemporada(2024, avisos).eventos().get(0).etapas();

        assertThat(etapas).extracting(EtapaNorm::nombre)
                .containsExactly("SS1 La Breole", "SS2 Saint-Clement", "Clasificación general");
        assertThat(etapas.get(0).dia()).isEqualTo(1);
        assertThat(etapas.get(1).dia()).isEqualTo(2);
        assertThat(etapas.get(0).inicio()).isEqualTo(Instant.parse("2024-01-25T18:00:00Z"));

        List<ResultadoEtapaNorm> tramo1 = etapas.get(0).resultados();
        // El de Rally2 sin puntos y el que no tiene copiloto no se guardan
        assertThat(tramo1).extracting(r -> r.piloto().apellido()).containsExactly("Ogier", "Neuville");
        assertThat(tramo1.get(0).tiempo()).isEqualByComparingTo("600.000");
        assertThat(tramo1.get(1).diferencia()).isEqualByComparingTo("3.500");
        ResultadoEtapaNorm ogier = tramo1.get(0);
        assertThat(ogier.piloto().idExterno()).isEqualTo("WRC:sebastien-ogier");
        assertThat(ogier.copiloto().apellido()).isEqualTo("Landais");
        assertThat(ogier.equipo().idExterno()).isEqualTo("WRC:E7");
        assertThat(ogier.equipo().nombre()).isEqualTo("Toyota Gazoo Racing WRT");
    }

    @Test
    void laClasificacionGeneralTraeLosPuntosYElAbandonoConSuMotivo() {
        temporada2024();

        EtapaNorm general = adaptador.obtenerTemporada(2024, avisos).eventos().get(0).etapas().get(2);

        assertThat(general.idExterno()).isEqualTo(Catalogo.ETAPA_GENERAL);
        ResultadoEtapaNorm ogier = general.resultados().stream()
                .filter(r -> r.piloto().apellido().equals("Ogier")).findFirst().orElseThrow();
        assertThat(ogier.posicion()).isEqualTo(1);
        assertThat(ogier.tiempo()).isEqualByComparingTo("1210.000");
        assertThat(ogier.puntos()).isEqualByComparingTo("30");
        ResultadoEtapaNorm neuville = general.resultados().stream()
                .filter(r -> r.piloto().apellido().equals("Neuville")).findFirst().orElseThrow();
        assertThat(neuville.estado()).isEqualTo(EstadoResultado.ABANDONO);
        assertThat(neuville.motivoAbandono()).isEqualTo("Accident");
        // Abandonó, pero los puntos del campeonato (ej: Power Stage) igual se registran
        assertThat(neuville.puntos()).isEqualByComparingTo("18");
    }

    @Test
    void anioSinTemporadaEnElProveedor_lanzaRespuestaVacia() {
        temporada2024();

        assertThatThrownBy(() -> adaptador.obtenerTemporada(2019, avisos))
                .isInstanceOf(IntegrationExternalException.class)
                .extracting(ex -> ((IntegrationExternalException) ex).getTipo())
                .isEqualTo(IntegrationExternalException.Tipo.RESPUESTA_VACIA);
    }

    @Test
    void normalizacionDeNombres() {
        // El id del piloto se arma con el nombre: la API cambia el personId entre temporadas
        assertThat(WrcAdapter.idPersona("Sébastien", "OGIER")).isEqualTo("WRC:sebastien-ogier");
        assertThat(WrcAdapter.idPersona("Kalle", "ROVANPERÄ")).isEqualTo("WRC:kalle-rovanpera");
        assertThat(WrcAdapter.nombreCorto("Rally Italia Sardegna")).isEqualTo("Italia Sardegna");
        assertThat(WrcAdapter.nombreEquipo("M-SPORT FORD WRT")).isEqualTo("M-Sport Ford WRT");
        assertThat(WrcAdapter.capitalizar("NEUVILLE")).isEqualTo("Neuville");
        assertThat(WrcAdapter.estadoTramo("Cancelled")).isEqualTo(Catalogo.EstadoEvento.CANCELADO);
        assertThat(WrcAdapter.estadoTramo(null)).isEqualTo(Catalogo.EstadoEvento.PROGRAMADO);
    }
}
