import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EstadoCarga } from '../../../shared/estado-carga/estado-carga';
import { EventoDto, FilaCampeonatoEquipo, FilaCampeonatoPiloto, PerfilCategoria, Temporada } from '../../../core/modelos';
import { SaludServices } from '../../../core/services/salud.service';
import { mensajeError } from '../../../core/errores/error';
import { EstadoPipe, HoraPipe } from '../../../shared/pipe/formato';
import { EventoService } from '../../../core/services/evento.service';
import { EstadisticaService } from '../../../core/services/estadistica.service';
import { ZonaHorariaService } from '../../../core/services/zona-horaria.service';

type Pestana = 'calendario' | 'pilotos' | 'equipos';

@Component({
  imports: [EstadoCarga, RouterLink, HoraPipe, EstadoPipe],
  selector: 'app-categoria-indivual',
  styleUrl: './categoria-indivual.css',
  templateUrl: './categoria-indivual.html',
})
export class CategoriaIndivual {
  private readonly eventoService = inject(EventoService);
  private readonly estadisticaService = inject(EstadisticaService);
  private readonly zonaService = inject(ZonaHorariaService);
  private readonly serviceSalud = inject(SaludServices);

  readonly siglas = input.required<string>();

  // Ficha de la categoría (MVP 4): temporada actual, líderes, último y próximo evento
  protected readonly perfil = signal<PerfilCategoria | null>(null);
  protected readonly errorPerfil = signal<string | null>(null);
  protected readonly temporadas = signal<Temporada[]>([]);
  protected readonly anio = signal<number | null>(null);

  protected readonly pestana = signal<Pestana>('calendario');

  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly eventos = signal<EventoDto[]>([]);

  protected readonly cargandoCampeonato = signal(false);
  protected readonly errorCampeonato = signal<string | null>(null);
  protected readonly pilotos = signal<FilaCampeonatoPiloto[]>([]);
  protected readonly equipos = signal<FilaCampeonatoEquipo[]>([]);

  /** Columnas que dependen de la categoría: poles solo en F1, tramos ganados solo en WRC. */
  protected readonly conPoles = computed(() => this.siglas().toUpperCase() === 'F1');
  protected readonly conTramos = computed(() => this.siglas().toUpperCase() === 'WRC');

  /** Ganador del último evento disputado de la temporada actual. */
  protected readonly ultimoGanador = computed(() => {
    const ganadores = this.perfil()?.resumen?.ganadores ?? [];
    return ganadores.length ? ganadores[ganadores.length - 1] : null;
  });

  constructor() {
    // Al cambiar de categoría (o de zona horaria): ficha y temporadas
    effect(() => {
      const siglas = this.siglas();
      this.zonaService.zona(); // las fechas del próximo/último evento dependen de la zona
      untracked(() => {
        this.errorPerfil.set(null);
        this.serviceSalud.perfil(siglas).subscribe({
          next: (p) => {
            this.perfil.set(p);
            this.temporadas.set(p.temporadas);
            const actual = this.anio();
            // Se arranca en la temporada actual que informa el backend (no en el año del sistema)
            if (actual === null || !p.temporadas.some((t) => t.anio === actual)) {
              this.anio.set(p.temporadaActual?.anio ?? p.temporadas[0]?.anio ?? null);
            }
            if (!p.temporadas.length) this.cargando.set(false);
          },
          error: (e) => {
            this.errorPerfil.set(mensajeError(e));
            this.cargando.set(false);
          },
        });
      });
    });

    // Calendario de la temporada elegida
    effect(() => {
      const siglas = this.siglas();
      const anio = this.anio();
      this.zonaService.zona();
      if (anio === null) return;
      untracked(() => {
        this.cargando.set(true);
        this.error.set(null);
        this.eventoService.eventosDeTemporada(siglas, anio).subscribe({
          next: (r) => { this.eventos.set(r); this.cargando.set(false); },
          error: (e) => { this.error.set(mensajeError(e)); this.cargando.set(false); },
        });
      });
    });

    // Campeonatos de la temporada elegida (los calcula el backend)
    effect(() => {
      const siglas = this.siglas();
      const anio = this.anio();
      if (anio === null) return;
      untracked(() => this.cargarCampeonatos(siglas, anio));
    });
  }

  protected cambiarAnio(valor: string): void {
    this.anio.set(Number(valor));
  }

  private cargarCampeonatos(siglas: string, anio: number): void {
    this.cargandoCampeonato.set(true);
    this.errorCampeonato.set(null);
    let pendientes = 2;
    const terminar = () => { if (--pendientes === 0) this.cargandoCampeonato.set(false); };
    this.estadisticaService.campeonatoPilotos(siglas, anio).subscribe({
      next: (filas) => { this.pilotos.set(filas); terminar(); },
      error: (e) => { this.pilotos.set([]); this.errorCampeonato.set(mensajeError(e)); terminar(); },
    });
    this.estadisticaService.campeonatoEquipos(siglas, anio).subscribe({
      next: (filas) => { this.equipos.set(filas); terminar(); },
      error: (e) => { this.equipos.set([]); this.errorCampeonato.set(mensajeError(e)); terminar(); },
    });
  }
}
