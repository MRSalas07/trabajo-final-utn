import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Solo usuarios con sesión iniciada. */
export const authGuard: CanActivateFn = () => (
    inject(AuthService).auth() ? true : inject(Router).createUrlTree(['/login'])
);
