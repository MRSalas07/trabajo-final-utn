import { inject,Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ZonaHorariaService } from './zona-horaria.service';
import { Categoria, PerfilCategoria } from '../modelos';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SaludServices {
    private readonly http = inject(HttpClient);
    private readonly apiUrl = environment.apiUrl;
    private readonly zonas = inject(ZonaHorariaService);

    probarSalud() {
        const url = `${this.apiUrl}/salud`;
        return this.http.get(url);
    }

    categorias(): Observable<Categoria[]> {
        const url = `${this.apiUrl}/categorias`;
        return this.http.get<Categoria[]>(url, { params: { activo: true } });
    }

    /** Ficha de la categoría: temporadas, temporada actual, resumen, próximo y último evento (MVP 4). */
    perfil(siglas: string): Observable<PerfilCategoria> {
        const url = `${this.apiUrl}/categorias/${siglas}/perfil`;
        return this.http.get<PerfilCategoria>(url, { params: this.enviarConZona() });
    }

    private enviarConZona(params: Record<string, string | number | undefined | null> = {}): HttpParams {
        let param = new HttpParams().set('zona', this.zonas.zona());
        for (const key in params) {
            if (params[key] !== undefined && params[key] !== null && params[key] !== '') {
                param = param.set(key, String(params[key]));
            }
        }
        return param;
    }
}
