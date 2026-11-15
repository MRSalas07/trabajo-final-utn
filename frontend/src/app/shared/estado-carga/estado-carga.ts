import { Component, input } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-estado-carga',
  styleUrl: './estado-carga.css',
  templateUrl: './estado-carga.html',
})
export class EstadoCarga {
  
  readonly cargando = input(false);
  readonly error = input<string | null>(null);
  readonly vacio = input(false);
  readonly mensajeVacio = input('No hay datos para mostrar.');

}
