package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.comun.PaginaDto;
import com.tfi.motorsport.backend.dtos.piloto.PalmaresDto;
import com.tfi.motorsport.backend.dtos.piloto.PerfilPilotoDto;
import com.tfi.motorsport.backend.dtos.piloto.PilotoResumenDto;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.Equipo;
import com.tfi.motorsport.backend.model.EquipoPiloto;
import com.tfi.motorsport.backend.model.Palmares;
import com.tfi.motorsport.backend.model.Piloto;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.EquipoPilotoJPARepository;
import com.tfi.motorsport.backend.repositories.PalmaresJPARepository;
import com.tfi.motorsport.backend.repositories.PilotoJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoEtapaJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoSesionJPARepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class PilotoServiceImplTest {

    @Mock private CategoriaJPARepository categoriaRepo;
    @Mock private PilotoJPARepository pilotoRepo;
    @Mock private EquipoPilotoJPARepository equipoPilotoRepo;
    @Mock private PalmaresJPARepository palmaresRepo;
    @Mock private ResultadoSesionJPARepository resultadoSesionRepo;
    @Mock private ResultadoEtapaJPARepository resultadoEtapaRepo;

    @InjectMocks
    private PilotoServiceImpl service;

    private static Piloto piloto(Long id, String nombre, String apellido, Integer numero) {
        Piloto p = new Piloto();
        p.setId(id);
        p.setNombre(nombre);
        p.setApellido(apellido);
        p.setNumeroActual(numero);
        return p;
    }

    private static Categoria categoria(Long id, String siglas) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setSiglas(siglas);
        return c;
    }

    private static EquipoPiloto participacion(Piloto piloto, Long equipoId, String equipo, Integer numero) {
        Equipo e = new Equipo();
        e.setId(equipoId);
        e.setNombre(equipo);
        EquipoPiloto ep = new EquipoPiloto();
        ep.setPiloto(piloto);
        ep.setEquipo(e);
        ep.setCategoria(categoria(1L, "F1"));
        ep.setTemporada(2025);
        ep.setNumero(numero);
        ep.setRol("PILOTO");
        return ep;
    }

    @Test
    void buscarPilotos_sinFiltros_noExigeParticipacionYUsaElEquipoMasReciente() {
        // Arrange
        Piloto tsunoda = piloto(1L, "Yuki", "Tsunoda", null);
        when(pilotoRepo.buscar(eq("%%"), eq(false), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tsunoda), PageRequest.of(0, 24), 1));
        // El repositorio devuelve las participaciones de la más reciente a la más antigua
        when(equipoPilotoRepo.participacionesDePilotos(List.of(1L), null, null)).thenReturn(List.of(
                participacion(tsunoda, 10L, "Red Bull Racing", 22),
                participacion(tsunoda, 11L, "Racing Bulls", 22)));

        // Act
        PaginaDto<PilotoResumenDto> pagina = service.buscarPilotos(null, null, null, 0, 24);

        // Assert
        assertThat(pagina.totalElementos()).isEqualTo(1);
        assertThat(pagina.tamanio()).isEqualTo(24);
        PilotoResumenDto resumen = pagina.contenido().getFirst();
        assertThat(resumen.equipoId()).isEqualTo(10L);
        assertThat(resumen.equipoActual()).isEqualTo("Red Bull Racing");
        assertThat(resumen.numero()).as("sin número propio usa el de la participación").isEqualTo(22);
    }

    @Test
    void buscarPilotos_normalizaElTextoYAplicaCategoriaAnioYPaginado() {
        // Arrange
        when(categoriaRepo.findBySiglasIgnoreCase("f1")).thenReturn(Optional.of(categoria(1L, "F1")));
        when(pilotoRepo.buscar(anyString(), anyBoolean(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 10), 0));
        ArgumentCaptor<String> patron = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        // Act
        service.buscarPilotos("  Hamilton ", " f1 ", 2025, 2, 10);

        // Assert
        verify(pilotoRepo).buscar(patron.capture(), eq(true), eq(1L), eq(2025), pageable.capture());
        assertThat(patron.getValue()).isEqualTo("%hamilton%");
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().getOrderFor("apellido")).isNotNull();
    }

    @Test
    void buscarPilotos_paginaVacia_noConsultaParticipaciones() {
        // Arrange
        when(pilotoRepo.buscar(anyString(), anyBoolean(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 24), 0));

        // Act
        PaginaDto<PilotoResumenDto> pagina = service.buscarPilotos("zzz", null, null, 0, 24);

        // Assert
        assertThat(pagina.contenido()).isEmpty();
        verifyNoInteractions(equipoPilotoRepo);
    }

    @Test
    void buscarPilotos_categoriaInexistente_lanza404SinBuscar() {
        // Arrange
        when(categoriaRepo.findBySiglasIgnoreCase("XX")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.buscarPilotos(null, "XX", null, 0, 24))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(pilotoRepo, never()).buscar(anyString(), anyBoolean(), any(), any(), any(Pageable.class));
    }

    @Test
    void perfil_pilotoInexistente_lanza404() {
        when(pilotoRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.perfil(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void perfil_pilotoDadoDeBaja_lanza404() {
        Piloto baja = piloto(5L, "Ana", "Baja", null);
        baja.setEliminado(true);
        when(pilotoRepo.findById(5L)).thenReturn(Optional.of(baja));

        assertThatThrownBy(() -> service.perfil(5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void perfil_completaElPalmaresFaltanteConCeroYCalculaLaEdad() {
        // Arrange
        Piloto canapino = piloto(7L, "Agustín", "Canapino", 1);
        LocalDate nacimiento = LocalDate.of(1990, 3, 30);
        canapino.setFechaNacimiento(nacimiento);
        when(pilotoRepo.findById(7L)).thenReturn(Optional.of(canapino));

        Palmares tc = new Palmares();
        tc.setCategoria(categoria(3L, "TC"));
        tc.setCarreras(15);
        tc.setVictorias(6);
        // podios, poles, abandonos y puntos sin cargar (null), como en los datos de TC
        tc.setCampeonatos(1);
        when(palmaresRepo.dePiloto(7L)).thenReturn(List.of(tc));
        when(equipoPilotoRepo.trayectoriaDePiloto(7L)).thenReturn(List.of(participacion(canapino, 20L, "Canning Motorsport", 1)));

        // Act
        PerfilPilotoDto perfil = service.perfil(7L);

        // Assert
        assertThat(perfil.piloto().edad()).isEqualTo(Period.between(nacimiento, LocalDate.now()).getYears());
        PalmaresDto palmares = perfil.palmares().getFirst();
        assertThat(palmares.categoria()).isEqualTo("TC");
        assertThat(palmares.victorias()).isEqualTo(6);
        assertThat(palmares.podios()).isZero();
        assertThat(palmares.puntosTotales()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(perfil.trayectoria()).singleElement()
                .satisfies(t -> {
                    assertThat(t.equipo()).isEqualTo("Canning Motorsport");
                    assertThat(t.piloto()).isEqualTo("Agustín Canapino");
                });
        assertThat(perfil.ultimosResultados()).isEmpty();
    }
}
