import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EventoService } from '../../core/services/evento.service';
import { ZonaHorariaService } from '../../core/services/zona-horaria.service';
import { EventoDto, FilaCampeonatoPiloto, ResultadoSesion } from '../../core/modelos';
import { ResultadoService } from '../../core/services/resultados.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { mensajeError } from '../../core/errores/error';
import { EstadoCarga } from '../../shared/estado-carga/estado-carga';
import { FilaEvento } from '../../shared/fila-evento/fila-evento';
import { HoraPipe, TiempoPipe } from '../../shared/pipe/formato';
import { CalendarioMes } from '../../shared/calendario-mes/calendario-mes';
import { SaludServices } from '../../core/services/salud.service';
import { EstadisticaService } from '../../core/services/estadistica.service';

@Component({
  imports: [RouterLink,EstadoCarga, FilaEvento, HoraPipe, TiempoPipe, CalendarioMes],
  selector: 'app-inicio',
  styleUrl: './inicio.css',
  templateUrl: './inicio.html',
})
export class Inicio {

  private readonly resultadoService = inject(ResultadoService);
  private readonly eventoService = inject(EventoService);
  private readonly zonaService = inject(ZonaHorariaService);
  private readonly serviceSalud = inject(SaludServices);
  private readonly estadisticaService = inject(EstadisticaService);

  protected readonly proximosEventos = signal<EventoDto[]>([]);
  protected readonly recientesEventos = signal<EventoDto[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly ahora = signal(Date.now());

  protected readonly ultimo = computed(() => this.recientesEventos()[0] ?? null);
  protected readonly tiempos = signal<ResultadoSesion[]>([]);
  protected readonly errorTiempos = signal<string | null>(null);
  protected readonly errorRecientes = signal<string | null>(null);

  protected readonly mes = signal(this.mesActual());
  protected readonly eventosMes = signal<EventoDto[]>([]);

  // Panel de campeonatos (MVP 4): top 5 de la temporada actual de cada categoría
  protected readonly categoriasCampeonato = ['F1', 'WRC', 'TC'] as const;
  protected readonly categoriaCampeonato = signal<string>('F1');
  protected readonly anioCampeonato = signal<number | null>(null);
  protected readonly topCampeonato = signal<FilaCampeonatoPiloto[]>([]);
  protected readonly cargandoCampeonato = signal(false);
  protected readonly errorCampeonato = signal<string | null>(null);
  private pedidoCampeonato = 0;

  /** Panel de telemetría F1 (MVP 7): solo si el backend tiene el módulo habilitado. */
  protected readonly telemetria = signal(false);

  protected readonly hoy = computed(() =>
    new Intl.DateTimeFormat('en-CA', { timeZone: this.zonaService.zona(), year: 'numeric', month: '2-digit', day: '2-digit' }).format(
      new Date(this.ahora()),
    ),
  );

  constructor() {

    effect(() => {
      this.zonaService.zona();
      untracked(() => this.cargar())
    })

    effect(() => {
      const ultimo= this.ultimo();
      untracked(() => {
        const carrera = ultimo?.sesiones.find((s) => s.tipo === 'CARRERA' && s.cantidadResultados > 0);
        if (carrera) this.resultadoService.resultadosSesion(carrera.id).subscribe({
          next: (t) => { this.tiempos.set(t.resultados.slice(0, 8)); this.errorTiempos.set(null); },
          error: (e) => { this.tiempos.set([]); this.errorTiempos.set(mensajeError(e)); },
        });
      });
    });
    effect(() => {
      const siglas = this.categoriaCampeonato();
      untracked(() => this.cargarCampeonato(siglas));
    });
    interval(60_000).pipe(takeUntilDestroyed()).subscribe(() => {
      this.ahora.set(Date.now())
    });
    this.serviceSalud.probarSalud().subscribe({
      next: (salud) => this.telemetria.set(salud.telemetria === true),
      error: () => this.telemetria.set(false),
    });
  }

  /** La temporada "actual" la decide el backend (perfil de la categoría); después se pide su campeonato. */
  private cargarCampeonato(siglas: string): void {
    const pedido = ++this.pedidoCampeonato;
    const vigente = () => pedido === this.pedidoCampeonato;
    this.cargandoCampeonato.set(true);
    this.errorCampeonato.set(null);
    this.topCampeonato.set([]);
    this.serviceSalud.perfil(siglas).subscribe({
      next: (perfil) => {
        if (!vigente()) return;
        const anio = perfil.temporadaActual?.anio ?? null;
        this.anioCampeonato.set(anio);
        if (anio === null) { this.cargandoCampeonato.set(false); return; }
        this.estadisticaService.campeonatoPilotos(siglas, anio).subscribe({
          next: (filas) => { if (vigente()) { this.topCampeonato.set(filas.slice(0, 5)); this.cargandoCampeonato.set(false); } },
          error: (e) => { if (vigente()) { this.errorCampeonato.set(mensajeError(e)); this.cargandoCampeonato.set(false); } },
        });
      },
      error: (e) => { if (vigente()) { this.errorCampeonato.set(mensajeError(e)); this.cargandoCampeonato.set(false); } },
    });
  }

    private cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.eventoService.proximos(4).subscribe({
      next: (response) => { 
        this.proximosEventos.set(response); this.cargando.set(false); 
      },
      error: (error) => { 
        this.error.set(mensajeError(error)); this.cargando.set(false); 
      },
    });
    this.eventoService.recientes(4).subscribe({
      next: (response) => { this.recientesEventos.set(response); this.errorRecientes.set(null); },
      error: (e) => this.errorRecientes.set(mensajeError(e)),
    });
  }

    protected moverMes(delta: number): void {
    const { anio, mes } = this.mes();
    const dia = new Date(anio, mes + delta, 1);
    this.mes.set({ anio: dia.getFullYear(), mes: dia.getMonth() });
  }

    private mesActual() {
    const hoy = new Date();
    return { anio: hoy.getFullYear(), mes: hoy.getMonth() };
  }

}
