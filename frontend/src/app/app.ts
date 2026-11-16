import { Component, signal ,inject} from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Icono } from './shared/icono';
import {ZonaHorariaService } from './core/services/zona-horaria.service'
import { NgTemplateOutlet } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from './core/services/auth.service';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icono, NgTemplateOutlet, FormsModule],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
  host: { '(document:keydown.escape)': 'menuAbierto.set(false)' },
})
export class App {
  protected readonly menuAbierto = signal(false);

  protected readonly secciones = [{ ruta: '/', texto: 'Inicio', icono: 'inicio' }];

  protected readonly zonas = inject(ZonaHorariaService)

  protected readonly authService = inject(AuthService)

  protected readonly categorias = [
  { ruta: '/categorias/F1', texto: 'Fórmula 1' },
  { ruta: '/categorias/WRC', texto: 'WRC (Rally)' },
  { ruta: '/categorias/TC', texto: 'TC (Turismo Carretera)' },
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
