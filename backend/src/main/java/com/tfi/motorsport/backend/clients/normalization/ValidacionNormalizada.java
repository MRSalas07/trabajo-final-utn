package com.tfi.motorsport.backend.clients.normalization;

import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.EtapaNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.EventoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.SesionNorm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validación de los datos normalizados con Bean Validation (@NotBlank, @Min, @Pattern...).
 *
 * Regla clave: lo inválido se DESCARTA con un aviso, no se aborta toda la importación.
 *
 * Los eventos, sesiones y etapas se validan SIN sus hijos: ModeloNormalizado tiene @Valid en
 * las listas, y validar el objeto completo haría que un solo resultado malo descarte el evento
 * entero. Los hijos se validan después, uno por uno, para descartar solo el que esté mal.
 */
@Component
@RequiredArgsConstructor
class ValidacionNormalizada {

    private final Validator validator;

    boolean eventoValido(EventoNorm e, ContextoNormalizacion ctx) {
        EventoNorm soloEvento = new EventoNorm(e.idExterno(), e.nombre(), e.nombreCorto(), e.numeroFecha(), e.pais(),
                e.ciudad(), e.circuito(), e.zonaHoraria(), e.inicio(), e.fin(), e.estado(), List.of(), List.of());
        return sinErrores(soloEvento, "Evento '" + e.nombre() + "'", ctx);
    }

    boolean sesionValida(SesionNorm s, String nombreEvento, ContextoNormalizacion ctx) {
        SesionNorm soloSesion = new SesionNorm(s.idExterno(), s.nombre(), s.tipo(), s.numero(), s.inicio(), s.fin(),
                s.vueltasProgramadas(), s.distancia(), s.estado(), List.of());
        return sinErrores(soloSesion, "Sesión '" + s.nombre() + "' de " + nombreEvento, ctx);
    }

    boolean etapaValida(EtapaNorm et, String nombreEvento, ContextoNormalizacion ctx) {
        EtapaNorm soloEtapa = new EtapaNorm(et.idExterno(), et.numero(), et.nombre(), et.dia(), et.distancia(),
                et.inicio(), et.fin(), et.estado(), List.of());
        return sinErrores(soloEtapa, "Etapa '" + et.nombre() + "' de " + nombreEvento, ctx);
    }

    /** Valida un resultado de sesión (ResultadoNorm) o de etapa (ResultadoEtapaNorm). */
    boolean resultadoValido(Object resultado, String nombreEvento, String nombreSesionOEtapa, ContextoNormalizacion ctx) {
        return sinErrores(resultado, "Resultado de " + nombreEvento + " / " + nombreSesionOEtapa, ctx);
    }

    private boolean sinErrores(Object objeto, String descripcion, ContextoNormalizacion ctx) {
        Set<ConstraintViolation<Object>> errores = validator.validate(objeto);
        if (errores.isEmpty()) {
            return true;
        }
        String detalle = errores.stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .sorted().collect(Collectors.joining("; "));
        ctx.avisos.add(descripcion + " descartado por datos inválidos: " + detalle);
        return false;
    }
}
