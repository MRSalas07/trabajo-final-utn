import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  ComparativaHistorica,
  ComparativaPilotos,
  EvolucionCampeonato,
  EvolucionGrupos,
  FilaMarca,
  Historico,
  TendenciaGrupo,
  TipoGrupo,
  FilaCampeonatoEquipo,
  FilaCampeonatoPiloto,
  ResumenTemporada,
  TendenciaPiloto,
} from '../modelos';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class EstadisticaService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/estadisticas`;

  resumen(siglas: string, anio: number) {
    return this.http.get<ResumenTemporada>(`${this.api}/${siglas}/${anio}/resumen`);
  }
  campeonatoPilotos(siglas: string, anio: number) {
    return this.http.get<FilaCampeonatoPiloto[]>(`${this.api}/${siglas}/${anio}/campeonato/pilotos`);
  }
  campeonatoEquipos(siglas: string, anio: number) {
    return this.http.get<FilaCampeonatoEquipo[]>(`${this.api}/${siglas}/${anio}/campeonato/equipos`);
  }
  evolucion(siglas: string, anio: number, pilotos: number[] = [], top = 5) {
    let params = new HttpParams().set('top', top);
    if (pilotos.length) params = params.set('pilotos', pilotos.join(','));
    return this.http.get<EvolucionCampeonato>(`${this.api}/${siglas}/${anio}/evolucion`, { params });
  }
  tendencia(siglas: string, anio: number, pilotoId: number) {
    return this.http.get<TendenciaPiloto>(`${this.api}/${siglas}/${anio}/tendencia/${pilotoId}`);
  }
  comparar(siglas: string, anio: number, a: number, b: number) {
    return this.http.get<ComparativaPilotos>(`${this.api}/${siglas}/${anio}/comparativa`, { params: { a, b } });
  }

  recalcularPalmares(siglas: string): Observable<{ categoria: string; pilotosActualizados: number }> {
    return this.http.post<{ categoria: string; pilotosActualizados: number }>(
      `${this.api}/${siglas}/palmares/recalcular`,
      {},
    );
  }

  // ---------------------------------------------------------------- MVP 6: equipos y marcas
  marcas(siglas: string, anio: number): Observable<FilaMarca[]> {
    return this.http.get<FilaMarca[]>(`${this.api}/${siglas}/${anio}/marcas`);
  }

  evolucionGrupos(siglas: string, anio: number, tipo: TipoGrupo, top = 6): Observable<EvolucionGrupos> {
    return this.http.get<EvolucionGrupos>(`${this.api}/${siglas}/${anio}/evolucion-grupos`, { params: { tipo, top } });
  }

  tendenciaEquipo(siglas: string, anio: number, equipoId: number): Observable<TendenciaGrupo> {
    return this.http.get<TendenciaGrupo>(`${this.api}/${siglas}/${anio}/tendencia-equipo/${equipoId}`);
  }

  tendenciaMarca(siglas: string, anio: number, marca: string): Observable<TendenciaGrupo> {
    return this.http.get<TendenciaGrupo>(`${this.api}/${siglas}/${anio}/tendencia-marca`, { params: { marca } });
  }

  // ---------------------------------------------------------------- MVP 6: histórico
  historicoPiloto(siglas: string, pilotoId: number): Observable<Historico> {
    return this.http.get<Historico>(`${this.api}/${siglas}/historico/pilotos/${pilotoId}`);
  }

  historicoEquipo(siglas: string, equipoId: number): Observable<Historico> {
    return this.http.get<Historico>(`${this.api}/${siglas}/historico/equipos/${equipoId}`);
  }

  comparativaHistorica(siglas: string, a: number, b: number): Observable<ComparativaHistorica> {
    return this.http.get<ComparativaHistorica>(`${this.api}/${siglas}/historico/comparativa`, { params: { a, b } });
  }
}
