import {HttpClient } from '@angular/common/http'
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenResponse, Usuario } from '../login/Usuario';
import {ZonaHorariaService } from  '../services/zona-horaria.service'


const CLAVE_SESION = 'ma.sesion';

interface UsuarioCreate {
    nombre : string,
    apellido : string | null,
    email: string,
    password: string,
    zonaHoraria : string
}

@Injectable({ providedIn: 'root'})
export class AuthService {
    private readonly http = inject(HttpClient)

    private readonly apiUrl = environment.apiUrl;

    private readonly sesion = signal<TokenResponse | null>(null);

    readonly usuario = computed<Usuario | null>(() => this.sesion()?.usuario ?? null);

    readonly auth = computed (() => this.usuario()?.roles.includes('ADMIN') ?? false );

    private readonly zonas = inject(ZonaHorariaService);

    get token(): string | null {
        return this.sesion()?.token ?? null;
    }
    
    
    login( email:string, password: string) : Observable<TokenResponse> {
        return this.http.post<TokenResponse>(`${this.apiUrl}/auth/login`, {
            email, password}
        ).pipe(tap((response)=> this.guardarStorage(response)));
    }

    registrar( datos: UsuarioCreate ) {
        return this.http.post<TokenResponse>(`${this.apiUrl}/auth/registro`,
            datos ).pipe(tap((response) => this.guardarStorage(response)))
    }

    cerrarSesion() : void {
        this.sesion.set(null)
    }
    guardarZona(zonaHoraria: string) {
        return this.http
        .put<TokenResponse>(`${this.apiUrl}/usuarios/me/preferencias`, { zonaHoraria })
        .pipe(tap((r) => this.guardarStorage(r)));
    }

    private guardarStorage(r: TokenResponse): void {
        this.sesion.set(r);
    // Al iniciar sesión se adopta la zona guardada en el perfil del usuario
        if (r.usuario.zonaHoraria) {
            this.zonas.cambiarZona(r.usuario.zonaHoraria);
        }
        try {
            localStorage.setItem(CLAVE_SESION, JSON.stringify(r));
        } catch {
      /* sin almacenamiento: la sesión dura hasta recargar */
    }
  }

    private restaurar(): TokenResponse | null {
        try {
            const crudo = localStorage.getItem(CLAVE_SESION);
            if (!crudo) return null;
            const r = JSON.parse(crudo) as TokenResponse;
            // Token vencido: se descarta
            return new Date(r.expira).getTime() > Date.now() ? r : null;
        } catch {
            return null;
        }
    }


}