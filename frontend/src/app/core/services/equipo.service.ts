import { inject,Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ZonaHorariaService } from './zona-horaria.service';
import { SaludServices } from './salud.service';


import { EquipoResumen, EventoDto, Pagina, PerfilEquipo, PerfilPiloto, PilotoResumen, Temporada } from '../modelos';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root'})
export class EquipoService {
    private readonly http = inject(HttpClient);
    private readonly apiUrl = environment.apiUrl;

    buscarEquipos(categoria: string, anio?: number | null): Observable<EquipoResumen[]> {
        const params: Record<string, string> = { categoria };
        if (anio) 
            params['anio'] = String(anio);
        return this.http.get<EquipoResumen[]>(`${this.apiUrl}/equipos`, { params });
    }
    buscarEquipoId(equipoId: number): Observable<PerfilEquipo> { 
        return this.http.get<PerfilEquipo>(`${this.apiUrl}/equipos/${equipoId}`); 
    }

}