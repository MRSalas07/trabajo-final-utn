import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { TokenResponse } from '../login/Usuario';
import { AuthService } from './auth.service';
import { ZonaHorariaService } from './zona-horaria.service';

/** La sesión se guarda en el navegador y se recupera al recargar, salvo que el token haya vencido. */
describe('AuthService', () => {
  const CLAVE_SESION = 'ma.sesion';

  function sesion(roles: string[], venceEnMinutos: number): TokenResponse {
    return {
      token: 'token-123',
      expira: new Date(Date.now() + venceEnMinutos * 60_000).toISOString(),
      usuario: { id: 1, nombre: 'Ana', apellido: null, email: 'ana@ejemplo.com', zonaHoraria: 'Europe/Madrid', roles },
    };
  }

  function crearServicio(): AuthService {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    return TestBed.inject(AuthService);
  }

  beforeEach(() => localStorage.clear());

  it('recupera la sesión guardada si el token sigue vigente', () => {
    localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion(['USER', 'ADMIN'], 60)));

    const auth = crearServicio();

    expect(auth.auth()).toBe(true);
    expect(auth.token).toBe('token-123');
    expect(auth.esAdmin()).toBe(true);
  });

  it('descarta la sesión guardada si el token venció', () => {
    localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion(['USER'], -5)));

    const auth = crearServicio();

    expect(auth.auth()).toBe(false);
    expect(auth.token).toBeNull();
  });

  it('login guarda la sesión y adopta la zona horaria del perfil', () => {
    const auth = crearServicio();
    const backend = TestBed.inject(HttpTestingController);

    auth.login('ana@ejemplo.com', 'secreta').subscribe();
    backend.expectOne(`${environment.apiUrl}/auth/login`).flush(sesion(['USER'], 60));

    expect(auth.auth()).toBe(true);
    expect(auth.esAdmin()).toBe(false);
    expect(TestBed.inject(ZonaHorariaService).zona()).toBe('Europe/Madrid');
    expect(localStorage.getItem(CLAVE_SESION)).toContain('token-123');
    backend.verify();
  });

  it('cerrarSesion borra la sesión del navegador', () => {
    localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion(['USER'], 60)));
    const auth = crearServicio();

    auth.cerrarSesion();

    expect(auth.auth()).toBe(false);
    expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
  });
});
