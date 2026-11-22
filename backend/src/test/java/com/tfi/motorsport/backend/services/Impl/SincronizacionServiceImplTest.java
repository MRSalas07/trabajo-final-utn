package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.clients.adapter.FuenteDatosCategoria;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.TemporadaNorm;
import com.tfi.motorsport.backend.clients.normalization.NormalizadorService;
import com.tfi.motorsport.backend.dtos.integracionAPI.CargaSincronizacionDto;
import com.tfi.motorsport.backend.dtos.integracionAPI.ResumenSincronizacion;
import com.tfi.motorsport.backend.handlers.exceptions.IntegrationExternalException;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.services.IEstadisticaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SincronizacionServiceImplTest {

    private static final int ANIO = 2025;

    private final FuenteDatosCategoria fuente = mock(FuenteDatosCategoria.class);
    private final NormalizadorService normalizador = mock(NormalizadorService.class);
    private final IEstadisticaService estadisticaService = mock(IEstadisticaService.class);
    private final CategoriaJPARepository categoriaRepo = mock(CategoriaJPARepository.class);

    private SincronizacionServiceImpl service;

    @BeforeEach
    void configurar() {
        when(fuente.siglasCategoria()).thenReturn("F1");
        when(fuente.proveedor()).thenReturn("Jolpica");
        when(fuente.habilitada()).thenReturn(true);
        when(categoriaRepo.existsBySiglasIgnoreCase(any())).thenReturn(true);
        service = new SincronizacionServiceImpl(List.of(fuente), normalizador, estadisticaService, categoriaRepo, null);
    }

    @AfterEach
    void detener() {
        service.detener();
    }

    private static TemporadaNorm temporada() {
        return new TemporadaNorm("F1", ANIO, "F1 2025", "Jolpica", List.of());
    }

    private static ResumenSincronizacion resumen(int eventos) {
        Instant ahora = Instant.now();
        return new ResumenSincronizacion("F1", ANIO, "Jolpica", eventos, 0, 0, 0, 0, 0, 0, List.of(), ahora, ahora, 0);
    }

    private CargaSincronizacionDto esperarFin(String trabajoId) throws InterruptedException {
        long limite = System.currentTimeMillis() + 5000;
        CargaSincronizacionDto trabajo = service.estado(trabajoId);
        while (System.currentTimeMillis() < limite
                && !CargaSincronizacionDto.COMPLETADO.equals(trabajo.estado())
                && !CargaSincronizacionDto.FALLIDO.equals(trabajo.estado())) {
            Thread.sleep(20);
            trabajo = service.estado(trabajoId);
        }
        return trabajo;
    }

    @Test
    void iniciar_categoriaInexistente_lanza404() {
        when(categoriaRepo.existsBySiglasIgnoreCase("XX")).thenReturn(false);

        assertThatThrownBy(() -> service.iniciar("XX", ANIO)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void iniciar_categoriaSinApiIntegrada_lanzaReglaDeNegocio() {
        assertThatThrownBy(() -> service.iniciar("TC", ANIO))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessageContaining("importación JSON");
    }

    @Test
    void iniciar_fuenteDeshabilitada_lanzaErrorDeIntegracion() {
        when(fuente.habilitada()).thenReturn(false);

        assertThatThrownBy(() -> service.iniciar("F1", ANIO))
                .isInstanceOf(IntegrationExternalException.class)
                .extracting(ex -> ((IntegrationExternalException) ex).getTipo())
                .isEqualTo(IntegrationExternalException.Tipo.NO_DISPONIBLE);
    }

    @Test
    void iniciar_temporadaPosteriorAlAnioSiguiente_lanzaReglaDeNegocio() {
        int anioInvalido = Year.now().getValue() + 2;

        assertThatThrownBy(() -> service.iniciar("F1", anioInvalido)).isInstanceOf(RuleBusinessException.class);
    }

    @Test
    void sincronizacionExitosa_quedaCompletadaYRecalculaElPalmares() throws Exception {
        when(fuente.obtenerTemporada(eq(ANIO), anyList())).thenReturn(temporada());
        when(normalizador.persistir(any(), anyList())).thenReturn(resumen(3));

        CargaSincronizacionDto creado = service.iniciar("f1", ANIO);
        CargaSincronizacionDto terminado = esperarFin(creado.id());

        assertThat(creado.estado()).isEqualTo(CargaSincronizacionDto.EN_COLA);
        assertThat(creado.categoria()).isEqualTo("F1");
        assertThat(terminado.estado()).isEqualTo(CargaSincronizacionDto.COMPLETADO);
        assertThat(terminado.resumen().eventosProcesados()).isEqualTo(3);
        assertThat(terminado.finalizado()).isNotNull();
        verify(estadisticaService).recalcularPalmares("F1");
    }

    @Test
    void errorDelProveedor_dejaElTrabajoFallidoSinGuardarNada() throws Exception {
        when(fuente.obtenerTemporada(anyInt(), anyList())).thenThrow(new IntegrationExternalException(
                "Jolpica", IntegrationExternalException.Tipo.LIMITE_USO, "Se superó el límite de consultas"));

        CargaSincronizacionDto terminado = esperarFin(service.iniciar("F1", ANIO).id());

        assertThat(terminado.estado()).isEqualTo(CargaSincronizacionDto.FALLIDO);
        assertThat(terminado.error()).contains("límite de consultas");
        verify(normalizador, never()).persistir(any(), anyList());
        verify(estadisticaService, never()).recalcularPalmares(any());
    }

    @Test
    void errorInesperado_dejaElTrabajoFallidoConElMotivo() throws Exception {
        when(fuente.obtenerTemporada(anyInt(), anyList())).thenReturn(temporada());
        when(normalizador.persistir(any(), anyList())).thenThrow(new IllegalStateException("fallo de base"));

        CargaSincronizacionDto terminado = esperarFin(service.iniciar("F1", ANIO).id());

        assertThat(terminado.estado()).isEqualTo(CargaSincronizacionDto.FALLIDO);
        assertThat(terminado.error()).startsWith("Error inesperado").contains("fallo de base");
    }

    @Test
    void mismaCategoriaYAnioEnCurso_seRechazaConConflicto() throws Exception {
        CountDownLatch liberar = new CountDownLatch(1);
        when(fuente.obtenerTemporada(anyInt(), anyList())).thenAnswer(inv -> {
            liberar.await(5, TimeUnit.SECONDS);
            return temporada();
        });
        when(normalizador.persistir(any(), anyList())).thenReturn(resumen(1));

        CargaSincronizacionDto primero = service.iniciar("F1", ANIO);

        assertThatThrownBy(() -> service.iniciar("F1", ANIO))
                .isInstanceOf(RuleBusinessException.class)
                .extracting(ex -> ((RuleBusinessException) ex).getEstado())
                .isEqualTo(HttpStatus.CONFLICT);
        CargaSincronizacionDto otroAnio = service.iniciar("F1", ANIO - 1);

        liberar.countDown();
        assertThat(esperarFin(primero.id()).estado()).isEqualTo(CargaSincronizacionDto.COMPLETADO);
        assertThat(esperarFin(otroAnio.id()).estado()).isEqualTo(CargaSincronizacionDto.COMPLETADO);
    }

    @Test
    void trabajosEnCola_seEjecutanDeAUnoPorVez() throws Exception {
        CountDownLatch liberar = new CountDownLatch(1);
        when(fuente.obtenerTemporada(anyInt(), anyList())).thenAnswer(inv -> {
            liberar.await(5, TimeUnit.SECONDS);
            return temporada();
        });
        when(normalizador.persistir(any(), anyList())).thenReturn(resumen(1));

        CargaSincronizacionDto primero = service.iniciar("F1", ANIO);
        CargaSincronizacionDto segundo = service.iniciar("F1", ANIO - 1);
        Thread.sleep(100);

        assertThat(service.estado(primero.id()).estado()).isEqualTo(CargaSincronizacionDto.EN_PROCESO);
        assertThat(service.estado(segundo.id()).estado()).isEqualTo(CargaSincronizacionDto.EN_COLA);

        liberar.countDown();
        assertThat(esperarFin(segundo.id()).estado()).isEqualTo(CargaSincronizacionDto.COMPLETADO);
    }

    @Test
    void estado_trabajoInexistente_lanza404() {
        assertThatThrownBy(() -> service.estado("no-existe")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void importar_normalizaLasSiglasUsaLaFuentePorDefectoYRecalculaElPalmares() {
        TemporadaNorm json = new TemporadaNorm("tc", ANIO, "TC 2025", null, List.of());
        when(normalizador.persistir(any(), anyList())).thenReturn(resumen(0));

        service.importar(json);

        verify(normalizador).persistir(org.mockito.ArgumentMatchers.argThat(t ->
                t.siglasCategoria().equals("TC") && t.fuente().equals("IMPORTACION_JSON")), eq(List.of()));
        verify(estadisticaService).recalcularPalmares("TC");
    }
}
