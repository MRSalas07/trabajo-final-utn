import { inject,Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ZonaHorariaService } from './zona-horaria.service';
import { EventoDto } from '../modelos';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root'})
export class CalendarioService {
    
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

    /** El .ics de toda la temporada: se pide con HttpClient para poder avisar si todavía no hay datos (404). */
    descargarIcsTemporada(siglas: string, anio: number): Observable<Blob> {
        return this.http.get(`${this.apiUrl}/calendario/${siglas}/${anio}.ics`, { responseType: 'blob' });
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