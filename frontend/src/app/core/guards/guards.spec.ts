import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { adminGuard, authGuard } from './guards';

/** Los guards protegen rutas: /cuenta pide sesión iniciada y /admin pide el rol ADMIN. */
describe('guards', () => {
  let router: Router;

  function configurar(sesionIniciada: boolean, esAdmin: boolean): void {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { auth: () => sesionIniciada, esAdmin: () => esAdmin } },
      ],
    });
    router = TestBed.inject(Router);
  }

  /** Los guards usan inject(): se ejecutan dentro del contexto de inyección del TestBed. */
  function ejecutar(guard: typeof authGuard, url = '/cuenta'): boolean | UrlTree {
    const estado = { url } as RouterStateSnapshot;
    return TestBed.runInInjectionContext(() => guard({} as ActivatedRouteSnapshot, estado)) as boolean | UrlTree;
  }

  it('authGuard deja pasar con la sesión iniciada', () => {
    configurar(true, false);

    expect(ejecutar(authGuard)).toBe(true);
  });

  it('authGuard sin sesión manda al login y recuerda a dónde volver', () => {
    configurar(false, false);

    const resultado = ejecutar(authGuard, '/cuenta');

    expect(resultado).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(resultado as UrlTree)).toBe('/login?volver=%2Fcuenta');
  });

  it('adminGuard deja pasar a un administrador', () => {
    configurar(true, true);

    expect(ejecutar(adminGuard, '/admin')).toBe(true);
  });

  it('adminGuard manda al inicio a un usuario común', () => {
    configurar(true, false);

    const resultado = ejecutar(adminGuard, '/admin');

    expect(router.serializeUrl(resultado as UrlTree)).toBe('/');
  });
});
