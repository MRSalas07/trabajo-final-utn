import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { SesionTelemetria } from '../../../core/modelos';
import { TelemetriaService } from '../../../core/services/telemetria.service';
import { datosLiberados } from '../sesiones';
import { Transmision, VELOCIDADES } from '../transmision/transmision';

type Desde = 'inicio' | 'minuto' | 'vuelta';

/**
 * Repetición de una sesión terminada (MVP 7): el espectador elige velocidad y desde dónde empezar, y la
 * sesión se reproduce como si fuera en vivo con los datos históricos de OpenF1 (gratuitos).
 */
@Component({
  imports: [Transmision],
  selector: 'app-repeticion',
  styleUrl: './repeticion.css',
  templateUrl: './repeticion.html',
})
export class Repeticion {
  private readonly telemetriaService = inject(TelemetriaService);

  readonly sesion = input.required<SesionTelemetria>();

  protected readonly velocidades = VELOCIDADES;
  protected readonly velocidad = signal(1);
  protected readonly desde = signal<Desde>('inicio');
  protected readonly minuto = signal(0);
  protected readonly vuelta = signal(1);
  /** OpenF1 libera los datos media hora después del final: antes, el backend no puede repetirla. */
  protected readonly liberada = computed(() => datosLiberados(this.sesion()));

  /** Repetición en curso; si se elige otra sesión, se corta (la transmisión se cierra al desaparecer). */
  private readonly iniciada = signal<{ sessionKey: number; url: string } | null>(null);
  private readonly sessionKey = computed(() => this.sesion().sessionKey);
  protected readonly url = computed(() => {
    const iniciada = this.iniciada();
    return iniciada && iniciada.sessionKey === this.sessionKey() ? iniciada.url : null;
  });

  constructor() {
    // Al cambiar de sesión se olvida la repetición anterior: si el usuario vuelve, ve el panel de inicio
    // (y no una repetición nueva que arranca sola desde el principio)
    effect(() => {
      const sessionKey = this.sessionKey();
      untracked(() => {
        if (this.iniciada()?.sessionKey !== sessionKey) this.iniciada.set(null);
      });
    });
  }

  protected elegirDesde(valor: string): void {
    this.desde.set(valor === 'minuto' || valor === 'vuelta' ? valor : 'inicio');
  }

  protected elegirVelocidad(valor: string): void {
    this.velocidad.set(Number(valor));
  }

  protected elegirMinuto(valor: string): void {
    this.minuto.set(entero(valor, 0));
  }

  protected elegirVuelta(valor: string): void {
    this.vuelta.set(entero(valor, 1));
  }

  protected iniciar(): void {
    const sesion = this.sesion();
    const desde = this.desde();
    const url = this.telemetriaService.urlRepeticion(sesion.sessionKey, {
      velocidad: this.velocidad(),
      inicioMin: desde === 'minuto' ? this.minuto() : 0,
      inicioVuelta: desde === 'vuelta' ? this.vuelta() : null,
    });
    this.iniciada.set({ sessionKey: sesion.sessionKey, url });
  }

  protected detener(): void {
    this.iniciada.set(null);
  }
}

/** Número entero no menor que "minimo" (lo que no es número vale "minimo"). */
function entero(valor: string, minimo: number): number {
  const numero = Math.floor(Number(valor));
  return Number.isFinite(numero) ? Math.max(minimo, numero) : minimo;
}
