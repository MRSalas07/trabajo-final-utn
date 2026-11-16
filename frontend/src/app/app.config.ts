import { ApplicationConfig,LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors  } from '@angular/common/http';
import { provideRouter, withInMemoryScrolling ,withComponentInputBinding } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import localeEsAr from '@angular/common/locales/es-AR';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptores/interceptores';

registerLocaleData(localeEsAr);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes,withComponentInputBinding(),withInMemoryScrolling({ scrollPositionRestoration: 'top' })),
    provideHttpClient(withInterceptors([authInterceptor])),
    { provide: LOCALE_ID, useValue: 'es-AR' },
  ]
};
