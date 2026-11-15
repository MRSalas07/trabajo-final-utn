import {Pipe, PipeTransform} from '@angular/core'
import {Horario} from '../../core/modelos'

@Pipe({name : 'hora'})
export class HoraPipe implements PipeTransform {
    transform(hora : Horario |null |undefined): string {
        if(!hora) return '-';
        return new Intl.DateTimeFormat('es-AR', {
            weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', hour12: false, timeZone: h.zona,
            }).format(new Date(hora.utc));
    }
}

/** Texto legible para los estados normalizados del backend. */
@Pipe({ name: 'estado' })
export class EstadoPipe implements PipeTransform {

    private static readonly TEXTOS: Record<string, string> = {
        PROGRAMADO: 'Programado', EN_CURSO: 'En vivo', FINALIZADO: 'Finalizado', CANCELADO: 'Cancelado',
    };
    transform(valor: string | null | undefined): string {
        return valor ? (EstadoPipe.TEXTOS[valor] ?? valor) : '—';
    }
}
