import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { EstadoCarga } from '../../shared/estado-carga/estado-carga';
import {Categoria } from '../../core/modelos';
import { SaludServices } from '../../core/services/salud.service';
import {  mensajeError } from '../../core/errores/error';

@Component({
  imports: [EstadoCarga, RouterLink],
  selector: 'app-categoria',
  styleUrl: './categorias.css',
  templateUrl: './categorias.html',
})
export class Categorias {

  private readonly serviceSalud = inject(SaludServices);

  protected readonly cargando = signal(true);

  protected readonly error = signal<string | null>(null);
  
  protected readonly categorias = signal<Categoria[]>([]);

  constructor() {
    this.serviceSalud.categorias().subscribe({
      next: (categoriasApi) => {
        this.categorias.set(categoriasApi);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(mensajeError(err));
        this.cargando.set(false);
      }
    });
  }

}
