package com.tfi.motorsport.backend.controllers;

import com.tfi.motorsport.backend.dtos.calendario.ArchivoIcs;
import com.tfi.motorsport.backend.dtos.evento.EventoDto;
import com.tfi.motorsport.backend.services.ICalendarioService;
import com.tfi.motorsport.backend.services.IEventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Calendario unificado, portada (próximos y recientes) y descargas .ics.
 * Los parámetros son los que envían calendario.service.ts y evento.service.ts del frontend.
 */
@Tag(name = "Calendario")
@RestController
@RequiredArgsConstructor
public class CalendarioController {

    private static final MediaType TEXT_CALENDAR = new MediaType("text", "calendar", StandardCharsets.UTF_8);

    private final IEventoService eventoService;
    private final ICalendarioService calendarioService;
    private final ZonaHorariaSolicitada zonaHoraria;

    @Operation(summary = "Eventos entre dos días (yyyy-MM-dd, en la zona del usuario); categorías separadas por coma")
    @GetMapping("/api/calendario")
    public List<EventoDto> calendario(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) List<String> categorias,
            @RequestParam(required = false) String zona) {
        return eventoService.calendario(desde, hasta, categorias, zonaHoraria.resolverZonaHoraria(zona));
    }

    @Operation(summary = "Próximos eventos (incluye los que están en curso)")
    @GetMapping("/api/calendario/proximos")
    public List<EventoDto> proximos(@RequestParam(defaultValue = "6") @Min(1) @Max(20) int limite,
                                    @RequestParam(required = false) String zona) {
        return eventoService.proximos(limite, zonaHoraria.resolverZonaHoraria(zona));
    }

    @Operation(summary = "Últimos eventos finalizados, del más reciente al más antiguo")
    @GetMapping("/api/calendario/recientes")
    public List<EventoDto> recientes(@RequestParam(defaultValue = "6") @Min(1) @Max(20) int limite,
                                     @RequestParam(required = false) String zona) {
        return eventoService.recientes(limite, zonaHoraria.resolverZonaHoraria(zona));
    }

    @Operation(summary = "Descarga el .ics de un evento para agregarlo al calendario del celular")
    @GetMapping("/api/calendario/eventos/{id}.ics")
    public ResponseEntity<String> icsEvento(@PathVariable Long id) {
        return descarga(calendarioService.icsDeEvento(id));
    }

    @Operation(summary = "Descarga el .ics de una temporada completa")
    @GetMapping("/api/calendario/{siglas}/{anio}.ics")
    public ResponseEntity<String> icsTemporada(@PathVariable String siglas,
                                               @PathVariable @Min(1950) @Max(2100) int anio) {
        return descarga(calendarioService.icsDeTemporada(siglas, anio));
    }

    private static ResponseEntity<String> descarga(ArchivoIcs archivo) {
        return ResponseEntity.ok()
                .contentType(TEXT_CALENDAR)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombreArchivo()).build().toString())
                .body(archivo.contenido());
    }
}
