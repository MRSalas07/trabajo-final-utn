import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Solo usuarios con sesión iniciada. */
export const authGuard: CanActivateFn =(_route, state) =>{
    const auth = inject(AuthService);
    return auth.auth() ? true : inject(Router).createUrlTree(['/login'], 
        { queryParams: { volver: state.url } 
    });

}

export const adminGuard: CanActivateFn = () => {
    const auth = inject(AuthService);
    return auth.esAdmin() ? true : inject(Router).createUrlTree(['/']);
};

