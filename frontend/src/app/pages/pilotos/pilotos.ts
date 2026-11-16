import { Component, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EstadoCarga } from '../../shared/estado-carga/estado-carga';
import { PilotoService } from '../../core/services/piloto.service';
import { Pagina, PilotoResumen } from '../../core/modelos';
import { mensajeError } from '../../core/errores/error';

@Component({
  imports: [FormsModule, RouterLink,EstadoCarga],
  selector: 'app-pilotos',
  styleUrl: './pilotos.css',
  templateUrl: './pilotos.html',
})
export class Piloto {
  private readonly pilotoService = inject(PilotoService);
  protected readonly buscadorTexto = signal('');
  protected readonly resultado = signal<Pagina<PilotoResumen> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly errorMensaje =signal<string | null>(null);

  constructor(){
    effect(() =>{
      const filtro = this.buscadorTexto();
      untracked (() =>{
        this.cargando.set(true);
        this.pilotoService.buscarPilotos({q: filtro}).subscribe( {
          next: (pilotos) => {this.resultado.set(pilotos);
            //this.cargando.set(false);
          },
          error: (error) => {
            this.errorMensaje.set(mensajeError(error));
            //this.cargando.set(false);
          },
          complete: () => {
            this.cargando.set(false);
          }
        })
      })
    })
  }
}
