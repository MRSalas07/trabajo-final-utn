import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class ZonaHorariaService {
    readonly zonaNavegador = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';

    readonly zona = signal(this.zonaNavegador);

    cambiarZona(nuevaZona: string) {
        this.zona.set(nuevaZona);
    }
}