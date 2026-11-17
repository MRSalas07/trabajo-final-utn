import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { EventoService } from '../../core/services/evento.service';
import { ZonaHorariaService } from '../../core/services/zona-horaria.service';
import { EventoDto, ResultadoSesion } from '../../core/modelos';
import { ResultadoService } from '../../core/services/resultados.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { mensajeError } from '../../core/errores/error';
import { EstadoCarga } from '../../shared/estado-carga/estado-carga';
import { FilaEvento } from '../../shared/fila-evento/fila-evento';
import { EstadoPipe, HoraPipe, TiempoPipe } from '../../shared/pipe/formato';

@Component({
  imports: [EstadoCarga, FilaEvento, HoraPipe, EstadoPipe,TiempoPipe],
  selector: 'app-inicio',
  styleUrl: './inicio.css',
  templateUrl: './inicio.html',
})
export class Inicio {

  private readonly resultadoService = inject(ResultadoService);
  private readonly eventoService = inject(EventoService);
  private readonly zonaService = inject(ZonaHorariaService);

  protected readonly proximosEventos = signal<EventoDto[]>([]);
  protected readonly recientesEventos = signal<EventoDto[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly ahora = signal(Date.now());

  protected readonly ultimo = computed(() => this.recientesEventos()[0] ?? null);
  protected readonly tiempos = signal<ResultadoSesion[]>([]);

  constructor() {

    effect(() => {
      this.zonaService.zona();
      untracked(() => this.cargar())
    })

    effect(() => {
      const ultimo= this.ultimo();
      untracked(() => {
        const carrera = ultimo?.sesiones.find((s) => s.tipo === 'CARRERA' && s.cantidadResultados > 0);
        if (carrera) this.resultadoService.resultadosSesion(carrera.id).subscribe((t) => {
          this.tiempos.set(t.resultados.slice(0, 8));
        });
      });
    });
    interval(60_000).pipe(takeUntilDestroyed()).subscribe(() => {
      this.ahora.set(Date.now())
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
    this.eventoService.recientes(4).subscribe((response) => {
      this.recientesEventos.set(response)
    });
  }



}
