import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { Router } from '@angular/router';
import { SesionTelemetria } from '../../core/modelos';
import { SaludServices } from '../../core/services/salud.service';
import { TelemetriaService } from '../../core/services/telemetria.service';
import { ZonaHorariaService } from '../../core/services/zona-horaria.service';
import { mensajeError } from '../../core/errores/error';
import { EstadoCarga } from '../../shared/estado-carga/estado-carga';
import { HoraPipe } from '../../shared/pipe/formato';
import { AnalisisSesion } from './analisis-sesion/analisis-sesion';
import { PanelVivo } from './panel-vivo/panel-vivo';
import { Repeticion } from './repeticion/repeticion';
import { PRIMER_ANIO_OPENF1, agruparSesiones, lugarSesion, nombreSesion, sesionPorDefecto } from './sesiones';
import { Transmision } from './transmision/transmision';

type Vista = 'analisis' | 'repeticion';

/**
 * Telemetría F1 (MVP 7, módulo plus con datos de OpenF1).
 * Primero consulta /api/salud: si el backend no tiene la telemetría habilitada, muestra el aviso y no
 * hace más pedidos. Si la tiene, muestra si hay una sesión en curso (para seguirla en vivo) y, para las
 * sesiones terminadas, se elige temporada y sesión y se ve su análisis o su repetición.
 */
@Component({
  imports: [EstadoCarga, HoraPipe, AnalisisSesion, Repeticion, PanelVivo, Transmision],
  selector: 'app-telemetria',
  styleUrl: './telemetria.css',
  templateUrl: './telemetria.html',
})
export class Telemetria {
  private readonly saludService = inject(SaludServices);
  private readonly telemetriaService = inject(TelemetriaService);
  private readonly router = inject(Router);
  protected readonly zona = inject(ZonaHorariaService).zona;

  // Query param (withComponentInputBinding): /telemetria?vivo=1 abre la transmisión en vivo (enlace de la portada)
  readonly vivo = input<string>();
  /** Se está mirando la sesión en curso: la transmisión en vivo reemplaza a la vista de sesiones. */
  protected readonly enVivo = signal(false);
  protected readonly urlVivo = this.telemetriaService.urlVivo();

  /** null mientras se consulta /api/salud. */
  protected readonly habilitada = signal<boolean | null>(null);
  protected readonly errorSalud = signal<string | null>(null);

  // Año en UTC, como el backend (que rechaza los años posteriores al actual)
  private readonly anioActual = new Date().getUTCFullYear();
  protected readonly anios = Array.from(
    { length: Math.max(1, this.anioActual - PRIMER_ANIO_OPENF1 + 1) },
    (_, i) => this.anioActual - i,
  );
  protected readonly anio = signal(this.anioActual);

  protected readonly sesiones = signal<SesionTelemetria[]>([]);
  protected readonly cargandoSesiones = signal(false);
  protected readonly errorSesiones = signal<string | null>(null);
  private readonly elegida = signal<number | null>(null);
  protected readonly vista = signal<Vista>('analisis');

  protected readonly grupos = computed(() => agruparSesiones(this.sesiones(), this.zona()));
  protected readonly sesion = computed<SesionTelemetria | null>(() => {
    const lista = this.sesiones();
    const elegida = this.elegida();
    return lista.find((s) => s.sessionKey === elegida) ?? sesionPorDefecto(lista);
  });
  protected readonly nombre = computed(() => {
    const s = this.sesion();
    return s ? nombreSesion(s) : '';
  });
  protected readonly lugar = computed(() => {
    const s = this.sesion();
    return s ? lugarSesion(s) : '';
  });

  private pedidoSesiones = 0;
  /** Al entrar, si el año actual todavía no tiene sesiones (enero o febrero), se pasa una sola vez al anterior. */
  private probarAnioAnterior = true;

  constructor() {
    this.saludService.probarSalud().subscribe({
      next: (salud) => this.habilitada.set(salud.telemetria === true),
      error: (e) => this.errorSalud.set(mensajeError(e)),
    });

    // Sesiones del año elegido (solo con la telemetría habilitada)
    effect(() => {
      const anio = this.anio();
      if (this.habilitada() !== true) return;
      untracked(() => this.cargarSesiones(anio));
    });

    // Desde la portada: abre el vivo apenas se confirma que la telemetría está habilitada
    effect(() => {
      if (this.vivo() && this.habilitada() === true) untracked(() => this.enVivo.set(true));
    });
  }

  protected verVivo(): void {
    this.enVivo.set(true);
  }

  protected salirVivo(): void {
    this.enVivo.set(false);
    // Sin ?vivo=1 en la dirección: si recarga la página, no vuelve a abrir el vivo
    if (this.vivo()) this.router.navigate(['/telemetria'], { replaceUrl: true });
  }

  protected elegirAnio(valor: string): void {
    this.probarAnioAnterior = false;
    this.anio.set(Number(valor));
  }

  protected elegirSesion(valor: string): void {
    this.elegida.set(Number(valor));
  }

  private cargarSesiones(anio: number): void {
    const pedido = ++this.pedidoSesiones;
    this.sesiones.set([]);
    this.cargandoSesiones.set(true);
    this.errorSesiones.set(null);
    this.telemetriaService.sesiones(anio).subscribe({
      next: (lista) => {
        if (pedido !== this.pedidoSesiones) return;
        this.cargandoSesiones.set(false);
        if (!lista.length && this.probarAnioAnterior && anio > PRIMER_ANIO_OPENF1) {
          this.probarAnioAnterior = false;
          this.anio.set(anio - 1);
          return;
        }
        this.probarAnioAnterior = false;
        this.sesiones.set(lista);
      },
      error: (e) => {
        if (pedido !== this.pedidoSesiones) return;
        this.cargandoSesiones.set(false);
        this.errorSesiones.set(mensajeError(e));
      },
    });
  }
}
