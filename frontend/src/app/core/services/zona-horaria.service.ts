import { Injectable, signal, computed } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class ZonaHorariaService {
    readonly zonaNavegador = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';

    readonly zona = signal(this.zonaNavegador);

    readonly zonasDisponibles = computed(() => Array.from(new Set([this.zona(), ...this.zonaNavegador])));


    cambiarZona(nuevaZona: string) {
        this.zona.set(nuevaZona);
    }
}