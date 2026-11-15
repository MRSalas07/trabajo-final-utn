import { HttpErrorResponse } from '@angular/common/http';
import { ErrorApi } from '../modelos';

export function mensajeError(error: HttpErrorResponse): string {
    if (error instanceof HttpErrorResponse) {
        if (error.status === 0) {
            return 'No se pudo conectar con el servidor. Verifique su conexión o intente más tarde.';
        }
        const cuerpo = error.error as Partial<ErrorApi> | null;
        if (cuerpo && Array.isArray(cuerpo.detalles) && cuerpo.detalles.length) {
            return cuerpo.detalles.join(' · ');
        }
        if (cuerpo?.error) {
            return cuerpo.error;
        }
        return `Error ${error.status}`;
    }
    return 'Ocurrió un error inesperado.';

}