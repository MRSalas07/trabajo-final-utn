package com.tfi.motorsport.backend.services.gestion;

import com.tfi.motorsport.backend.dtos.gestionDatos.PalmaresDtos;

import java.util.List;

/**
 * Palmarés para el panel de administración. Es un dato DERIVADO: solo se consulta;
 * se actualiza recalculándolo (POST /api/palmares/{siglas}/recalcular), no editándolo a mano.
 */
public interface IPalmaresGestionService {

    List<PalmaresDtos.Registro> listar(Long categoriaId);
}
