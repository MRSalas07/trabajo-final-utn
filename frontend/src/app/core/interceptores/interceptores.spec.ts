import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './interceptores';

/**
 * El interceptor agrega el token solo a los pedidos al backend propio y, si el backend responde 401
 * con una sesión abierta (token vencido), la cierra y manda al login.
 */
describe('authInterceptor', () => {
  const API = environment.apiUrl;
  let http: HttpClient;
  let backend: HttpTestingController;
  let router: Router;
  let auth: { token: string | null; cerrarSesion: ReturnType<typeof vi.fn> };

  function configurar(token: string | null): void {
    auth = { token, cerrarSesion: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  }

  afterEach(() => backend.verify());

  it('agrega el token a los pedidos al backend', () => {
    configurar('token-123');

    http.get(`${API}/categorias`).subscribe();

    const pedido = backend.expectOne(`${API}/categorias`);
    expect(pedido.request.headers.get('Authorization')).toBe('Bearer token-123');
    pedido.flush([]);
  });

  it('no manda el token a otros sitios (ej: la API de OpenF1)', () => {
    configurar('token-123');

    http.get('https://api.openf1.org/v1/sessions').subscribe();

    const pedido = backend.expectOne('https://api.openf1.org/v1/sessions');
    expect(pedido.request.headers.has('Authorization')).toBe(false);
    pedido.flush([]);
  });

  it('sin sesión iniciada, el pedido sale sin token', () => {
    configurar(null);

    http.get(`${API}/categorias`).subscribe();

    const pedido = backend.expectOne(`${API}/categorias`);
    expect(pedido.request.headers.has('Authorization')).toBe(false);
    pedido.flush([]);
  });

  it('un 401 con la sesión abierta (token vencido) cierra la sesión y manda al login', () => {
    configurar('token-vencido');
    const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    let estadoDelError = 0;

    http.get(`${API}/usuarios/me`).subscribe({ error: (e) => (estadoDelError = e.status) });
    backend.expectOne(`${API}/usuarios/me`).flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(auth.cerrarSesion).toHaveBeenCalledOnce();
    expect(navegar).toHaveBeenCalledWith(['/login'], { queryParams: { expirada: 1 } });
    // El error igual llega a quien hizo el pedido (para mostrar el mensaje)
    expect(estadoDelError).toBe(401);
  });

  it('un 401 sin sesión (ej: login con contraseña incorrecta) no cierra nada', () => {
    configurar(null);
    const navegar = vi.spyOn(router, 'navigate');

    http.post(`${API}/auth/login`, {}).subscribe({ error: () => undefined });
    backend.expectOne(`${API}/auth/login`).flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(auth.cerrarSesion).not.toHaveBeenCalled();
    expect(navegar).not.toHaveBeenCalled();
  });
});
