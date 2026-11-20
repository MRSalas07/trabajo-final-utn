import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FuenteDisponible, ResumenSincronizacion, CargaSincronizacion } from './../modelos';

//Administra la integración de Datos
@Injectable({providedIn: 'root'})
export class SyncServices {
    private readonly httpClient = inject(HttpClient);
    private readonly apiUrl = environment.apiUrl;
    private readonly url = `${this.apiUrl}/admin`;

    obtenerFuentes() : Observable<FuenteDisponible[]> {
        return this.httpClient.get<FuenteDisponible[]>(`${this.url}/fuentes`);
    }

    sincronizarDatosxCategoria(categoria: string, anio: number): Observable<CargaSincronizacion> {
        return this.httpClient.post<CargaSincronizacion>(`${this.url}/sincronizaciones`, { categoria, anio });
    }
    trabajos(): Observable<CargaSincronizacion[]> {
        return this.httpClient.get<CargaSincronizacion[]>(`${this.url}/sincronizaciones`);
    }
  /** Envía un archivo JSON con el formato normalizado */
    importarDatosxJson(json: unknown): Observable<ResumenSincronizacion> {
        return this.httpClient.post<ResumenSincronizacion>(`${this.url}/importaciones`, json);
    }

}