import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EstadoCarga } from '../../shared/estado-carga/estado-carga';
import { PilotoService } from '../../core/services/piloto.service';
import { Pagina, PilotoResumen } from '../../core/modelos';
import { mensajeError } from '../../core/errores/error';
import { debounceTime, Subject } from 'rxjs';
import { NumberSymbol } from '@angular/common';

export interface Filtros{ 
  filPiloto : string | undefined,
  filtroCategoria:string | undefined,
  filtroAnio : number | null,
  paginado : number | undefined,
  cantidad : number | undefined

}
@Component({
  imports: [FormsModule, RouterLink,EstadoCarga],
  selector: 'app-pilotos',
  styleUrl: './pilotos.css',
  templateUrl: './pilotos.html',
})
export class Pilotos {
  private readonly pilotoService = inject(PilotoService);
  protected readonly buscadorPiloto = signal('');
  
  protected readonly filtroAnio = signal<number | null>(null);
  protected readonly filtroCategoria = signal('');
  readonly categoria = input<string>();
  readonly anio = input<string>();

  protected readonly resultado = signal<Pagina<PilotoResumen> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly errorMensaje =signal<string | null>(null);
  protected readonly pagina = signal(0);
  protected readonly buscador = new Subject<string>();

  constructor(){
    this.buscador.pipe(debounceTime(300)).subscribe((inputTexto) => {
      this.buscadorPiloto.set(inputTexto);
      this.pagina.set(0);
    });

    effect(() => {
      const categoriaFiltro = this.categoria();
      const anioSelect = this.anio();
      untracked(() => {
        if (categoriaFiltro) this.filtroCategoria.set(categoriaFiltro);
        if (anioSelect) this.filtroAnio.set(Number(anioSelect));
      });
    });

    effect (() => {
      const filtros : Filtros = { 
        filPiloto: this.buscadorPiloto(), 
        filtroCategoria: this.categoria(), 
        filtroAnio: this.filtroAnio(), 
        paginado: this.pagina(), 
        cantidad: 24 };
      untracked(() => {
        this.cargando.set(true);
        this.errorMensaje.set(null);
        this.pilotoService.buscarPilotos(filtros).subscribe({
          next: (r) => {
            this.resultado.set(r);
            this.cargando.set(false);
          },
          error: (e) => {
            this.errorMensaje.set(mensajeError(e));
            this.cargando.set(false);
          },
        });
      });

    })
  }


}
