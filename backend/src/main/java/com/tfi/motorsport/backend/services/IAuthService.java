package com.tfi.motorsport.backend.services;

import com.tfi.motorsport.backend.dtos.auth.AuthDto.*;
import com.tfi.motorsport.backend.dtos.auth.RegisterDto;

/** Módulo de gestión de usuarios: registro, login y preferencias (zona horaria). */
public interface IAuthService {

    TokenResponse registrar(RegisterDto dto);

    TokenResponse login(LoginRequest request);

    UsuarioDto perfil(Long usuarioId);

    /** Actualiza la zona horaria y devuelve un token nuevo (el token incluye la zona). */
    TokenResponse actualizarPreferencias(Long usuarioId, PreferenciasRequest request);
}
