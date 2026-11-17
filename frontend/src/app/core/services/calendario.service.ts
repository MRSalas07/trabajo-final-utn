import { inject,Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ZonaHorariaService } from './zona-horaria.service';
import { SaludServices } from './salud.service';


import { EventoDto, Temporada } from '../modelos';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root'})
export class Calendario {
    
    private readonly http = inject(HttpClient);
    private readonly apiUrl = environment.apiUrl;
    private readonly zonaService = inject(ZonaHorariaService);

    calendario(desde: string, hasta: string, categorias: string[]): Observable<EventoDto[]> {
        return this.http.get<EventoDto[]>(`${this.apiUrl}/calendario`, {
            params: this.enviarConZona({ desde, hasta, categorias: categorias.join(',') }),
        });
    }

    /** URL directa del .ics (el navegador lo descarga y el celular ofrece agregarlo al calendario). */
    urlIcsEvento(id: number): string {
        return `${this.apiUrl}/calendario/eventos/${id}.ics`;
    }

    urlIcsTemporada(siglas: string, anio: number): string { 
        return `${this.apiUrl}/calendario/${siglas}/${anio}.ics`; 
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