import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', title: 'Motorsport Analytics', loadComponent: () => import('./pages/inicio/inicio').then((m) => m.Inicio) },
  { path: '**', redirectTo: '' },
];
