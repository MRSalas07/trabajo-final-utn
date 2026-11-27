import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/guards/guards';

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
  { path: 'estadisticas', title: 'Estadísticas', loadComponent: () => 
    import('./pages/estadisticas/estadisticas').then((m) => m.Estadisticas) 
  },
  { path: 'telemetria', title: 'Telemetría F1', loadComponent: () =>
    import('./pages/telemetria/telemetria').then((m) => m.Telemetria)
  },
  { path: 'admin', title: 'Administración', canActivate: [adminGuard], loadComponent: () => 
    import('./pages/admin/admin/admin').then((m) => m.Admin) },

  { path: '**', redirectTo: '' },

];
