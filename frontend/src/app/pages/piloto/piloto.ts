import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EstadoCarga } from '../../shared/estado-carga/estado-carga';
import { EstadoPipe, HoraPipe } from '../../shared/pipe/formato';
import { PilotoService } from '../../core/services/piloto.service';
import { PerfilPiloto } from '../../core/modelos';
import { mensajeError } from '../../core/errores/error';

@Component({
  imports: [RouterLink, EstadoCarga, HoraPipe, EstadoPipe],
  selector: 'app-piloto',
  styleUrl: './piloto.css',
  templateUrl: './piloto.html',
})
export class Piloto {
  private readonly pilotoService = inject(PilotoService);
  readonly id = input.required<string>();
  private readonly perfil = signal<PerfilPiloto | null>(null);
  protected readonly errorMensaje = signal<string | null>(null);

  constructor() {
    effect(() => {
      const id = Number(this.id());
      untracked(() => {
        this.perfil.set(null);
        this.pilotoService.buscarPilotoId(id).subscribe({
          next: (piloto)=> {
            this.perfil.set(piloto);
          },
          error: (error) =>{
            this.errorMensaje.set(mensajeError(error));
          }
        });
      });
    });
  }
}
