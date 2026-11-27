import { HttpErrorResponse, HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { AuthService } from "../services/auth.service";
import { Router } from "@angular/router";
import { environment } from "../../../environments/environment";
import { catchError, throwError } from "rxjs";

export const authInterceptor : HttpInterceptorFn = (req, next) => {
    const router = inject(Router);
    const auth = inject(AuthService);
    const token = auth.token;
    const apiBackend = req.url.startsWith(environment.apiUrl);

    const peticion = token && apiBackend ? req.clone({
        setHeaders: {Authorization: `Bearer ${token}`} 
    }) : req;

    return next(peticion).pipe (
        catchError((error : HttpErrorResponse) => {
            if (error.status === 401 && token && apiBackend){
                auth.cerrarSesion();
                router.navigate(['/login'], {queryParams: {expirada : 1}});
            }
            return throwError(() => error);
        }),
    );
}