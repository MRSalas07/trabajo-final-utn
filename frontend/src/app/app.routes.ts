import { Routes } from '@angular/router';
import { authGuard } from './core/guards/guards';

export const routes: Routes = [
  { path: '', title: 'Motorsport Analytics', loadComponent: () => import('./pages/inicio/inicio').then((m) => m.Inicio) },
  { path: 'categorias', title: 'Categorías', loadComponent: () => import('./pages/categoria/categorias').then((m) => m.Categorias) },
  { path: 'login', title: 'Ingresar', loadComponent: () => import('./pages/login/login').then((m) => m.Login) },
  { path: 'cuenta', title: 'Mi cuenta', canActivate: [authGuard], loadComponent: () => import('./pages/perfil/perfil').then((m) => m.Perfil) },
  
  
  { path: '**', redirectTo: '' },

];
