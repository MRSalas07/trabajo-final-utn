import { Component, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EstadoCarga } from '../../shared/estado-carga/estado-carga';
import { EquipoService } from '../../core/services/equipo.service';
import { EquipoResumen } from '../../core/modelos';
import { mensajeError } from '../../core/errores/error';

@Component({
  imports: [FormsModule, RouterLink, EstadoCarga],
  selector: 'app-equipos',
  styleUrl: './equipos.css',
  templateUrl: './equipos.html',
})
export class Equipos {
  private readonly equipoService = inject(EquipoService);
  protected readonly categoria = signal('F1');
  protected readonly anio = signal<number | null>(new Date().getFullYear());
  protected readonly equipos = signal<EquipoResumen[]>([]);
  protected readonly cargando = signal(false);
  protected readonly errorMensaje = signal<string | null>(null);

  constructor() {
    effect(() => {
      const categoriaSelec = this.categoria();
      const anioSelec = this.anio();
      untracked(() => {
        this.cargando.set(true);
        this.errorMensaje.set(null);
        this.equipoService.buscarEquipos(categoriaSelec, anioSelec).subscribe({
          next: (response) => {
            this.equipos.set(response);
            this.cargando.set(false);
          },
          error: (error) => {
            this.errorMensaje.set(mensajeError(error));
            this.cargando.set(false);
          },
        });
      });
    });
  }


}
