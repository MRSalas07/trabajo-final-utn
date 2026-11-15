import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', title: 'Motorsport Analytics', loadComponent: () => import('./pages/inicio/inicio').then((m) => m.Inicio) },
  { path: 'categorias', title: 'Categorías', loadComponent: () => import('./pages/categoria/categorias').then((m) => m.Categorias) },
  { path: 'login', title: 'Ingresar', loadComponent: () => import('./pages/login/login').then((m) => m.Login) },
  { path: '**', redirectTo: '' },

];
