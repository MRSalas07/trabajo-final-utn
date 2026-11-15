import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { EventoService } from '../../core/services/evento.service';
import { ZonaHorariaService } from '../../core/services/zona-horaria.service';
import { EventoDto, TablaEtapa, TablaSesion } from '../../core/modelos';
import { RouterLink } from '@angular/router';
import { EstadoCarga } from '../../shared/estado-carga/estado-carga';
import { EstadoPipe, HoraPipe, HoraSedePipe, TiempoPipe } from '../../shared/pipe/formato';
import { ResultadoService } from '../../core/services/resultados.service';
import { mensajeError } from '../../core/errores/error';

@Component({
  imports: [RouterLink,EstadoCarga,HoraPipe,HoraSedePipe,EstadoPipe,TiempoPipe],
  selector: 'app-evento',
  styleUrl: './evento.css',
  templateUrl: './evento.html',
})
export class Evento {
  private readonly resultadoService = inject(ResultadoService);
  private readonly eventoService = inject(EventoService);
  private readonly zonaService = inject(ZonaHorariaService);

  readonly id = input.required<string>();

  protected readonly evento = signal<EventoDto | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly tablaSesion = signal<TablaSesion | null>(null);
  protected readonly tablaEtapa = signal<TablaEtapa | null>(null);

  constructor() {
    effect(() => {
      const id = Number(this.id());
      this.zonaService.zona();
      untracked(() =>
        this.resultadoService.evento(id).subscribe({
          next: (e) => this.evento.set(e),
          error: (e) => this.error.set(mensajeError(e)),
        }),
      );
    });
  }


  protected verSesion(id: number): void {
    this.tablaEtapa.set(null);
    this.resultadoService.resultadosSesion(id).subscribe((t) => this.tablaSesion.set(t));
  }

  protected verEtapa(id: number): void {
    this.tablaSesion.set(null);
    this.resultadoService.resultadosEtapa(id).subscribe((t) => this.tablaEtapa.set(t));
  }


}
