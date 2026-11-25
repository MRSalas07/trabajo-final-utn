import { SesionTelemetria } from '../../core/modelos';
import { agruparSesiones, datosLiberados, sesionPorDefecto, traducirSesion } from './sesiones';

/** Funciones puras de la página de telemetría: nombres, regla de la media hora de OpenF1 y agrupación. */
describe('sesiones de telemetría', () => {
  const MINUTO = 60_000;
  const ahora = Date.parse('2026-11-22T12:00:00Z');

  function sesion(sessionKey: number, nombre: string, circuito: string, finHaceMinutos: number | null): SesionTelemetria {
    const fin = finHaceMinutos === null ? null : new Date(ahora - finHaceMinutos * MINUTO).toISOString();
    return {
      sessionKey, nombre, tipo: nombre === 'Race' ? 'Race' : 'Practice',
      inicio: new Date(ahora - 3 * 60 * MINUTO).toISOString(), fin,
      pais: 'United States', circuito, ubicacion: circuito,
    };
  }

  it('traduce los nombres de sesión de OpenF1', () => {
    expect(traducirSesion('Race')).toBe('Carrera');
    expect(traducirSesion('Practice 2')).toBe('Práctica 2');
    expect(traducirSesion('Sprint Qualifying')).toBe('Clasificación sprint');
    expect(traducirSesion('Day 2')).toBe('Pretemporada · día 2');
    expect(traducirSesion('Algo nuevo')).toBe('Algo nuevo');
    expect(traducirSesion(null)).toBe('Sesión');
  });

  it('OpenF1 libera los datos media hora después del final', () => {
    expect(datosLiberados(sesion(1, 'Race', 'Las Vegas', 31), ahora)).toBe(true);
    expect(datosLiberados(sesion(1, 'Race', 'Las Vegas', 10), ahora)).toBe(false);
  });

  it('sin fin informado, se toma el inicio más 4 horas', () => {
    // Empezó hace 3 horas: todavía no se cumplieron las 4 h + 30 min
    expect(datosLiberados(sesion(1, 'Race', 'Las Vegas', null), ahora)).toBe(false);
  });

  it('al entrar se muestra la última carrera con datos liberados', () => {
    // sesionPorDefecto usa la hora actual: se fija el reloj para que el test no dependa del día
    vi.useFakeTimers({ now: ahora });
    const sesiones = [
      sesion(3, 'Race', 'Las Vegas', 10), // terminó hace 10 min: todavía no liberada
      sesion(2, 'Qualifying', 'Las Vegas', 24 * 60),
      sesion(1, 'Race', 'Interlagos', 7 * 24 * 60),
    ];

    expect(sesionPorDefecto(sesiones)?.sessionKey).toBe(1);
    vi.useRealTimers();
  });

  it('agrupa por fin de semana las sesiones seguidas del mismo circuito', () => {
    const grupos = agruparSesiones(
      [sesion(3, 'Race', 'Las Vegas', 60), sesion(2, 'Qualifying', 'Las Vegas', 1500), sesion(1, 'Race', 'Interlagos', 9000)],
      'UTC',
    );

    expect(grupos.map((grupo) => grupo.sesiones.length)).toEqual([2, 1]);
    expect(grupos[0].titulo).toBe('Las Vegas · United States');
    // Las claves son únicas aunque un circuito aparezca dos veces en el año
    expect(new Set(grupos.map((grupo) => grupo.clave)).size).toBe(2);
  });
});
