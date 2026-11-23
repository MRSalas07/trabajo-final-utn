import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EstadoTransmision, SesionTelemetria, TelemetriaSesion } from '../modelos';

/** Lo que va llegando de una transmisión abierta. */
export type EventoTransmision =
  /** Una foto de la sesión (llega una por segundo). */
  | { tipo: 'estado'; estado: EstadoTransmision }
  /** El servidor no pudo transmitir y explicó por qué (ej: demasiadas repeticiones en curso). */
  | { tipo: 'error'; mensaje: string }
  /** Se cortó la conexión sin aviso (servidor caído, red, ruta inexistente). */
  | { tipo: 'desconexion' };

/** Opciones para empezar una repetición. inicioVuelta tiene prioridad sobre inicioMin. */
export interface OpcionesRepeticion {
  velocidad?: number;
  inicioMin?: number;
  inicioVuelta?: number | null;
  /** Piloto a seguir desde el principio (sin piloto, se sigue al líder). */
  piloto?: number | null;
}

/** Cambios para una transmisión en curso. piloto 0 = volver a seguir al líder. */
export interface AjusteTransmision {
  piloto?: number;
  velocidad?: number;
  pausa?: boolean;
}

/**
 * Telemetría de F1 (MVP 7, módulo plus con datos de OpenF1).
 * El backend solo publica estas rutas si TELEMETRIA_HABILITADA=true: antes de usarlas conviene
 * consultar /api/salud (campo "telemetria"), si no responden 404.
 */
@Injectable({ providedIn: 'root' })
export class TelemetriaService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/telemetria`;

  /** Sesiones del año que ya empezaron (OpenF1 tiene datos desde 2023), de la más reciente a la más vieja. */
  sesiones(anio: number): Observable<SesionTelemetria[]> {
    return this.http.get<SesionTelemetria[]>(`${this.api}/sesiones`, { params: { anio } });
  }

  /**
   * Análisis de una sesión terminada: tiempos por vuelta y evolución de posiciones.
   * pilotos: números de auto a comparar (hasta 6); sin pilotos, el backend elige los 6 mejor ubicados.
   */
  sesion(sessionKey: number, pilotos: number[] = []): Observable<TelemetriaSesion> {
    let params = new HttpParams();
    if (pilotos.length) params = params.set('pilotos', pilotos.join(','));
    return this.http.get<TelemetriaSesion>(`${this.api}/sesiones/${sessionKey}`, { params });
  }

  /** Dirección del stream de una repetición (la ruta es pública: EventSource no puede mandar el token). */
  urlRepeticion(sessionKey: number, opciones: OpcionesRepeticion = {}): string {
    const params = new URLSearchParams({ sessionKey: String(sessionKey) });
    if (opciones.velocidad !== undefined) params.set('velocidad', String(opciones.velocidad));
    if (opciones.inicioVuelta) params.set('inicioVuelta', String(opciones.inicioVuelta));
    else if (opciones.inicioMin) params.set('inicioMin', String(opciones.inicioMin));
    if (opciones.piloto) params.set('piloto', String(opciones.piloto));
    return `${this.api}/transmisiones/repeticion?${params}`;
  }

  /**
   * Abre una transmisión (Server-Sent Events) y emite cada foto. La conexión se cierra al desuscribirse,
   * cuando la transmisión termina (FINALIZADA o ERROR) o ante cualquier error. No se reconecta sola:
   * EventSource reintentaría por su cuenta y, en una repetición, eso la empezaría de nuevo desde el principio.
   */
  transmitir(url: string): Observable<EventoTransmision> {
    return new Observable<EventoTransmision>((suscriptor) => {
      const fuente = new EventSource(url);
      const terminar = (evento: EventoTransmision) => {
        fuente.close();
        suscriptor.next(evento);
        suscriptor.complete();
      };
      fuente.addEventListener('estado', (evento) => {
        let estado: EstadoTransmision;
        try {
          estado = JSON.parse((evento as MessageEvent<string>).data) as EstadoTransmision;
        } catch {
          return; // una foto ilegible se descarta: al segundo llega otra
        }
        if (estado.estado === 'FINALIZADA' || estado.estado === 'ERROR') {
          terminar({ tipo: 'estado', estado });
        } else {
          suscriptor.next({ tipo: 'estado', estado });
        }
      });
      // "error" llega en dos casos: el evento que manda el servidor (trae datos) o la falla de la conexión
      fuente.addEventListener('error', (evento) => {
        const datos = evento instanceof MessageEvent ? evento.data : null;
        terminar(datos ? { tipo: 'error', mensaje: mensajeDelServidor(datos) } : { tipo: 'desconexion' });
      });
      return () => fuente.close();
    });
  }

  /** Cambia el piloto seguido, la velocidad o la pausa de una transmisión propia (responde 204). */
  ajustar(id: string, cambios: AjusteTransmision): Observable<void> {
    let params = new HttpParams();
    if (cambios.piloto !== undefined) params = params.set('piloto', cambios.piloto);
    if (cambios.velocidad !== undefined) params = params.set('velocidad', cambios.velocidad);
    if (cambios.pausa !== undefined) params = params.set('pausa', cambios.pausa);
    return this.http.put<void>(`${this.api}/transmisiones/${encodeURIComponent(id)}`, null, { params });
  }
}

/** El servidor manda {"mensaje": "..."}; si no se puede leer, un texto genérico. */
function mensajeDelServidor(datos: unknown): string {
  try {
    const cuerpo = JSON.parse(String(datos)) as { mensaje?: unknown };
    if (typeof cuerpo.mensaje === 'string' && cuerpo.mensaje) return cuerpo.mensaje;
  } catch {
    // no era JSON
  }
  return 'El servidor no pudo iniciar la transmisión.';
}
