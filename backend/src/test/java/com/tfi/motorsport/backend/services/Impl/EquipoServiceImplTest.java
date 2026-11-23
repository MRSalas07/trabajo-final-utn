package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.equipo.EquipoResumenDto;
import com.tfi.motorsport.backend.dtos.equipo.EstadisticasEquipoDto;
import com.tfi.motorsport.backend.dtos.equipo.PerfilEquipoDto;
import com.tfi.motorsport.backend.dtos.perfil.ResultadoRecienteDto;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.Equipo;
import com.tfi.motorsport.backend.model.EquipoPiloto;
import com.tfi.motorsport.backend.model.Piloto;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.EquipoJPARepository;
import com.tfi.motorsport.backend.repositories.EquipoPilotoJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoEtapaJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoSesionJPARepository;
import com.tfi.motorsport.backend.repositories.unificacion.FilaResultado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipoServiceImplTest {

    @Mock private CategoriaJPARepository categoriaRepo;
    @Mock private EquipoJPARepository equipoRepo;
    @Mock private EquipoPilotoJPARepository equipoPilotoRepo;
    @Mock private ResultadoSesionJPARepository resultadoSesionRepo;
    @Mock private ResultadoEtapaJPARepository resultadoEtapaRepo;

    @InjectMocks
    private EquipoServiceImpl service;

    private static Categoria categoria(Long id, String siglas) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setSiglas(siglas);
        return c;
    }

    private static Equipo equipo(Long id, String nombre) {
        Equipo e = new Equipo();
        e.setId(id);
        e.setNombre(nombre);
        e.setNombreCorto(nombre.split(" ")[0]);
        return e;
    }

    private static FilaResultado fila(long eventoId, String tipo, Integer posicion, String puntos) {
        return new FilaResultado(eventoId, 1, "Evento " + eventoId, Instant.parse("2025-03-16T04:00:00Z").plusSeconds(eventoId * 86_400),
                1L, 2025, "F1", 1L, "Lando", "Norris", "NOR", 10L, "McLaren",null,tipo, posicion,
                puntos != null ? new BigDecimal(puntos) : null, "FINALIZADO");
    }

    @Test
    void equiposDeCategoria_devuelveLosEquiposDeEsaCategoria() {
        when(categoriaRepo.findBySiglasIgnoreCase("F1")).thenReturn(Optional.of(categoria(1L, "F1")));
        when(equipoRepo.deCategoria(1L, 2025)).thenReturn(List.of(equipo(10L, "McLaren F1 Team")));

        List<EquipoResumenDto> equipos = service.equiposDeCategoria("F1", 2025);

        assertThat(equipos).extracting(EquipoResumenDto::nombre).containsExactly("McLaren F1 Team");
    }

    @Test
    void equiposDeCategoria_categoriaInexistente_lanza404() {
        when(categoriaRepo.findBySiglasIgnoreCase("XX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.equiposDeCategoria("XX", null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void perfil_equipoInexistente_lanza404() {
        when(equipoRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.perfil(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void perfil_armaCategoriasPlantelYEstadisticas() {
        // Arrange
        Equipo mclaren = equipo(10L, "McLaren F1 Team");
        when(equipoRepo.findById(10L)).thenReturn(Optional.of(mclaren));

        Piloto norris = new Piloto();
        norris.setId(1L);
        norris.setNombre("Lando");
        norris.setApellido("Norris");
        EquipoPiloto ep = new EquipoPiloto();
        ep.setEquipo(mclaren);
        ep.setPiloto(norris);
        ep.setCategoria(categoria(1L, "F1"));
        ep.setTemporada(2025);
        ep.setNumero(4);
        ep.setRol("PILOTO");
        when(equipoPilotoRepo.plantelDeEquipo(10L)).thenReturn(List.of(ep));
        when(resultadoSesionRepo.filasDeEquipo(10L)).thenReturn(List.of(
                fila(1, "CARRERA", 1, "25"),
                fila(1, "CARRERA", 3, "15"),
                fila(1, "CLASIFICACION", 1, null),
                fila(2, "SPRINT", 1, "8")));

        // Act
        PerfilEquipoDto perfil = service.perfil(10L);

        // Assert
        assertThat(perfil.categorias()).containsExactly("F1");
        assertThat(perfil.plantel()).singleElement().satisfies(p -> assertThat(p.piloto()).isEqualTo("Lando Norris"));
        EstadisticasEquipoDto est = perfil.estadisticas();
        assertThat(est.eventos()).as("el sprint no es resultado final del evento").isEqualTo(1);
        assertThat(est.victorias()).isEqualTo(1);
        assertThat(est.podios()).isEqualTo(2);
        assertThat(est.puntos()).as("carreras + sprint").isEqualByComparingTo("48");
        assertThat(perfil.ultimosResultados()).extracting(ResultadoRecienteDto::sesion)
                .as("el evento con solo sprint y los dos pilotos del mismo evento")
                .containsExactly("SPRINT", "CARRERA", "CARRERA");
    }
}
