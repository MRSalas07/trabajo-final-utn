import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { EventoService } from '../../core/services/evento.service';
import { ZonaHorariaService } from '../../core/services/zona-horaria.service';
import { EventoDto, TablaEtapa, TablaSesion } from '../../core/modelos';
import { RouterLink } from '@angular/router';
import { EstadoCarga } from '../../shared/estado-carga/estado-carga';
import { EstadoPipe, HoraPipe, HoraSedePipe, TiempoPipe } from '../../shared/pipe/formato';
import { ResultadoService } from '../../core/services/resultados.service';
import { mensajeError } from '../../core/errores/error';
import { compatForm } from '@angular/forms/signals/compat';

@Component({
  imports: [RouterLink,EstadoCarga,HoraPipe,HoraSedePipe,EstadoPipe,TiempoPipe],
  selector: 'app-evento',
  styleUrl: './evento.css',
  templateUrl: './evento.html',
})
export class Evento {

  private readonly resultadoService = inject(ResultadoService);
  // protected: la plantilla lo usa para armar el enlace de descarga del .ics
  protected readonly eventoService = inject(EventoService);
  private readonly zonaService = inject(ZonaHorariaService);

  readonly id = input.required<string>();

  protected readonly evento = signal<EventoDto | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly tablaSesion = signal<TablaSesion | null>(null);
  protected readonly tablaEtapa = signal<TablaEtapa | null>(null);
  protected readonly cargandoTabla = signal(false);
  protected readonly errorTabla = signal<string | null>(null);


  protected readonly seleccion = signal<{ tipo: 'sesion' | 'etapa'; id: number } | null>(null);

  protected readonly conPuntos = computed(() => {
    const t = this.tablaSesion();
    return !!t && t.resultados.some((r) => (r.puntos ?? 0) > 0);
});

  constructor() {
    effect(() => {
      const id = Number(this.id());
      this.zonaService.zona();
      untracked(() =>
        this.cargarResultados(id) 
      );
    });
  }

  private cargarResultados(id: number): void {
    this.resultadoService.evento(id).subscribe({
      next: (e) => {
        this.evento.set(e);
        // Por defecto se muestra la carrera (o la clasificación general del rally) si tiene resultados
        if (!this.seleccion()) {
          const carrera = e.sesiones.find((s) => s.tipo === 'CARRERA' && s.cantidadResultados > 0);
          const general = e.etapas.find((t) => t.clasificacionGeneral && t.cantidadResultados > 0);
          if (carrera) this.verSesion(carrera.id, false);
          else if (general) this.verEtapa(general.id, false);
        }
      },
      error: (e) => this.error.set(mensajeError(e)),
    });
  }



  protected verSesion(id: number, desplazar = true): void {
    this.seleccion.set({ tipo: 'sesion', id });
    this.tablaEtapa.set(null);
    this.pedir(this.resultadoService.resultadosSesion(id), (t) => this.tablaSesion.set(t), desplazar);
  }


  protected verEtapa(id: number, desplazar = true): void {
    this.seleccion.set({ tipo: 'etapa', id });
    this.tablaSesion.set(null);
    this.pedir(this.resultadoService.resultadosEtapa(id), (t) => this.tablaEtapa.set(t), desplazar);
  }


  private pedir<T>(obs: import('rxjs').Observable<T>, ok: (t: T) => void, desplazar: boolean): void {
    this.cargandoTabla.set(true);
    this.errorTabla.set(null);
    obs.subscribe({
      next: (t) => {
        ok(t);
        this.cargandoTabla.set(false);
        if (desplazar) setTimeout(() => document.getElementById('resultados')?.scrollIntoView({ behavior: 'smooth' }));
      },
      error: (e) => {
        this.errorTabla.set(mensajeError(e));
        this.cargandoTabla.set(false);
      },
    });
  }


}
