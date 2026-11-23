package com.tfi.motorsport.backend.clients.normalization;

import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.EquipoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.EventoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.PilotoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.ResultadoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.SesionNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.TemporadaNorm;
import com.tfi.motorsport.backend.dtos.integracionAPI.ResumenSincronizacion;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.ResultadoSesion;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.EquipoJPARepository;
import com.tfi.motorsport.backend.repositories.EventoJPARepository;
import com.tfi.motorsport.backend.repositories.PilotoJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoSesionJPARepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class NormalizadorServiceTest {

    private static final String SIGLAS = "NRM";

    @Autowired private NormalizadorService normalizador;
    @Autowired private CategoriaJPARepository categoriaRepo;
    @Autowired private EventoJPARepository eventoRepo;
    @Autowired private PilotoJPARepository pilotoRepo;
    @Autowired private EquipoJPARepository equipoRepo;
    @Autowired private ResultadoSesionJPARepository resultadoSesionRepo;
    @Autowired private TransactionTemplate tx;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void prepararBase() {
        jdbc.queryForList("""
                select table_name, column_name from information_schema.columns
                where column_name in ('fechaCreacion', 'fechaActualizacion') and table_schema = 'PUBLIC'
                """).forEach(columna -> jdbc.execute("alter table " + columna.get("TABLE_NAME")
                + " alter column \"" + columna.get("COLUMN_NAME") + "\" set default current_timestamp"));
        if (!categoriaRepo.existsBySiglasIgnoreCase(SIGLAS)) {
            Categoria categoria = new Categoria();
            categoria.setNombre("Categoría de prueba");
            categoria.setSiglas(SIGLAS);
            categoria.setActivo(true);
            categoriaRepo.save(categoria);
        }
    }

    private static ResultadoNorm resultado(String piloto, String apellido, int posicion, String puntos) {
        return new ResultadoNorm(new PilotoNorm(piloto, "Piloto", apellido, null, null, null, null, null),
                new EquipoNorm("eq-" + piloto, "Equipo " + apellido, null, null),
                posicion, null, new BigDecimal(puntos), null, null, null, null, "FINALIZADO", null, null);
    }

    private static EventoNorm evento(String idExterno, String nombre, String puntosGanador) {
        SesionNorm carrera = new SesionNorm(idExterno + "-carrera", "Carrera", "CARRERA", 1,
                Instant.parse("2025-03-16T04:00:00Z"), Instant.parse("2025-03-16T06:00:00Z"), null, null, "FINALIZADO",
                List.of(resultado(idExterno + "-p1", "Uno", 1, puntosGanador), resultado(idExterno + "-p2", "Dos", 2, "18")));
        return new EventoNorm(idExterno, nombre, null, 1, "Australia", "Melbourne", "Albert Park", "Australia/Melbourne",
                Instant.parse("2025-03-14T01:00:00Z"), Instant.parse("2025-03-16T06:00:00Z"), "FINALIZADO",
                List.of(carrera), List.of());
    }

    private static TemporadaNorm temporada(String anio, String puntosGanador) {
        EventoNorm valido = evento("nrm-" + anio + "-1", "Gran Premio de Prueba", puntosGanador);
        EventoNorm sinNombre = evento("nrm-" + anio + "-2", "", puntosGanador);
        return new TemporadaNorm(SIGLAS, Integer.valueOf(anio), null, "PRUEBA", List.of(valido, sinNombre));
    }

    private BigDecimal puntosDelGanador(String idExternoSesion) {
        return tx.execute(estado -> resultadoSesionRepo.findAll().stream()
                .filter(r -> r.getPosicion() != null && r.getPosicion() == 1)
                .filter(r -> idExternoSesion.equals(r.getSesion().getIdExterno()))
                .map(ResultadoSesion::getPuntos)
                .findFirst().orElseThrow());
    }

    @Test
    void repetirLaMismaSincronizacion_noDuplicaDatos() {
        long eventosAntes = eventoRepo.count();
        long pilotosAntes = pilotoRepo.count();
        long equiposAntes = equipoRepo.count();
        long resultadosAntes = resultadoSesionRepo.count();

        ResumenSincronizacion primera = normalizador.persistir(temporada("2030", "25"), List.of());
        ResumenSincronizacion segunda = normalizador.persistir(temporada("2030", "25"), List.of());

        assertThat(primera.eventosProcesados()).isEqualTo(1);
        assertThat(primera.pilotosNuevos()).isEqualTo(2);
        assertThat(primera.equiposNuevos()).isEqualTo(2);
        assertThat(segunda.eventosProcesados()).isEqualTo(1);
        assertThat(segunda.pilotosNuevos()).isZero();
        assertThat(segunda.equiposNuevos()).isZero();
        assertThat(eventoRepo.count()).isEqualTo(eventosAntes + 1);
        assertThat(pilotoRepo.count()).isEqualTo(pilotosAntes + 2);
        assertThat(equipoRepo.count()).isEqualTo(equiposAntes + 2);
        assertThat(resultadoSesionRepo.count()).isEqualTo(resultadosAntes + 2);
    }

    @Test
    void sincronizarConDatosCorregidos_actualizaSinCrearRegistrosNuevos() {
        normalizador.persistir(temporada("2031", "25"), List.of());
        long resultados = resultadoSesionRepo.count();

        normalizador.persistir(temporada("2031", "26"), List.of());

        assertThat(resultadoSesionRepo.count()).isEqualTo(resultados);
        assertThat(puntosDelGanador("nrm-2031-1-carrera")).isEqualByComparingTo("26");
    }

    @Test
    void eventoInvalido_seOmiteConAvisoYElRestoSeGuarda() {
        ResumenSincronizacion resumen = normalizador.persistir(temporada("2032", "25"), List.of("aviso del adaptador"));

        assertThat(resumen.eventosProcesados()).isEqualTo(1);
        assertThat(resumen.eventosOmitidos()).isEqualTo(1);
        assertThat(resumen.resultadosProcesados()).isEqualTo(2);
        assertThat(resumen.avisos()).contains("aviso del adaptador").hasSizeGreaterThan(1);
    }

    @Test
    void categoriaInexistente_lanzaReglaDeNegocio() {
        TemporadaNorm desconocida = new TemporadaNorm("ZZZ", 2025, null, "PRUEBA", List.of());

        assertThatThrownBy(() -> normalizador.persistir(desconocida, List.of()))
                .isInstanceOf(RuleBusinessException.class);
    }
}
