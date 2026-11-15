import { Component, signal ,inject} from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Icono } from './shared/icono';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icono],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
  host: { '(document:keydown.escape)': 'menuAbierto.set(false)' },
})
export class App {
  protected readonly menuAbierto = signal(false);

  protected readonly secciones = [{ ruta: '/', texto: 'Inicio', icono: 'inicio' }];


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
