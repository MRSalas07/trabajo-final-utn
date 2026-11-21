package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.clients.adapter.FuenteDatosCategoria;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.*;
import com.tfi.motorsport.backend.clients.normalization.NormalizadorService;
import com.tfi.motorsport.backend.config.AppProperties;
import com.tfi.motorsport.backend.dtos.integracionAPI.CargaSincronizacionDto;
import com.tfi.motorsport.backend.dtos.integracionAPI.ResumenSincronizacion;
import com.tfi.motorsport.backend.handlers.exceptions.IntegrationExternalException;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.services.IEstadisticaService;
import com.tfi.motorsport.backend.services.ISincronizacionService;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.digester.Rule;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Year;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orquesta la integración de datos:
 *   API externa --(adaptador)--> modelo normalizado --(NormalizadorService)--> PostgreSQL
 *   y al final recalcula el palmarés de la categoría.
 *
 * Las sincronizaciones se ejecutan en SEGUNDO PLANO en una cola de un solo hilo:
 *  - el usuario no espera minutos con la petición abierta (respuesta HTTP 202 inmediata);
 *  - nunca corren dos sincronizaciones a la vez, así se respetan los límites de los proveedores.
 * El historial de trabajos se guarda en memoria (se pierde al reiniciar; ver "próximos pasos").
 */

@Slf4j
@Service
public class SincronizacionServiceImpl implements ISincronizacionService {

    private static final int HISTORIAL_MAXIMO = 50;

    private final Map<String, FuenteDatosCategoria> fuentes;
    private final NormalizadorService normalizador;
    private final IEstadisticaService estadisticaService;
    private final CategoriaJPARepository categoriaRepo;
    private final AppProperties props;
    private final ExecutorService cola = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sincronizacion");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, CargaSincronizacionDto> trabajos = new ConcurrentHashMap<>();

    public SincronizacionServiceImpl(List<FuenteDatosCategoria> fuentes, NormalizadorService normalizador,
                                     IEstadisticaService estadisticaService, CategoriaJPARepository categoriaRepo,
                                     AppProperties props) {
        // Spring inyecta TODOS los adaptadores; se indexan por siglas de categoría
        this.fuentes = fuentes.stream().collect(Collectors.toMap(
                f -> f.siglasCategoria().toUpperCase(Locale.ROOT), Function.identity()));
        this.normalizador = normalizador;
        this.estadisticaService = estadisticaService;
        this.categoriaRepo = categoriaRepo;
        this.props = props;
    }


    @Override
    public CargaSincronizacionDto iniciar(String siglas, int anio) {
        String clave = siglas.trim().toUpperCase(Locale.ROOT);
        if (!categoriaRepo.existsBySiglasIgnoreCase(clave)) {
            throw new ResourceNotFoundException("No existe la categoría '" + siglas + "'");
        }
        FuenteDatosCategoria fuente = fuentes.get(clave);
        if (fuente == null) {
            throw new RuleBusinessException("La categoría " + clave + " no tiene una API pública integrada. "
                    + "Cargue sus datos con la importación JSON (POST /api/admin/importaciones).");
        }
        if (!fuente.habilitada()) {
            throw new IntegrationExternalException(fuente.proveedor(), IntegrationExternalException.Tipo.NO_DISPONIBLE,
                    "La integración de " + clave + " está deshabilitada por configuración");
        }
        int anioMaximo = Year.now().getValue() + 1;
        if (anio > anioMaximo) {
            throw new RuleBusinessException("No se puede sincronizar una temporada posterior a " + anioMaximo);
        }
        boolean yaEnCurso = trabajos.values().stream().anyMatch(t -> t.categoria().equals(clave) && t.anio() == anio
                && (CargaSincronizacionDto.EN_COLA.equals(t.estado()) || CargaSincronizacionDto.EN_PROCESO.equals(t.estado())));
        if (yaEnCurso) {
            throw new RuleBusinessException("Ya hay una sincronización de " + clave + " " + anio + " en curso",
                    HttpStatus.CONFLICT);
        }

        String id = UUID.randomUUID().toString();
        CargaSincronizacionDto trabajo = new CargaSincronizacionDto(id, clave, anio,
                CargaSincronizacionDto.EN_COLA, Instant.now(), null, null, null);
        trabajos.put(id, trabajo);
        limpiarHistorial();
        cola.submit(() -> ejecutar(trabajo, fuente));
        return trabajo;
    }

    /** Lo que corre en segundo plano. Nunca lanza excepciones: el error queda en el trabajo. */
    private void ejecutar(CargaSincronizacionDto t, FuenteDatosCategoria fuente) {
        actualizar(t, CargaSincronizacionDto.EN_PROCESO, null, null);
        log.info("Sincronizando {} {} desde {}", t.categoria(), t.anio(), fuente.proveedor());
        try {
            List<String> avisos = new ArrayList<>();
            TemporadaNorm datos = fuente.obtenerTemporada(t.anio(), avisos);
            ResumenSincronizacion resumen = normalizador.persistir(datos, avisos);
            estadisticaService.recalcularPalmares(t.categoria());
            actualizar(t, CargaSincronizacionDto.COMPLETADO, resumen, null);
            log.info("Sincronización {} {} completada: {} eventos, {} resultados, {} avisos",
                    t.categoria(), t.anio(), resumen.eventosProcesados(), resumen.resultadosProcesados(),
                    resumen.avisos().size());
        } catch (IntegrationExternalException ex) {
            actualizar(t, CargaSincronizacionDto.FALLIDO, null, ex.getMessage());
            log.warn("Sincronización {} {} fallida: {}", t.categoria(), t.anio(), ex.getMessage());
        } catch (RuntimeException ex) {
            actualizar(t, CargaSincronizacionDto.FALLIDO, null, "Error inesperado: " + ex.getMessage());
            log.error("Sincronización {} {} con error inesperado", t.categoria(), t.anio(), ex);
        }
    }

    @Override
    public CargaSincronizacionDto estado(String trabajoId) {
        CargaSincronizacionDto t = trabajos.get(trabajoId);
        if (t == null) {
            throw new ResourceNotFoundException("No existe el trabajo de sincronización " + trabajoId);
        }
        return t;
    }

    @Override
    public List<CargaSincronizacionDto> trabajos() {
        return trabajos.values().stream()
                .sorted(Comparator.comparing(CargaSincronizacionDto::creado).reversed())
                .toList();
    }

    @Override
    public ResumenSincronizacion importar(TemporadaNorm temporada) {
        String fuente = temporada.fuente() != null ? temporada.fuente() : "IMPORTACION_JSON";
        TemporadaNorm conFuente = new TemporadaNorm(temporada.siglasCategoria().toUpperCase(Locale.ROOT),
                temporada.anio(), temporada.nombre(), fuente, temporada.eventos(), temporada.idExternoCampeon());

        ResumenSincronizacion resumen = normalizador.persistir(conFuente, List.of());
        estadisticaService.recalcularPalmares(conFuente.siglasCategoria());
        return resumen;
    }

    @Override
    public List<FuenteDisponible> fuentes() {
        return fuentes.values().stream()
                .map(f -> new FuenteDisponible(f.siglasCategoria(), f.proveedor(), f.habilitada()))
                .sorted(Comparator.comparing(FuenteDisponible::categoria))
                .toList();
    }

    /**
     * Sincronización automática diaria de la temporada en curso (si SYNC_AUTOMATICA=true).
     * El horario se configura con SYNC_CRON (por defecto, 6:00 todos los días).
     */
    @Scheduled(cron = "${app.integracion.sync-cron}")
    public void sincronizacionProgramada() {
        if (!props.integracion().syncAutomatica()) {
            return;
        }
        int anio = Year.now().getValue();
        for (FuenteDatosCategoria f : fuentes.values()) {
            if (!f.habilitada()) {
                continue;
            }
            try {
                iniciar(f.siglasCategoria(), anio);
            } catch (RuntimeException ex) {
                log.warn("Sincronización automática de {} no iniciada: {}", f.siglasCategoria(), ex.getMessage());
            }
        }
    }

    private void actualizar(CargaSincronizacionDto t, String estado, ResumenSincronizacion resumen, String error) {
        boolean terminado = CargaSincronizacionDto.COMPLETADO.equals(estado) || CargaSincronizacionDto.FALLIDO.equals(estado);
        trabajos.put(t.id(), new CargaSincronizacionDto(t.id(), t.categoria(), t.anio(), estado, t.creado(),
                terminado ? Instant.now() : null, resumen, error));
    }

    /** Conserva solo los últimos trabajos para no acumular memoria. */
    private void limpiarHistorial() {
        if (trabajos.size() <= HISTORIAL_MAXIMO) {
            return;
        }
        trabajos.values().stream()
                .sorted(Comparator.comparing(CargaSincronizacionDto::creado))
                .limit(trabajos.size() - HISTORIAL_MAXIMO)
                .map(CargaSincronizacionDto::id)
                .toList()
                .forEach(trabajos::remove);
    }

    @PreDestroy
    void detener() {
        cola.shutdownNow();
    }
}



