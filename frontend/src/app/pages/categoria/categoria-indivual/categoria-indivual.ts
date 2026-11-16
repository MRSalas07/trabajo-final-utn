import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { EstadoCarga } from '../../../shared/estado-carga/estado-carga';
import {Categoria, EventoDto, Temporada } from '../../../core/modelos';
import { SaludServices } from '../../../core/services/salud.service';
import {  mensajeError } from '../../../core/errores/error';
import { EstadoPipe, HoraPipe } from '../../../shared/pipe/formato';
import { EventoService } from '../../../core/services/evento.service';
import { ZonaHorariaService } from '../../../core/services/zona-horaria.service';

@Component({
  imports: [EstadoCarga, RouterLink,HoraPipe,EstadoPipe],
  selector: 'app-categoria-indivual',
  styleUrl: './categoria-indivual.css',
  templateUrl: './categoria-indivual.html',
})
export class CategoriaIndivual {
  
  private readonly eventoService = inject(EventoService);
  private readonly zonaService = inject(ZonaHorariaService);

  private readonly serviceSalud = inject(SaludServices);

  protected readonly cargando = signal(true);

  protected readonly error = signal<string | null>(null);
  
  protected readonly categorias = signal<Categoria[]>([]);

  

  readonly siglas = input.required<string>();

  protected readonly temporadas = signal<Temporada[]>([]);
  protected readonly anio = signal<number>(new Date().getFullYear());
  protected readonly eventos = signal<EventoDto[]>([]);


  

  constructor() {
    //Cuando se cambia de categoria
    effect(() => {
      const s = this.siglas();
      untracked(() =>
        this.eventoService.buscarTemporadas(s).subscribe({
          next: (t) => {
            this.temporadas.set(t);
            if (t.length && !t.some((x) => x.anio === this.anio())) this.anio.set(t[0].anio);
          },
          error: (e) => this.error.set(mensajeError(e)),
        }),
      );
    });

    effect(() => {
      const s = this.siglas();
      const anio = this.anio();
      this.zonaService.zona(); // solo para "suscribirse" a la zona
      untracked(() => {
        this.cargando.set(true);
        this.error.set(null);
        this.eventoService.eventosDeTemporada(s, anio).subscribe({
          next: (r) => { this.eventos.set(r); this.cargando.set(false); },
          error: (e) => { this.error.set(mensajeError(e)); this.cargando.set(false); },
        });
      });
    });


  }

}

