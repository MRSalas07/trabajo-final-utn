import { Injectable, signal, computed } from '@angular/core';

const CLAVE = 'ma.zona';

@Injectable({
  providedIn: 'root',
})
export class ZonaHorariaService {
    readonly zonaNavegador = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';

    readonly zona = signal<string>(this.leer() ?? this.zonaNavegador);

    private readonly zonasNavegador: string[] = this.listarZonas();

    readonly zonasDisponibles = computed(() => Array.from(new Set([this.zona(), ...this.zonasNavegador])));


    cambiarZona(nuevaZona: string) {
        this.zona.set(nuevaZona);
        try {
            localStorage.setItem(CLAVE, nuevaZona);
            } catch {
            /* almacenamiento no disponible (modo privado): solo se pierde la preferencia */
            }

    }

    private leer(): string | null {
        try {
            return localStorage.getItem(CLAVE);
        } catch {
            return null;
        }
    } 


    private listarZonas(): string[] {
        const intl = Intl as unknown as { supportedValuesOf?: (k: string) => string[] };
        const zonas = intl.supportedValuesOf ? intl.supportedValuesOf('timeZone') : [];
        const base = zonas.length ? zonas : ['UTC', 'America/Argentina/Buenos_Aires', 'Europe/Madrid', 'America/New_York'];
        return Array.from(new Set([this.zonaNavegador, 'UTC', ...base]));
  }

}