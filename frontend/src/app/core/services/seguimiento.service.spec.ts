import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CargaSincronizacion } from '../modelos';
import { AuthService } from './auth.service';
import { NotificacionService } from './notificacion.service';
import { SeguimientoService } from './seguimiento.service';
import { SyncServices } from './sync.service';

/**
 * El seguimiento consulta el estado de las sincronizaciones cada 3 s, SOLO mientras hay trabajos en curso,
 * y avisa cuando terminan. Los temporizadores se simulan con vi.useFakeTimers (el test no espera de verdad).
 */
describe('SeguimientoService', () => {
  let respuestas: CargaSincronizacion[][];
  let trabajos: ReturnType<typeof vi.fn>;
  let notificaciones: { exito: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };
  const esAdmin = signal(true);

  function trabajo(estado: CargaSincronizacion['estado'], error: string | null = null): CargaSincronizacion {
    return { id: 'trabajo-1', categoria: 'F1', anio: 2025, estado, creado: '2025-11-22T12:00:00Z', finalizado: null, resumen: null, error };
  }

  function iniciar(): void {
    // Cada consulta devuelve la siguiente respuesta de la lista (la última se repite)
    trabajos = vi.fn(() => of(respuestas.length > 1 ? respuestas.shift()! : respuestas[0]));
    notificaciones = { exito: vi.fn(), error: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: SyncServices, useValue: { trabajos } },
        { provide: NotificacionService, useValue: notificaciones },
        { provide: AuthService, useValue: { esAdmin } },
      ],
    });
    TestBed.inject(SeguimientoService);
    TestBed.tick(); // ejecuta el effect() del constructor
  }

  beforeEach(() => {
    vi.useFakeTimers();
    esAdmin.set(true);
  });

  afterEach(() => vi.useRealTimers());

  it('mientras hay un trabajo en curso consulta cada 3 segundos', () => {
    respuestas = [[trabajo('EN_PROCESO')]];
    iniciar();
    expect(trabajos).toHaveBeenCalledTimes(1);

    vi.advanceTimersByTime(3000);
    vi.advanceTimersByTime(3000);

    expect(trabajos).toHaveBeenCalledTimes(3);
  });

  it('cuando el trabajo termina, avisa una vez y deja de consultar', () => {
    respuestas = [[trabajo('EN_PROCESO')], [trabajo('COMPLETADO')]];
    iniciar();

    vi.advanceTimersByTime(3000);
    vi.advanceTimersByTime(30_000);

    expect(notificaciones.exito).toHaveBeenCalledOnce();
    expect(notificaciones.exito.mock.calls[0][0]).toContain('F1 2025 completada');
    expect(trabajos).toHaveBeenCalledTimes(2);
  });

  it('si el trabajo falla, avisa el error con el motivo', () => {
    respuestas = [[trabajo('EN_PROCESO')], [trabajo('FALLIDO', 'el proveedor no respondió')]];
    iniciar();

    vi.advanceTimersByTime(3000);

    expect(notificaciones.error).toHaveBeenCalledOnce();
    expect(notificaciones.error.mock.calls[0][0]).toContain('el proveedor no respondió');
  });

  it('sin trabajos en curso no consulta más (no genera tráfico inútil)', () => {
    respuestas = [[trabajo('COMPLETADO')]];
    iniciar();

    vi.advanceTimersByTime(30_000);

    expect(trabajos).toHaveBeenCalledTimes(1);
    // Lo que ya estaba terminado al entrar no es una novedad: no se avisa
    expect(notificaciones.exito).not.toHaveBeenCalled();
  });

  it('un usuario que no es administrador no consulta las sincronizaciones', () => {
    esAdmin.set(false);
    respuestas = [[trabajo('EN_PROCESO')]];
    iniciar();

    expect(trabajos).not.toHaveBeenCalled();
  });
});
