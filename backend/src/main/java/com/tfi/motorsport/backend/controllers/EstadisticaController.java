package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.*;
import com.tfi.motorsport.backend.services.IEstadisticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

@Tag(name = "Estadísticas")
@RestController
@RequestMapping("/api/estadisticas/{siglas}")
@RequiredArgsConstructor
public class EstadisticaController {

    private final IEstadisticaService  estadisticaService;

    @Operation(summary = "Resumen de la temporada: líder, ganadores por fecha, fechas disputadas")
    @GetMapping("/{anio}/resumen")
    public ResumenTemporada resumen(
            @PathVariable String siglas,
            @PathVariable @Min(1950) @Max(2100) int anio) {
        return estadisticaService.resumen(siglas, anio);
    }

    @Operation(summary = "Campeonato de pilotos calculado")
    @GetMapping("/{anio}/campeonato/pilotos")
    public List<FilaCampeonatoPiloto> pilotos(@PathVariable String siglas, @PathVariable @Min(1950) @Max(2100) int anio) {
        return estadisticaService.campeonatoPilotos(siglas, anio);
    }

    @Operation(summary = "Campeonato de equipos calculado (suma de puntos de sus pilotos)")
    @GetMapping("/{anio}/campeonato/equipos")
    public List<FilaCampeonatoEquipo> equipos(@PathVariable String siglas, @PathVariable @Min(1950) @Max(2100) int anio) {
        return estadisticaService.campeonatoEquipos(siglas, anio);
    }

    @Operation(summary = "Evolución del campeonato: puntos acumulados y posición de cada piloto fecha a fecha")
    @GetMapping("/{anio}/evolucion")
    public EvolucionCampeonato evolucion(@PathVariable String siglas, @PathVariable @Min(1950) @Max(2100) int anio,
                                         @RequestParam(required = false) List<Long> pilotos,
                                         @RequestParam(defaultValue = "5") @Min(1) @Max(20) int top) {
        return estadisticaService.evolucionPilotos(siglas, anio, pilotos, top);
    }

    @Operation(summary = "Tendencia de rendimiento de un piloto en la temporada")
    @GetMapping("/{anio}/tendencia/{pilotoId}")
    public TendenciaPiloto tendencia(@PathVariable String siglas, @PathVariable @Min(1950) @Max(2100) int anio,
                                     @PathVariable Long pilotoId) {
        return estadisticaService.tendenciaPiloto(siglas, anio, pilotoId);
    }

    @Operation(summary = "Comparativa entre 2 pilotos de la temporada")
    @GetMapping("/{anio}/comparativa")
    public ComparativaPilotos comparativa(@PathVariable String siglas,
                                          @PathVariable @Min(1950) @Max(2100) int anio,
                                          @RequestParam Long a,
                                          @RequestParam Long b) {
        return estadisticaService.compararPilotos(siglas, anio, a, b);
    }

    @Operation(summary = "Recalcular el palmarés histórico de una categoría")
    @PostMapping("/palmares/recalcular")
    public Map<String, Object> recalcular(@PathVariable String siglas) {
        return Map.of("categoria", siglas.toUpperCase(), "pilotosActualizados", estadisticaService.recalcularPalmares(siglas));
    }

    @Operation(summary = "Ranking de marcas de la temporada")
    @GetMapping("/{anio}/marcas")
    public List<FilaMarca> marcas(@PathVariable String siglas,
                                  @PathVariable @Min(1950) @Max(2100) int anio) {
        return estadisticaService.rankingMarcas(siglas, anio);
    }

    @Operation(summary = "Tendencia de rendimiento de un equipo (puntos por fecha, media móvil y regresión)")
    @GetMapping("/{anio}/tendencia-equipo/{equipoId}")
    public TendenciaGrupo tendenciaEquipo(@PathVariable String siglas, @PathVariable @Min(1950) @Max(2100) int anio,
                                          @PathVariable Long equipoId) {
        return estadisticaService.tendenciaEquipo(siglas, anio, equipoId);
    }

    @Operation(summary = "Tendencia de rendimiento de una marca (ej: Toyota, Ford)")
    @GetMapping("/{anio}/tendencia-marca")
    public TendenciaGrupo tendenciaMarca(@PathVariable String siglas, @PathVariable @Min(1950) @Max(2100) int anio,
                                         @RequestParam @Size(min = 1, max = 60) String marca) {
        return estadisticaService.tendenciaMarca(siglas, anio, marca);
    }

    @Operation(summary = "Comparativa histórica entre dos pilotos a lo largo de todas las temporadas")
    @GetMapping("/historico/comparativa")
    public ComparativaHistorica comparativaHistorica(@PathVariable String siglas,
                                                     @RequestParam("a") Long pilotoA, @RequestParam("b") Long pilotoB) {
        return estadisticaService.comparativaHistorica(siglas, pilotoA, pilotoB);
    }


}
