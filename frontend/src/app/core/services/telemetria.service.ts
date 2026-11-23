import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SesionTelemetria, TelemetriaSesion } from '../modelos';

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
}
