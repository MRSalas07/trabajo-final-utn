import { Component, signal ,inject} from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  imports: [],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html'
})
export class App {
  protected readonly menuAbierto = signal(false);

  protected readonly categorias = [
    { ruta: '/categorias/F1', texto: 'Fórmula 1' },
    { ruta: '/categorias/WRC', texto: 'WRC (Rally)' },
    { ruta: '/categorias/TC', texto: 'TC (Turismo Carretera)' },
  ];

  protected readonly secciones = [
    { ruta: '/', texto: 'Inicio', icono: 'inicio' },
    { ruta: '/calendario', texto: 'Calendario', icono: 'calendario' },
    { ruta: '/estadisticas', texto: 'Estadísticas', icono: 'estadisticas' },
    { ruta: '/telemetria', texto: 'Telemetría', icono: 'telemetria' },
    { ruta: '/pilotos', texto: 'Pilotos', icono: 'pilotos' },
    { ruta: '/equipos', texto: 'Equipos', icono: 'equipos' },
  ];

  constructor() {
    // Al navegar, el menú del celular se cierra solo
    inject(Router)
      .events.pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.menuAbierto.set(false));
  }
}
