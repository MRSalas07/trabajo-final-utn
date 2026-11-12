package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.auth.AuthDto.*;
import com.tfi.motorsport.backend.dtos.auth.RegisterDto;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Usuario;
import com.tfi.motorsport.backend.repositories.UsuarioJPARepository;
import com.tfi.motorsport.backend.security.JwtService;
import com.tfi.motorsport.backend.services.IAuthService;
import com.tfi.motorsport.backend.services.ZonaHoraria;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    /** Mensaje genérico: no revela si el email existe (evita enumeración de usuarios). */
    private static final String CREDENCIALES_INVALIDAS = "Email o contraseña incorrectos";

    private final UsuarioJPARepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;


    @Override
    @Transactional
    public TokenResponse registrar(RegisterDto req) {
        String email = req.email().trim().toLowerCase();
        if (usuarioRepo.existsByEmailIgnoreCase(email)) {
            throw new RuleBusinessException("El email ya está registrado", HttpStatus.CONFLICT);
        }
        String zona = req.zonaHoraria() == null || req.zonaHoraria().isBlank()
                ? "UTC" : ZonaHoraria.resolverZonaHoraria(req.zonaHoraria()).getId();

        Usuario u = new Usuario();
        u.setNombre(req.nombre().trim());
        u.setApellido(req.apellido() != null ? req.apellido().trim() : null);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setZonaHoraria(zona);
        u.setFechaRegistro(LocalDateTime.now());
        u.setActivo(true);
        return respuesta(usuarioRepo.save(u));
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        Usuario u = usuarioRepo.findByEmailIgnoreCaseAndEliminadoFalse(req.email().trim())
                .orElseThrow(() -> new RuleBusinessException(CREDENCIALES_INVALIDAS, HttpStatus.UNAUTHORIZED));
        if (!Boolean.TRUE.equals(u.getActivo()) || !passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw new RuleBusinessException(CREDENCIALES_INVALIDAS, HttpStatus.UNAUTHORIZED);
        }
        return respuesta(u);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDto perfil(Long usuarioId) {
        return dto(buscar(usuarioId));
    }

    @Override
    @Transactional
    public TokenResponse actualizarPreferencias(Long usuarioId, PreferenciasRequest req) {
        throw new UnsupportedOperationException("No implementado aún");
    }

    private Usuario buscar(Long id) {
        return usuarioRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("el usuario", id));
    }

    private TokenResponse respuesta(Usuario u) {
        JwtService.TokenGenerado token = jwtService.generarToken(u);
        return new TokenResponse(token.token(), token.expira(), dto(u));
    }

    private UsuarioDto dto(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNombre(), u.getApellido(), u.getEmail(), u.getZonaHoraria(),
                jwtService.rolesDe(u));
    }
}
