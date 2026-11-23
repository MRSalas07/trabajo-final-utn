package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.categoria.CategoriaCreateDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaUpdateDto;
import com.tfi.motorsport.backend.dtos.gestionDatos.CategoriaDtos;
import com.tfi.motorsport.backend.dtos.gestionDatos.PalmaresDtos;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.Palmares;
import com.tfi.motorsport.backend.model.Usuario;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.PalmaresJPARepository;
import com.tfi.motorsport.backend.repositories.UsuarioJPARepository;
import com.tfi.motorsport.backend.services.ICategoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.tfi.motorsport.backend.services.gestion.Impl.GestionDePrueba.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Categorías desde el panel (las reglas del alta y la modificación siguen en ICategoriaService: acá se
 * prueba que además queda registrado quién las hizo) y el listado del palmarés.
 */
@ExtendWith(MockitoExtension.class)
class CategoriaYPalmaresGestionServiceImplTest {

    @Mock
    private CategoriaJPARepository categoriaRepo;

    @Mock
    private ICategoriaService categoriaService;

    @Mock
    private PalmaresJPARepository palmaresRepo;

    @Mock
    private UsuarioJPARepository usuarioRepo;

    private final Usuario admin = admin();
    private CategoriaGestionServiceImpl categorias;
    private PalmaresGestionServiceImpl palmares;

    @BeforeEach
    void setUp() {
        lenient().when(usuarioRepo.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        SoporteGestion soporte = new SoporteGestion(usuarioRepo);
        categorias = new CategoriaGestionServiceImpl(categoriaRepo, categoriaService, soporte);
        palmares = new PalmaresGestionServiceImpl(palmaresRepo, categoriaRepo, soporte);
    }

    // ---------------------------------------------------------------- categorías

    @Test
    void crear_usaLasReglasDeCategoriaServiceYRegistraQuienLaCreo() {
        CategoriaCreateDto dto = new CategoriaCreateDto("Turismo Carretera", "TC", null, "Argentina", null);
        CategoriaDto creada = new CategoriaDto(3L, "Turismo Carretera", "TC", null, "Argentina", null, true);
        Categoria entidad = categoria(3L, "TC");
        when(categoriaService.crearCategoria(dto)).thenReturn(creada);
        when(categoriaRepo.findById(3L)).thenReturn(Optional.of(entidad));

        assertThat(categorias.crear(dto, ADMIN_ID)).isEqualTo(creada);
        assertThat(entidad.getCreadoPor()).isSameAs(admin);
        assertThat(entidad.getActualizadoPor()).isSameAs(admin);
    }

    @Test
    void crear_siLasReglasLaRechazan_noRegistraAuditoria() {
        CategoriaCreateDto dto = new CategoriaCreateDto("Turismo Carretera", "TC", null, "Argentina", null);
        when(categoriaService.crearCategoria(dto))
                .thenThrow(new RuleBusinessException("Ya existe una categoría con las siglas TC", HttpStatus.CONFLICT));

        assertThatThrownBy(() -> categorias.crear(dto, ADMIN_ID))
                .isInstanceOfSatisfying(RuleBusinessException.class, conEstado(HttpStatus.CONFLICT));
        verifyNoInteractions(categoriaRepo);
    }

    @Test
    void modificar_registraQuienLaModificoSinTocarAlCreador() {
        CategoriaUpdateDto dto = new CategoriaUpdateDto("Turismo Carretera", "TC", "ACTC", "Argentina", null, true);
        CategoriaDto modificada = new CategoriaDto(3L, "Turismo Carretera", "TC", "ACTC", "Argentina", null, true);
        Categoria entidad = categoria(3L, "TC");
        when(categoriaService.modificarCategoria(3L, dto)).thenReturn(modificada);
        when(categoriaRepo.findById(3L)).thenReturn(Optional.of(entidad));

        assertThat(categorias.modificar(3L, dto, ADMIN_ID)).isEqualTo(modificada);
        assertThat(entidad.getActualizadoPor()).isSameAs(admin);
        assertThat(entidad.getCreadoPor()).isNull();
    }

    @Test
    void eliminar_laDesactivaYEsBajaLogica() {
        Categoria entidad = categoria(3L, "TC");
        when(categoriaRepo.findById(3L)).thenReturn(Optional.of(entidad));

        categorias.eliminar(3L, ADMIN_ID);

        assertThat(entidad.getActivo()).isFalse();
        assertThat(entidad.isEliminado()).isTrue();
        assertThat(entidad.getActualizadoPor()).isSameAs(admin);
    }

    @Test
    void listar_incluyeLaAuditoriaDeCadaCategoria() {
        Categoria f1 = categoria(1L, "F1");
        Categoria tc = categoria(3L, "TC");
        tc.setCreadoPor(admin);
        tc.setActualizadoPor(admin);
        when(categoriaRepo.findAllByEliminadoFalseOrderByIdAsc()).thenReturn(List.of(f1, tc));

        List<CategoriaDtos.Registro> listado = categorias.listar();

        assertThat(listado).extracting(CategoriaDtos.Registro::siglas).containsExactly("F1", "TC");
        assertThat(listado.get(0).auditoria().creadoPor()).isNull();
        assertThat(listado.get(1).auditoria().creadoPor()).isEqualTo(ADMIN_EMAIL);
        assertThat(listado.get(1).auditoria().actualizadoPor()).isEqualTo(ADMIN_EMAIL);
    }

    // ---------------------------------------------------------------- palmarés

    @Test
    void listarPalmares_devuelveLosDatosDeCadaPiloto() {
        Categoria tc = categoria(3L, "TC");
        Palmares p = new Palmares();
        p.setId(90L);
        p.setPiloto(piloto(1L, "Agustín", "Canapino"));
        p.setCategoria(tc);
        p.setCampeonatos(0);
        p.setVictorias(9);
        p.setPuntosTotales(new BigDecimal("1520.5"));
        when(categoriaRepo.findById(3L)).thenReturn(Optional.of(tc));
        when(palmaresRepo.deCategoria(3L)).thenReturn(List.of(p));

        List<PalmaresDtos.Registro> listado = palmares.listar(3L);

        assertThat(listado).hasSize(1);
        assertThat(listado.get(0).piloto()).isEqualTo("Agustín Canapino");
        assertThat(listado.get(0).categoria()).isEqualTo("TC");
        assertThat(listado.get(0).victorias()).isEqualTo(9);
        assertThat(listado.get(0).auditoria().creadoPor()).isNull();
    }

    @Test
    void listarPalmares_deUnaCategoriaDadaDeBaja_lanza404() {
        when(categoriaRepo.findById(3L)).thenReturn(Optional.of(dadoDeBaja(categoria(3L, "TC"))));

        assertThatThrownBy(() -> palmares.listar(3L)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(palmaresRepo);
    }
}
