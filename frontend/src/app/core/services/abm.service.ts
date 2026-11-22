//Services reutilizable para los modelos de temporadas, eventos, sesiones, etapas, pilotos, 
// equipos, tripulaciones, participaciones y resultados
import {HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type RegistroGenerico = Record<string, unknown> & {id: number};

export type NombreRecurso =
    | 'categorias' 
    | 'temporadas' 
    | 'eventos' 
    | 'sesiones' 
    | 'etapas'
    | 'resultados-sesion' 
    | 'resultados-etapa' 
    | 'pilotos' 
    | 'equipos'
    | 'participaciones' 
    | 'tripulaciones' 
    | 'palmares';

export type Filtros = Record<string, string | number | null | undefined>;

@Injectable({ providedIn: 'root' })
export class AbmService {
    private readonly httpClient = inject(HttpClient);

    listar(recurso: NombreRecurso, filtros: Filtros = {}): Observable<RegistroGenerico[]> {
        let params = new HttpParams();
        for (const [clave, valor] of Object.entries(filtros)) {
            if (valor !== null && valor !== undefined && valor !== '') {
                params = params.set(clave, String(valor));
            }
        }
        const url = this.urlDe(recurso);
        return this.httpClient.get<RegistroGenerico[]>(url, { params });
    }

    crear(recurso: NombreRecurso, cuerpo: Record<string, unknown>): Observable<RegistroGenerico> {
        const url = this.urlDe(recurso);
        return this.httpClient.post<RegistroGenerico>(url, cuerpo);
    }

    modificar(recurso: NombreRecurso, id: number, cuerpo: Record<string, unknown>): Observable<RegistroGenerico> {
        const url = this.urlDe(recurso);
        return this.httpClient.put<RegistroGenerico>(`${url}/${id}`, cuerpo);
    }


    eliminar(recurso: NombreRecurso, id: number): Observable<void> {
        const url = this.urlDe(recurso);
        return this.httpClient.delete<void>(`${url}/${id}`);
    }

    private urlDe(recurso: NombreRecurso): string {
        return recurso === 'categorias'
        ? `${environment.apiUrl}/categorias`
        : `${environment.apiUrl}/admin/${recurso}`;
    }
}
