import { Routes } from '@angular/router';
import { authGuard } from './core/guards/guards';

export const routes: Routes = [
  { path: '', title: 'Motorsport Analytics', loadComponent: () => 
    import('./pages/inicio/inicio').then((m) => m.Inicio) 
  },
  { path: 'calendario', title: 'Calendario', loadComponent: () => 
    import('./pages/calendario/calendario').then((m) => m.Calendario) 
  },
  { path: 'categorias', title: 'Categorías', loadComponent: () => 
    import('./pages/categoria/categorias').then((m) => m.Categorias) 
  },
  { path: 'login', title: 'Ingresar', loadComponent: () => 
    import('./pages/login/login').then((m) => m.Login) 
  },
  { path: 'cuenta', title: 'Mi cuenta', canActivate: [authGuard], loadComponent: () => 
    import('./pages/perfil/perfil').then((m) => m.Perfil) 
  },  
  { path: 'categorias/:siglas', title: 'Categoría', loadComponent: () => 
    import('./pages/categoria/categoria-indivual/categoria-indivual').then((m) => m.CategoriaIndivual) 
  },
  { path: 'eventos/:id', title: 'Evento', loadComponent: () => 
    import('./pages/evento/evento').then((m) => m.Evento) 
  },
  { path: 'pilotos', title: 'Pilotos', loadComponent: () => 
    import('./pages/pilotos/pilotos').then((m) => m.Pilotos) 
  },
  { path: 'pilotos/:id', title: 'Piloto', loadComponent: () => 
    import('./pages/piloto/piloto').then((m) => m.Piloto) 
  },
  { path: 'equipos', title: 'Equipos', loadComponent: () => 
    import('./pages/equipos/equipos').then((m) => m.Equipos) 
  },
  { path: 'equipos/:id', title: 'Equipo', loadComponent: () => 
    import('./pages/equipo/equipo').then((m) => m.Equipo) 
  },

  { path: '**', redirectTo: '' },

];
