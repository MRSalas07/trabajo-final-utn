import { inject,Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ZonaHorariaService } from './zona-horaria.service';
import { SaludServices } from './salud.service';


import { Evento, Temporada } from '../modelos';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root'})
export class EventoService {
    
    private readonly http = inject(HttpClient);
    private readonly apiUrl = environment.apiUrl;
    private readonly zonaService = inject(ZonaHorariaService);

    buscarTemporadas(siglas: string): Observable<Temporada[]> {
        return this.http.get<Temporada[]>(`${this.apiUrl}/categorias/${siglas}/temporadas`);
    }

    eventosDeTemporada(siglas: string, anio: number): Observable<Evento[]> {
    return this.http.get<Evento[]>(`${this.apiUrl}/categorias/${siglas}/temporadas/${anio}/eventos`, {
        params: this.enviarConZona(),
    });

    
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