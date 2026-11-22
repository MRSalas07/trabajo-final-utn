package com.tfi.motorsport.backend.clients.normalization;

import com.tfi.motorsport.backend.dtos.integracionAPI.ResumenSincronizacion;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.EventoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.TemporadaNorm;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
/**
 * NORMALIZACIÓN Y ALMACENAMIENTO.
 *
 * Recibe una temporada en el modelo canónico (de cualquier fuente) y:
 *  1. VALIDA cada evento, sesión, etapa y resultado (Bean Validation + reglas propias).
 *     Lo inválido se descarta con un aviso; no se aborta toda la temporada por un dato malo.
 *  2. NORMALIZA valores (zonas horarias válidas, fechas coherentes, textos recortados).
 *  3. Hace UPSERT por id_externo: si el registro existe se actualiza, si no se crea.
 *     Así la sincronización se puede repetir sin duplicar datos (operación idempotente).
 *
 * Cada evento se guarda en su propia transacción: si falla uno, los demás se conservan.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NormalizadorService {

    private final TransactionTemplate tx;
    private final CategoriaJPARepository categoriaRepo;
    private final ValidacionNormalizada validacion;
    private final CalendarioPersistidor calendario;

    public ResumenSincronizacion persistir(TemporadaNorm temporada, List<String> avisosPrevios) {
        Instant inicio = Instant.now();
        List<String> avisos = new ArrayList<>(avisosPrevios);

        Categoria categoria = categoriaRepo.findBySiglasIgnoreCase(temporada.siglasCategoria())
                .orElseThrow(() -> new RuleBusinessException(
                        "La categoría '" + temporada.siglasCategoria() + "' no existe. Cárguela antes de importar datos."));
        ContextoNormalizacion ctx = new ContextoNormalizacion(categoria.getId(), temporada.anio(), avisos);

        Long temporadaId = tx.execute(s -> calendario.upsertTemporada(categoria.getId(), temporada).getId());

        for (EventoNorm evento : temporada.eventos()) {
            if (!validacion.eventoValido(evento, ctx)) {
                ctx.omitidos++;
                continue;
            }
            try {
                tx.executeWithoutResult(s -> calendario.persistirEvento(temporadaId, evento, ctx));
                ctx.eventos++;
            } catch (RuntimeException ex) {
                ctx.omitidos++;
                ctx.avisos.add("Evento '" + evento.nombre() + "' no guardado: " + ex.getMessage());
                log.error("Error guardando evento {}", evento.idExterno(), ex);
            }
        }
        tx.executeWithoutResult(s -> calendario.asignarCampeon(temporadaId, temporada.idExternoCampeon(), ctx));
        Instant fin = Instant.now();
        return new ResumenSincronizacion(categoria.getSiglas(), temporada.anio(), temporada.fuente(),
                ctx.eventos, ctx.omitidos, ctx.sesiones, ctx.etapas, ctx.resultados,
                ctx.pilotosNuevos, ctx.equiposNuevos, ctx.avisos, inicio, fin,
                fin.toEpochMilli() - inicio.toEpochMilli());
    }

}

