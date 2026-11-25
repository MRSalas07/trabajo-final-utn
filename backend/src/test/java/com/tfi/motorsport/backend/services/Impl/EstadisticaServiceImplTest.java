package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.ComparativaHistorica;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.ComparativaPilotos;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.EvolucionCampeonato;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.EvolucionGrupos;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.FilaCampeonatoEquipo;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.FilaCampeonatoPiloto;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.FilaMarca;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.Historico;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.ResumenTemporada;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.TendenciaGrupo;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.TendenciaPiloto;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.Evento;
import com.tfi.motorsport.backend.model.Palmares;
import com.tfi.motorsport.backend.model.Piloto;
import com.tfi.motorsport.backend.model.Temporada;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.EquipoJPARepository;
import com.tfi.motorsport.backend.repositories.EquipoPilotoJPARepository;
import com.tfi.motorsport.backend.repositories.EventoJPARepository;
import com.tfi.motorsport.backend.repositories.PalmaresJPARepository;
import com.tfi.motorsport.backend.repositories.PilotoJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoEtapaJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoSesionJPARepository;
import com.tfi.motorsport.backend.repositories.TemporadaJPARepository;
import com.tfi.motorsport.backend.repositories.unificacion.FilaResultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class EstadisticaServiceImplTest {

    private static final long ALONSO = 1L;
    private static final long HAMILTON = 2L;
    private static final long COLAPINTO = 3L;
    private static final long EQUIPO_ROJO = 10L;
    private static final long EQUIPO_AZUL = 20L;
    private static final long CATEGORIA_ID = 5L;
    private static final long TEMPORADA_ID = 50L;

    @Mock
    private PilotoJPARepository pilotoRepo;
    @Mock
    private TemporadaJPARepository temporadaRepo;
    @Mock
    private EventoJPARepository eventoRepo;
    @Mock
    private ResultadoSesionJPARepository resultadoSesionRepo;
    @Mock
    private CategoriaJPARepository categoriaRepo;
    @Mock
    private PalmaresJPARepository palmaresRepo;
    @Mock
    private EquipoJPARepository equipoRepo;

    @InjectMocks
    private EstadisticaServiceImpl service;

    private Categoria categoria;
    private Temporada temporada;

    @BeforeEach
    void prepararCategoriaYTemporada() {
        categoria = new Categoria();
        categoria.setId(CATEGORIA_ID);
        categoria.setSiglas("F1");
        categoria.setNombre("Fórmula 1");

        temporada = new Temporada();
        temporada.setId(TEMPORADA_ID);
        temporada.setCategoria(categoria);
        temporada.setAnio(2025);
    }


    private static FilaResultado fila(long eventoId, int numeroFecha, long pilotoId, int posicion, int puntos) {
        long equipoId = pilotoId == HAMILTON ? EQUIPO_AZUL : EQUIPO_ROJO;
        String apellido = pilotoId == ALONSO ? "Alonso" : pilotoId == HAMILTON ? "Hamilton" : "Colapinto";
        return new FilaResultado(eventoId, numeroFecha, "GP fecha " + numeroFecha,
                Instant.parse("2025-0" + numeroFecha + "-10T15:00:00Z"), TEMPORADA_ID, 2025, "F1",
                pilotoId, "Piloto", apellido, apellido.substring(0, 3).toUpperCase(),
                equipoId, equipoId == EQUIPO_ROJO ? "Rojo" : "Azul", equipoId == EQUIPO_ROJO ? "Ford" : "Toyota",
                "CARRERA", posicion, BigDecimal.valueOf(puntos), "FINALIZADO");
    }

    private static List<FilaResultado> temporadaDeDosFechas() {
        List<FilaResultado> filas = new ArrayList<>();
        filas.add(fila(1, 1, ALONSO, 1, 25));
        filas.add(fila(1, 1, HAMILTON, 2, 18));
        filas.add(fila(1, 1, COLAPINTO, 3, 15));
        filas.add(fila(2, 2, HAMILTON, 1, 25));
        filas.add(fila(2, 2, ALONSO, 2, 18));
        filas.add(fila(2, 2, COLAPINTO, 3, 15));
        return filas;
    }

    private static Evento evento(String estado) {
        Evento evento = new Evento();
        evento.setEstado(estado);
        evento.setFechaInicio(Instant.parse("2025-03-10T15:00:00Z"));
        evento.setFechaFin(Instant.parse("2025-03-10T17:00:00Z"));
        return evento;
    }

    private void prepararTemporadaConResultados() {
        when(temporadaRepo.buscarPorSiglasYAnio("F1", 2025)).thenReturn(Optional.of(temporada));
        when(resultadoSesionRepo.filasDeTemporada(TEMPORADA_ID)).thenReturn(temporadaDeDosFechas());
    }

    private void prepararCategoriaConResultados() {
        when(categoriaRepo.findBySiglasIgnoreCase("F1")).thenReturn(Optional.of(categoria));
        when(resultadoSesionRepo.filasDeCategoria(CATEGORIA_ID)).thenReturn(temporadaDeDosFechas());
    }

    @Test
    void campeonatoPilotos_ordenaLaTablaPorPuntos() {
        // Arrange
        prepararTemporadaConResultados();

        // Act
        List<FilaCampeonatoPiloto> tabla = service.campeonatoPilotos("F1", 2025);

        // Assert
        assertThat(tabla).hasSize(3);
        assertThat(tabla.get(2).pilotoId()).isEqualTo(COLAPINTO);
        assertThat(tabla.get(2).puntos()).isEqualByComparingTo("30");
        assertThat(tabla.get(0).puntos()).isEqualByComparingTo("43");
        assertThat(tabla.get(0).posicion()).isEqualTo(1);
    }

    @Test
    void campeonatoPilotos_conTemporadaInexistente_lanzaRecursoNoEncontrado() {
        // Arrange
        when(temporadaRepo.buscarPorSiglasYAnio("F1", 1900)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.campeonatoPilotos("F1", 1900))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("1900")
                .hasMessageContaining("F1");
    }

    @Test
    void campeonatoEquipos_sumaLosPuntosDeSusPilotos() {
        // Arrange
        prepararTemporadaConResultados();

        // Act
        List<FilaCampeonatoEquipo> tabla = service.campeonatoEquipos("F1", 2025);

        // Assert
        assertThat(tabla).hasSize(2);
        assertThat(tabla.get(0).equipoId()).isEqualTo(EQUIPO_ROJO);
        assertThat(tabla.get(0).puntos()).isEqualByComparingTo("73");
        assertThat(tabla.get(1).equipoId()).isEqualTo(EQUIPO_AZUL);
        assertThat(tabla.get(1).puntos()).isEqualByComparingTo("43");
    }

    @Test
    void resumen_incluyeLiderEquipoLiderYGanadoresDistintos() {
        // Arrange
        prepararTemporadaConResultados();
        when(eventoRepo.listarPorTemporada(TEMPORADA_ID)).thenReturn(List.of(evento("FINALIZADO"), evento("FINALIZADO"), evento("PROGRAMADO")));

        // Act
        ResumenTemporada resumen = service.resumen("F1", 2025);

        // Assert
        assertThat(resumen.categoria()).isEqualTo("F1");
        assertThat(resumen.anio()).isEqualTo(2025);
        assertThat(resumen.eventosTotales()).isEqualTo(3);
        assertThat(resumen.eventosDisputados()).isEqualTo(2);
        assertThat(resumen.lider().puntos()).isEqualByComparingTo("43");
        assertThat(resumen.equipoLider().equipoId()).isEqualTo(EQUIPO_ROJO);
        assertThat(resumen.ganadoresDistintos()).isEqualTo(2);
        assertThat(resumen.ganadores()).hasSize(2);
    }

    @Test
    void resumen_sinResultados_devuelveLideresNulos() {
        // Arrange
        when(temporadaRepo.buscarPorSiglasYAnio("F1", 2025)).thenReturn(Optional.of(temporada));
        when(eventoRepo.listarPorTemporada(TEMPORADA_ID)).thenReturn(List.of(evento("PROGRAMADO")));

        // Act
        ResumenTemporada resumen = service.resumen("F1", 2025);

        // Assert
        assertThat(resumen.lider()).isNull();
        assertThat(resumen.equipoLider()).isNull();
        assertThat(resumen.eventosDisputados()).isZero();
        assertThat(resumen.ganadoresDistintos()).isZero();
        assertThat(resumen.eventosTotales()).isEqualTo(1);
    }


    @Test
    void evolucionPilotos_conMasDeVeintePilotos_lanzaErrorDeNegocio() {
        // Arrange
        List<Long> veintiuno = new ArrayList<>();
        for (long id = 1; id <= 21; id++) {
            veintiuno.add(id);
        }

        // Act + Assert
        assertThatThrownBy(() -> service.evolucionPilotos("F1", 2025, veintiuno, 5))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessageContaining("20");
        verify(temporadaRepo, never()).buscarPorSiglasYAnio(any(), any());
    }

    @Test
    void evolucionPilotos_devuelveUnaRondaPorFecha() {
        // Arrange
        prepararTemporadaConResultados();

        // Act
        EvolucionCampeonato evolucion = service.evolucionPilotos("F1", 2025, null, 100);

        // Assert
        assertThat(evolucion.rondas()).hasSize(2);
        assertThat(evolucion.series()).hasSize(3);
    }


    @Test
    void tendenciaPiloto_conPilotoInexistente_lanzaRecursoNoEncontrado() {
        // Arrange
        when(pilotoRepo.existsById(99L)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> service.tendenciaPiloto("F1", 2025, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(temporadaRepo, never()).buscarPorSiglasYAnio(any(), any());
    }

    @Test
    void tendenciaPiloto_devuelveUnPuntoPorFechaDisputada() {
        // Arrange
        when(pilotoRepo.existsById(ALONSO)).thenReturn(true);
        prepararTemporadaConResultados();

        // Act
        TendenciaPiloto tendencia = service.tendenciaPiloto("F1", 2025, ALONSO);

        // Assert
        assertThat(tendencia.pilotoId()).isEqualTo(ALONSO);
        assertThat(tendencia.puntos()).hasSize(2);
        assertThat(tendencia.mejorResultado()).isEqualTo(1);
        assertThat(tendencia.peorResultado()).isEqualTo(2);
    }

    @Test
    void compararPilotos_conElMismoPiloto_lanzaErrorDeNegocio() {
        // Act + Assert
        assertThatThrownBy(() -> service.compararPilotos("F1", 2025, ALONSO, ALONSO))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessageContaining("distintos");
        verify(pilotoRepo, never()).existsById(any());
    }

    @Test
    void compararPilotos_conPilotosValidos_devuelveLaComparativa() {
        // Arrange
        when(pilotoRepo.existsById(ALONSO)).thenReturn(true);
        when(pilotoRepo.existsById(HAMILTON)).thenReturn(true);
        prepararTemporadaConResultados();

        // Act
        ComparativaPilotos comparativa = service.compararPilotos("F1", 2025, ALONSO, HAMILTON);

        // Assert
        assertThat(comparativa).isNotNull();
        assertThat(comparativa.a().pilotoId()).isEqualTo(ALONSO);
        assertThat(comparativa.b().pilotoId()).isEqualTo(HAMILTON);
    }

    @Test
    void recalcularPalmares_creaElPalmaresConLosContadoresDeCadaPiloto() {
        // Arrange
        prepararCategoriaConResultados();
        when(eventoRepo.listarPorTemporada(TEMPORADA_ID)).thenReturn(List.of(evento("FINALIZADO"), evento("CANCELADO")));
        when(temporadaRepo.findById(TEMPORADA_ID)).thenReturn(Optional.of(temporada));

        // Act
        int pilotosActualizados = service.recalcularPalmares("F1");

        // Assert
        assertThat(pilotosActualizados).isEqualTo(3);
        ArgumentCaptor<Palmares> guardados = ArgumentCaptor.forClass(Palmares.class);
        verify(palmaresRepo, times(3)).save(guardados.capture());
        Palmares deColapinto = guardados.getAllValues().stream()
                .filter(p -> p.getCarreras() == 2 && p.getVictorias() == 0)
                .findFirst().orElseThrow();
        assertThat(deColapinto.getPodios()).isEqualTo(2);
        assertThat(deColapinto.getPuntosTotales()).isEqualByComparingTo("30");
        assertThat(deColapinto.getCampeonatos()).isZero();
        assertThat(deColapinto.getCategoria()).isSameAs(categoria);
    }

    @Test
    void recalcularPalmares_cuentaElCampeonatoOficialSoloSiLaTemporadaTermino() {
        // Arrange
        Piloto campeon = new Piloto();
        campeon.setId(HAMILTON);
        temporada.setCampeon(campeon);
        prepararCategoriaConResultados();
        when(eventoRepo.listarPorTemporada(TEMPORADA_ID)).thenReturn(List.of(evento("FINALIZADO"), evento("FINALIZADO")));
        when(temporadaRepo.findById(TEMPORADA_ID)).thenReturn(Optional.of(temporada));
        Palmares existente = new Palmares();
        when(palmaresRepo.findByPilotoIdAndCategoriaId(HAMILTON, CATEGORIA_ID)).thenReturn(Optional.of(existente));
        when(palmaresRepo.findByPilotoIdAndCategoriaId(ALONSO, CATEGORIA_ID)).thenReturn(Optional.empty());
        when(palmaresRepo.findByPilotoIdAndCategoriaId(COLAPINTO, CATEGORIA_ID)).thenReturn(Optional.empty());

        // Act
        service.recalcularPalmares("F1");

        // Assert
        assertThat(existente.getCampeonatos()).isEqualTo(1);
        assertThat(existente.getVictorias()).isEqualTo(1);
        verify(palmaresRepo).save(existente);
    }

    @Test
    void recalcularPalmares_conTemporadaSinTerminar_noCuentaCampeonatos() {
        // Arrange
        prepararCategoriaConResultados();
        Evento futuro = evento("PROGRAMADO");
        futuro.setFechaInicio(Instant.parse("2999-01-01T00:00:00Z"));
        futuro.setFechaFin(Instant.parse("2999-01-02T00:00:00Z"));
        when(eventoRepo.listarPorTemporada(TEMPORADA_ID)).thenReturn(List.of(evento("FINALIZADO"), futuro));

        // Act
        service.recalcularPalmares("F1");

        // Assert
        ArgumentCaptor<Palmares> guardados = ArgumentCaptor.forClass(Palmares.class);
        verify(palmaresRepo, times(3)).save(guardados.capture());
        assertThat(guardados.getAllValues()).allSatisfy(p -> assertThat(p.getCampeonatos()).isZero());
        verify(temporadaRepo, never()).findById(any());
    }

    @Test
    void recalcularPalmares_conCategoriaEliminada_lanzaRecursoNoEncontrado() {
        // Arrange
        categoria.setEliminado(true);
        when(categoriaRepo.findBySiglasIgnoreCase("F1")).thenReturn(Optional.of(categoria));

        // Act + Assert
        assertThatThrownBy(() -> service.recalcularPalmares("F1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("F1");
        verify(palmaresRepo, never()).save(any());
    }


    @Test
    void rankingMarcas_agrupaLosPuntosPorMarca() {
        // Arrange
        prepararTemporadaConResultados();

        // Act
        List<FilaMarca> ranking = service.rankingMarcas("F1", 2025);

        // Assert
        assertThat(ranking).hasSize(2);
        assertThat(ranking.get(0).marca()).isEqualTo("Ford");
        assertThat(ranking.get(0).puntos()).isEqualByComparingTo("73");
    }

    @Test
    void tendenciaEquipo_conEquipoInexistente_lanzaRecursoNoEncontrado() {
        // Arrange
        when(equipoRepo.existsById(99L)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> service.tendenciaEquipo("F1", 2025, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void tendenciaEquipo_conEquipoSinResultadosEnLaTemporada_lanzaRecursoNoEncontrado() {
        // Arrange
        when(equipoRepo.existsById(777L)).thenReturn(true);
        prepararTemporadaConResultados();

        // Act + Assert
        assertThatThrownBy(() -> service.tendenciaEquipo("F1", 2025, 777L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("F1 2025");
    }

    @Test
    void tendenciaEquipo_conResultados_devuelveUnPuntoPorFecha() {
        // Arrange
        when(equipoRepo.existsById(EQUIPO_ROJO)).thenReturn(true);
        prepararTemporadaConResultados();

        // Act
        TendenciaGrupo tendencia = service.tendenciaEquipo("F1", 2025, EQUIPO_ROJO);

        // Assert
        assertThat(tendencia.fechas()).hasSize(2);
    }

    @Test
    void tendenciaMarca_sinMarca_lanzaErrorDeNegocio() {
        // Act + Assert
        assertThatThrownBy(() -> service.tendenciaMarca("F1", 2025, "  "))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessageContaining("marca");
        verify(temporadaRepo, never()).buscarPorSiglasYAnio(any(), any());
    }

    @Test
    void tendenciaMarca_conMarcaSinResultados_lanzaRecursoNoEncontrado() {
        // Arrange
        prepararTemporadaConResultados();

        // Act + Assert
        assertThatThrownBy(() -> service.tendenciaMarca("F1", 2025, "Ferrari"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ferrari");
    }

    @Test
    void tendenciaMarca_conResultados_devuelveLaTendenciaDeLaMarca() {
        // Arrange
        prepararTemporadaConResultados();

        // Act
        TendenciaGrupo tendencia = service.tendenciaMarca("F1", 2025, "Ford");

        // Assert
        assertThat(tendencia.fechas()).hasSize(2);
    }

    @Test
    void evolucionGrupos_conTipoInvalido_lanzaErrorDeNegocio() {
        // Act + Assert
        assertThatThrownBy(() -> service.evolucionGrupos("F1", 2025, "PILOTO", 5))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessageContaining("EQUIPO o MARCA");
    }

    @Test
    void evolucionGrupos_sinTipo_usaEquipoPorDefecto() {
        // Arrange
        prepararTemporadaConResultados();

        // Act
        EvolucionGrupos evolucion = service.evolucionGrupos("F1", 2025, null, 5);

        // Assert
        assertThat(evolucion.tipo()).isEqualTo("EQUIPO");
        assertThat(evolucion.series()).hasSize(2);
    }

    @Test
    void evolucionGrupos_conTipoMarcaEnMinusculas_lo_normaliza() {
        // Arrange
        prepararTemporadaConResultados();

        // Act
        EvolucionGrupos evolucion = service.evolucionGrupos("F1", 2025, " marca ", 5);

        // Assert
        assertThat(evolucion.tipo()).isEqualTo("MARCA");
    }

    @Test
    void historicoPiloto_conResultados_devuelveUnaTemporadaPorAnio() {
        // Arrange
        when(pilotoRepo.existsById(ALONSO)).thenReturn(true);
        prepararCategoriaConResultados();

        // Act
        Historico historico = service.historicoPiloto("F1", ALONSO);

        // Assert
        assertThat(historico.categoria()).isEqualTo("F1");
        assertThat(historico.temporadas()).hasSize(1);
        assertThat(historico.victoriasTotales()).isEqualTo(1);
        assertThat(historico.puntosTotales()).isEqualByComparingTo("43");
    }

    @Test
    void historicoPiloto_sinResultadosEnLaCategoria_lanzaRecursoNoEncontrado() {
        // Arrange
        when(pilotoRepo.existsById(777L)).thenReturn(true);
        prepararCategoriaConResultados();

        // Act + Assert
        assertThatThrownBy(() -> service.historicoPiloto("F1", 777L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("F1");
    }

    @Test
    void historicoEquipo_conResultados_devuelveSuHistorico() {
        // Arrange
        when(equipoRepo.existsById(EQUIPO_ROJO)).thenReturn(true);
        prepararCategoriaConResultados();

        // Act
        Historico historico = service.historicoEquipo("F1", EQUIPO_ROJO);

        // Assert
        assertThat(historico.puntosTotales()).isEqualByComparingTo("73");
    }

    @Test
    void historicoEquipo_sinResultados_lanzaRecursoNoEncontrado() {
        // Arrange
        when(equipoRepo.existsById(777L)).thenReturn(true);
        prepararCategoriaConResultados();

        // Act + Assert
        assertThatThrownBy(() -> service.historicoEquipo("F1", 777L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void comparativaHistorica_conElMismoPiloto_lanzaErrorDeNegocio() {
        // Act + Assert
        assertThatThrownBy(() -> service.comparativaHistorica("F1", HAMILTON, HAMILTON))
                .isInstanceOf(RuleBusinessException.class);
        verify(categoriaRepo, never()).findBySiglasIgnoreCase(any());
    }

    @Test
    void comparativaHistorica_conPilotosValidos_devuelveLaComparativa() {
        // Arrange
        when(pilotoRepo.existsById(ALONSO)).thenReturn(true);
        when(pilotoRepo.existsById(HAMILTON)).thenReturn(true);
        prepararCategoriaConResultados();

        // Act
        ComparativaHistorica comparativa = service.comparativaHistorica("F1", ALONSO, HAMILTON);

        // Assert
        assertThat(comparativa.categoria()).isEqualTo("F1");
        assertThat(comparativa.anios()).containsExactly(2025);
        assertThat(comparativa.a().id()).isEqualTo(ALONSO);
        assertThat(comparativa.b().id()).isEqualTo(HAMILTON);
    }
}
