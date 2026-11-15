import { inject,Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ZonaHorariaService } from './zona-horaria.service';
import { SaludServices } from './salud.service';


import { EventoDto, TablaEtapa, TablaSesion, Temporada } from '../modelos';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root'})
export class ResultadoService {
    private readonly http = inject(HttpClient);
    private readonly apiUrl = environment.apiUrl;
    private readonly zonaService = inject(ZonaHorariaService);

    evento(id: number): Observable<EventoDto> {
        return this.http.get<EventoDto>(
            `${this.apiUrl}/eventos/${id}`, { params: this.enviarConZona() }
        );
    }


    resultadosSesion(id: number): Observable<TablaSesion> {
        return this.http.get<TablaSesion>(
            `${this.apiUrl}/sesiones/${id}/resultados`, { params: this.enviarConZona() }
        );
    }
    resultadosEtapa(id: number): Observable<TablaEtapa> {
        return this.http.get<TablaEtapa>(
            `${this.apiUrl}/etapas/${id}/resultados`, { params: this.enviarConZona() }
        );
    }

    private enviarConZona(params: Record<string, string | number | undefined | null> = {}): HttpParams {
        let param = new HttpParams().set('zona', this.zonaService.zona());
        for (const key in params) {
            if (params[key] !== undefined && params[key] !== null && params[key] !== '') {
                param = param.set(key, String(params[key]));
            }
        }
        return param;
    }

}