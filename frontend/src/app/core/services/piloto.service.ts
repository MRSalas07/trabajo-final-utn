import { inject,Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ZonaHorariaService } from './zona-horaria.service';
import { SaludServices } from './salud.service';


import { EventoDto, Pagina, PerfilPiloto, PilotoResumen, Temporada } from '../modelos';
import { Observable } from 'rxjs';

export interface Filtros{ 
    filPiloto : string | undefined,
    filtroCategoria:string | undefined,
    filtroAnio : number | null,
    paginado : number | undefined,
    cantidad : number | undefined

}
@Injectable({ providedIn: 'root'})
export class PilotoService {
    private readonly http = inject(HttpClient);
    private readonly apiUrl = environment.apiUrl;
    private readonly zonaService = inject(ZonaHorariaService);

    buscarPilotos(filtros: Filtros) {
        let params = new HttpParams();
        for (const [k, v] of Object.entries(filtros)) {
            if (v !== undefined && v !== null && v !== '') 
                params = params.set(k, String(v));
        }
        return this.http.get<Pagina<PilotoResumen>>(`${this.apiUrl}/pilotos`, { params });
    }

    buscarPilotoId(pilotoId :number): Observable<PerfilPiloto> {
        return this.http.get<PerfilPiloto>(`${this.apiUrl}/pilotos/${pilotoId}`);
    }



}