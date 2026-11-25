package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.auth.AuthDto.LoginRequest;
import com.tfi.motorsport.backend.dtos.auth.AuthDto.PreferenciasRequest;
import com.tfi.motorsport.backend.dtos.auth.AuthDto.TokenResponse;
import com.tfi.motorsport.backend.dtos.auth.AuthDto.UsuarioDto;
import com.tfi.motorsport.backend.dtos.auth.RegisterDto;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Usuario;
import com.tfi.motorsport.backend.repositories.UsuarioJPARepository;
import com.tfi.motorsport.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String MENSAJE_CREDENCIALES = "Email o contraseña incorrectos";

    @Mock
    private UsuarioJPARepository usuarioRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl service;

    private static Usuario usuario(Long id, String email, boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Mateo");
        usuario.setApellido("Salas");
        usuario.setEmail(email);
        usuario.setPasswordHash("hash-guardado");
        usuario.setZonaHoraria("UTC");
        usuario.setActivo(activo);
        return usuario;
    }

    private void prepararToken() {
        when(jwtService.generarToken(any(Usuario.class)))
                .thenReturn(new JwtService.TokenGenerado("token-de-prueba", Instant.parse("2030-01-01T00:00:00Z"), List.of("USER")));
        when(jwtService.rolesDe(any(Usuario.class))).thenReturn(List.of("USER"));
    }


    @Test
    void registrar_guardaElUsuarioConEmailNormalizadoYContrasenaCodificada() {
        // Arrange
        RegisterDto pedido = new RegisterDto("  Mateo ", " Salas ", "  Mateo@Correo.COM ", "clave-segura-1", "America/Argentina/Buenos_Aires");
        when(usuarioRepo.existsByEmailIgnoreCase("mateo@correo.com")).thenReturn(false);
        when(passwordEncoder.encode("clave-segura-1")).thenReturn("hash-bcrypt");
        when(usuarioRepo.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        prepararToken();

        // Act
        TokenResponse respuesta = service.registrar(pedido);

        // Assert
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepo).save(guardado.capture());
        assertThat(guardado.getValue().getEmail()).isEqualTo("mateo@correo.com");
        assertThat(guardado.getValue().getNombre()).isEqualTo("Mateo");
        assertThat(guardado.getValue().getApellido()).isEqualTo("Salas");
        assertThat(guardado.getValue().getPasswordHash()).isEqualTo("hash-bcrypt");
        assertThat(guardado.getValue().getZonaHoraria()).isEqualTo("America/Argentina/Buenos_Aires");
        assertThat(guardado.getValue().getActivo()).isTrue();
        assertThat(guardado.getValue().getFechaRegistro()).isNotNull();
        assertThat(respuesta.token()).isEqualTo("token-de-prueba");
        assertThat(respuesta.usuario().email()).isEqualTo("mateo@correo.com");
    }

    @Test
    void registrar_sinZonaHoraria_usaUtcPorDefecto() {
        // Arrange
        RegisterDto pedido = new RegisterDto("Ana", null, "ana@correo.com", "clave-segura-1", "  ");
        when(usuarioRepo.existsByEmailIgnoreCase("ana@correo.com")).thenReturn(false);
        when(passwordEncoder.encode("clave-segura-1")).thenReturn("hash-bcrypt");
        when(usuarioRepo.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        prepararToken();

        // Act
        service.registrar(pedido);

        // Assert
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepo).save(guardado.capture());
        assertThat(guardado.getValue().getZonaHoraria()).isEqualTo("UTC");
        assertThat(guardado.getValue().getApellido()).isNull();
    }

    @Test
    void registrar_conEmailYaRegistrado_lanzaConflictoYNoGuarda() {
        // Arrange
        RegisterDto pedido = new RegisterDto("Mateo", "Salas", "repetido@correo.com", "clave-segura-1", null);
        when(usuarioRepo.existsByEmailIgnoreCase("repetido@correo.com")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> service.registrar(pedido))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessage("El email ya está registrado")
                .satisfies(error -> assertThat(((RuleBusinessException) error).getEstado()).isEqualTo(HttpStatus.CONFLICT));
        verify(usuarioRepo, never()).save(any());
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void registrar_conZonaHorariaInvalida_lanzaErrorYNoGuarda() {
        // Arrange
        RegisterDto pedido = new RegisterDto("Mateo", "Salas", "mateo@correo.com", "clave-segura-1", "Marte/Olympus");
        when(usuarioRepo.existsByEmailIgnoreCase("mateo@correo.com")).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> service.registrar(pedido))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessageContaining("Zona horaria inválida");
        verify(usuarioRepo, never()).save(any());
    }


    @Test
    void login_conCredencialesCorrectas_devuelveTokenYUsuario() {
        // Arrange
        Usuario existente = usuario(7L, "mateo@correo.com", true);
        when(usuarioRepo.findByEmailIgnoreCaseAndEliminadoFalse("mateo@correo.com")).thenReturn(Optional.of(existente));
        when(passwordEncoder.matches("clave-segura-1", "hash-guardado")).thenReturn(true);
        prepararToken();

        // Act
        TokenResponse respuesta = service.login(new LoginRequest(" mateo@correo.com ", "clave-segura-1"));

        // Assert
        assertThat(respuesta.token()).isEqualTo("token-de-prueba");
        assertThat(respuesta.usuario().id()).isEqualTo(7L);
        assertThat(respuesta.usuario().roles()).containsExactly("USER");
    }

    @Test
    void login_conEmailInexistente_respondeNoAutorizadoConMensajeGenerico() {
        // Arrange
        when(usuarioRepo.findByEmailIgnoreCaseAndEliminadoFalse("nadie@correo.com")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.login(new LoginRequest("nadie@correo.com", "cualquiera")))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessage(MENSAJE_CREDENCIALES)
                .satisfies(error -> assertThat(((RuleBusinessException) error).getEstado()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_conContrasenaIncorrecta_usaElMismoMensajeQueConEmailInexistente() {
        // Arrange
        Usuario existente = usuario(7L, "mateo@correo.com", true);
        when(usuarioRepo.findByEmailIgnoreCaseAndEliminadoFalse("mateo@correo.com")).thenReturn(Optional.of(existente));
        when(passwordEncoder.matches("equivocada", "hash-guardado")).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> service.login(new LoginRequest("mateo@correo.com", "equivocada")))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessage(MENSAJE_CREDENCIALES)
                .satisfies(error -> assertThat(((RuleBusinessException) error).getEstado()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_conUsuarioInactivo_rechazaSinSiquieraComprobarLaContrasena() {
        // Arrange
        Usuario inactivo = usuario(8L, "baja@correo.com", false);
        when(usuarioRepo.findByEmailIgnoreCaseAndEliminadoFalse("baja@correo.com")).thenReturn(Optional.of(inactivo));

        // Act + Assert
        assertThatThrownBy(() -> service.login(new LoginRequest("baja@correo.com", "clave-segura-1")))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessage(MENSAJE_CREDENCIALES);
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void perfil_devuelveLosDatosDelUsuarioConSusRoles() {
        // Arrange
        when(usuarioRepo.findById(7L)).thenReturn(Optional.of(usuario(7L, "mateo@correo.com", true)));
        when(jwtService.rolesDe(any(Usuario.class))).thenReturn(List.of("USER", "ADMIN"));

        // Act
        UsuarioDto perfil = service.perfil(7L);

        // Assert
        assertThat(perfil.id()).isEqualTo(7L);
        assertThat(perfil.nombre()).isEqualTo("Mateo");
        assertThat(perfil.email()).isEqualTo("mateo@correo.com");
        assertThat(perfil.roles()).containsExactly("USER", "ADMIN");
    }

    @Test
    void perfil_conUsuarioInexistente_lanzaRecursoNoEncontrado() {
        // Arrange
        when(usuarioRepo.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.perfil(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void actualizarPreferencias_guardaLaZonaNuevaYDevuelveUnTokenNuevo() {
        // Arrange
        Usuario existente = usuario(7L, "mateo@correo.com", true);
        when(usuarioRepo.findById(7L)).thenReturn(Optional.of(existente));
        when(usuarioRepo.save(existente)).thenReturn(existente);
        prepararToken();

        // Act
        TokenResponse respuesta = service.actualizarPreferencias(7L, new PreferenciasRequest("Europe/Madrid"));

        // Assert
        assertThat(existente.getZonaHoraria()).isEqualTo("Europe/Madrid");
        assertThat(respuesta.token()).isEqualTo("token-de-prueba");
        assertThat(respuesta.usuario().zonaHoraria()).isEqualTo("Europe/Madrid");
        verify(usuarioRepo).save(existente);
    }

    @Test
    void actualizarPreferencias_conZonaInvalida_lanzaErrorYNoGuarda() {
        // Arrange
        Usuario existente = usuario(7L, "mateo@correo.com", true);
        when(usuarioRepo.findById(7L)).thenReturn(Optional.of(existente));

        // Act + Assert
        assertThatThrownBy(() -> service.actualizarPreferencias(7L, new PreferenciasRequest("No/Existe")))
                .isInstanceOf(RuleBusinessException.class)
                .hasMessageContaining("Zona horaria inválida");
        verify(usuarioRepo, never()).save(any());
    }

    @Test
    void actualizarPreferencias_conUsuarioInexistente_lanzaRecursoNoEncontrado() {
        // Arrange
        when(usuarioRepo.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.actualizarPreferencias(99L, new PreferenciasRequest("Europe/Madrid")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(usuarioRepo, never()).save(any());
    }
}
