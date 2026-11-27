package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.Usuario;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.UsuarioJPARepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static com.tfi.motorsport.backend.services.gestion.Impl.GestionDePrueba.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Utilidades comunes de la gestión: búsqueda de registros activos, auditoría (alta, cambio y baja)
 * y validaciones que comparten todos los ABM.
 */
@ExtendWith(MockitoExtension.class)
class SoporteGestionTest {

    @Mock
    private UsuarioJPARepository usuarioRepo;

    @Mock
    private CategoriaJPARepository categoriaRepo;

    @InjectMocks
    private SoporteGestion soporte;

    // ---------------------------------------------------------------- activa()

    @Test
    void activa_registroExistente_loDevuelve() {
        Categoria f1 = categoria(1L, "F1");
        when(categoriaRepo.findById(1L)).thenReturn(Optional.of(f1));

        assertThat(soporte.activa(categoriaRepo, 1L, "la categoría")).isSameAs(f1);
    }

    @Test
    void activa_sinId_lanzaErrorDeNegocioSinConsultarLaBase() {
        assertThatThrownBy(() -> soporte.activa(categoriaRepo, null, "la categoría"))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessage("Falta indicar la categoría");
        verifyNoInteractions(categoriaRepo);
    }

    @Test
    void activa_inexistente_lanza404() {
        when(categoriaRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> soporte.activa(categoriaRepo, 99L, "la categoría"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No se encontró la categoría con ID 99");
    }

    @Test
    void activa_dadoDeBaja_lanza404ComoSiNoExistiera() {
        when(categoriaRepo.findById(1L)).thenReturn(Optional.of(dadoDeBaja(categoria(1L, "F1"))));

        assertThatThrownBy(() -> soporte.activa(categoriaRepo, 1L, "la categoría"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No se encontró la categoría con ID 1");
    }

    // ---------------------------------------------------------------- auditoría

    @Test
    void alta_registraAlUsuarioComoCreadorYComoUltimoEnModificar() {
        Usuario admin = admin();
        when(usuarioRepo.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        Categoria c = categoria(1L, "F1");

        soporte.alta(c, ADMIN_ID);

        assertThat(c.getCreadoPor()).isSameAs(admin);
        assertThat(c.getActualizadoPor()).isSameAs(admin);
    }

    @Test
    void alta_sinUsuario_quedaComoCargaDelSistema() {
        Categoria c = categoria(1L, "F1");

        soporte.alta(c, null);

        assertThat(c.getCreadoPor()).isNull();
        assertThat(c.getActualizadoPor()).isNull();
        verify(usuarioRepo).findById(-1L);
    }

    @Test
    void cambio_actualizaSoloAlUltimoEnModificar() {
        Usuario creador = usuario(3L, "carga@motorsport.local");
        Usuario admin = admin();
        when(usuarioRepo.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        Categoria c = categoria(1L, "F1");
        c.setCreadoPor(creador);

        soporte.cambio(c, ADMIN_ID);

        assertThat(c.getCreadoPor()).isSameAs(creador);
        assertThat(c.getActualizadoPor()).isSameAs(admin);
    }

    @Test
    void baja_esLogicaYRegistraQuienLaHizo() {
        Usuario admin = admin();
        when(usuarioRepo.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        Categoria c = categoria(1L, "F1");

        soporte.baja(c, ADMIN_ID);

        assertThat(c.isEliminado()).isTrue();
        assertThat(c.getActualizadoPor()).isSameAs(admin);
    }

    // ---------------------------------------------------------------- validaciones

    @Test
    void validarFechas_finAnteriorAlInicio_lanzaError() {
        LocalDate inicio = LocalDate.of(2025, 3, 10);

        assertThatThrownBy(() -> SoporteGestion.validarFechas(inicio, inicio.minusDays(1)))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessage("La fecha de fin no puede ser anterior a la de inicio");
    }

    @Test
    void validarFechas_mismoDiaOFechasIncompletas_sonValidas() {
        LocalDate dia = LocalDate.of(2025, 3, 10);

        assertThatCode(() -> {
            SoporteGestion.validarFechas(dia, dia);
            SoporteGestion.validarFechas(null, dia);
            SoporteGestion.validarFechas(dia, null);
        }).doesNotThrowAnyException();
    }

    @Test
    void validarInstantes_finAnteriorAlInicio_lanzaError() {
        Instant inicio = Instant.parse("2025-03-16T04:00:00Z");

        assertThatThrownBy(() -> SoporteGestion.validarInstantes(inicio, inicio.minusSeconds(1)))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessage("La fecha y hora de fin no puede ser anterior a la de inicio");
    }

    @Test
    void texto_vacioOEnBlancoEsNullYElRestoSeRecorta() {
        assertThat(SoporteGestion.texto(null)).isNull();
        assertThat(SoporteGestion.texto("   ")).isNull();
        assertThat(SoporteGestion.texto("  Ford ")).isEqualTo("Ford");
    }

    @Test
    void patron_armaUnLikeEnMinusculas() {
        assertThat(SoporteGestion.patron("  Verstappen ")).isEqualTo("%verstappen%");
        assertThat(SoporteGestion.patron(null)).isEqualTo("%%");
    }
}
