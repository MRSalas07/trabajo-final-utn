package com.tfi.motorsport.backend.dtos.comun;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Respuesta paginada genérica (interfaz Pagina<T> del frontend).
 * "pagina" empieza en 0.
 */
public record PaginaDto<T>(
        List<T> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas
) {
    public static <T> PaginaDto<T> de(Page<T> page) {
        return new PaginaDto<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
