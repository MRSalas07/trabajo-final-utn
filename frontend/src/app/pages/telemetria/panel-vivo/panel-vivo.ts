import { Component, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { interval } from 'rxjs';
import { InfoVivo } from '../../../core/modelos';
import { TelemetriaService } from '../../../core/services/telemetria.service';
import { ZonaHorariaService } from '../../../core/services/zona-horaria.service';
import { mensajeError } from '../../../core/errores/error';
import { EstadoCarga } from '../../../shared/estado-carga/estado-carga';
import { HoraPipe } from '../../../shared/pipe/formato';
import { datosLiberados, lugarSesion, nombreSesion } from '../sesiones';

/** Cada cuánto se vuelve a consultar el estado (el backend guarda la respuesta un minuto). */
const REFRESCO_MS = 60_000;

/**
 * ¿Hay una sesión de F1 en curso? (MVP 7) Muestra el estado del modo en vivo y, si se puede ver, el acceso
 * a la transmisión. Lo usan la portada (el botón lleva a /telemetria?vivo=1) y la página de telemetría
 * (el botón avisa con el evento "ver" para abrir la transmisión ahí mismo).
 */
@Component({
  imports: [RouterLink, EstadoCarga, HoraPipe],
  selector: 'app-panel-vivo',
  styleUrl: './panel-vivo.css',
  templateUrl: './panel-vivo.html',
})
export class PanelVivo {
  private readonly telemetriaService = inject(TelemetriaService);
  protected readonly zona = inject(ZonaHorariaService).zona;

  readonly ubicacion = input<'portada' | 'telemetria'>('telemetria');
  readonly ver = output<void>();

  protected readonly info = signal<InfoVivo | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly sesion = computed(() => this.info()?.ultimaSesion ?? null);
  protected readonly titulo = computed(() => {
    const s = this.sesion();
    return s ? `${nombreSesion(s)} · ${lugarSesion(s)}` : '';
  });
  protected readonly enCurso = computed(() => this.info()?.enVentanaVivo === true);
  /** Se puede mirar en vivo: hay una sesión en curso y el backend tiene la suscripción de OpenF1. */
  protected readonly puedeVer = computed(() => this.enCurso() && this.info()?.credenciales === true);

  protected readonly etiqueta = computed(() => {
    const info = this.info();
    if (!info) return '';
    if (info.enVentanaVivo) return 'En vivo';
    const s = info.ultimaSesion;
    if (!s) return 'Sin datos';
    return s.inicio && Date.parse(s.inicio) > Date.now() ? 'Próxima sesión' : 'Última sesión';
  });

  protected readonly texto = computed(() => {
    const info = this.info();
    if (!info) return '';
    if (info.enVentanaVivo) {
      return info.credenciales
        ? 'Seguila en vivo: torre de tiempos, autos en el circuito, telemetría y dirección de carrera.'
        : 'El modo en vivo necesita la suscripción de OpenF1. La repetición se habilita media hora después del final.';
    }
    const s = info.ultimaSesion;
    if (!s) return info.mensaje; // OpenF1 no respondió
    if (s.inicio && Date.parse(s.inicio) > Date.now()) return 'No hay ninguna sesión en curso.';
    return datosLiberados(s)
      ? 'No hay ninguna sesión en curso. La última ya se puede analizar y ver en repetición.'
      : 'No hay ninguna sesión en curso. El análisis y la repetición de la última se habilitan media hora después del final.';
  });

  constructor() {
    this.cargar();
    interval(REFRESCO_MS).pipe(takeUntilDestroyed()).subscribe(() => this.cargar());
  }

  private cargar(): void {
    this.telemetriaService.infoVivo().subscribe({
      next: (info) => {
        this.info.set(info);
        this.error.set(null);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeError(e));
        this.cargando.set(false);
      },
    });
  }
}
