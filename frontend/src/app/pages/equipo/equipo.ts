import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EstadoCarga } from '../../shared/estado-carga/estado-carga';
import { EstadoPipe, HoraPipe } from '../../shared/pipe/formato';
import { EquipoService } from '../../core/services/equipo.service';
import { Participacion, PerfilEquipo } from '../../core/modelos';
import { mensajeError } from '../../core/errores/error';


@Component({
  imports: [RouterLink,EstadoCarga,HoraPipe,EstadoPipe],
  selector: 'app-equipo',
  styleUrl: './equipo.css',
  templateUrl: './equipo.html',
})
export class Equipo {
  private readonly equipoService = inject(EquipoService);
  readonly id = input.required<string>();
  protected readonly perfil = signal<PerfilEquipo | null>(null);
  protected readonly error = signal<string | null>(null);


  protected readonly equipoPorAnio = computed(() => {
    const grupos = new Map<number, Participacion[]>();
    for (const m of this.perfil()?.plantel ?? []) {
      if (!grupos.has(m.anio)) grupos.set(m.anio, []);
      grupos.get(m.anio)!.push(m);
    }
    return [...grupos.entries()].sort((a, b) => b[0] - a[0]).slice(0, 5).map(([anio, miembros]) => ({ anio, miembros }));
  });

  constructor() {
    effect(() => {
      const id = Number(this.id());
      untracked(() => {
        this.perfil.set(null);
        this.equipoService.buscarEquipoId(id).subscribe({ 
          next: (response) => {
            this.perfil.set(response)
          }, 
          error: (error) => {
            this.error.set(mensajeError(error))
          } 
        });
      });
    });
  }


}
