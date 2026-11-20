import { Injectable, signal } from '@angular/core';

export interface Notificacion {
  id: number;
  tipo: 'exito' | 'error' | 'aviso';
  texto: string;
  enlace?: string;
}

/**
 * Notificaciones emergentes (toasts) de toda la aplicación.
 * Cualquier clase puede inyectarlo y llamar a exito(), error() o aviso();
 * el componente <app-notificaciones> (en app.ts) se encarga de dibujarlas.
 */
@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private siguienteId = 0;
  readonly lista = signal<Notificacion[]>([]);

  exito(texto: string, enlace?: string): void {
    this.mostrar('exito', texto, 6000, enlace);
  }

  error(texto: string, enlace?: string): void {
    this.mostrar('error', texto, 10000, enlace);
  }

  aviso(texto: string, enlace?: string): void {
    this.mostrar('aviso', texto, 6000, enlace);
  }

  cerrar(id: number): void {
    this.lista.update((l) => l.filter((n) => n.id !== id));
  }

  private mostrar(tipo: Notificacion['tipo'], texto: string, duracionMs: number, enlace?: string): void {
    const id = this.siguienteId++;
    this.lista.update((l) => [...l, { id, tipo, texto, enlace }]);
    setTimeout(() => this.cerrar(id), duracionMs);
  }
}
