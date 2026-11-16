import {Pipe, PipeTransform} from '@angular/core'
import {Horario} from '../../core/modelos'

@Pipe({name : 'hora'})
export class HoraPipe implements PipeTransform {
    transform(hora: Horario | string | null | undefined, 
        formato: 'corta' | 'fecha' | 'hora' | 'dia' | 'mes' = 'corta', zona?: string): string {
    if (!hora) return '—';
    const utc = typeof hora === 'string' ? hora : hora.utc;
    const timeZone = zona ?? (typeof hora === 'string' ? undefined : hora.zona);
    const fecha = new Date(utc);
    const opciones: Record<string, Intl.DateTimeFormatOptions> = {
      corta: { weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', hour12: false },
      fecha: { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' },
      hora: { hour: '2-digit', minute: '2-digit', hour12: false },
      dia: { weekday: 'short', day: 'numeric', month: 'short' },
      mes: { month: 'long', year: 'numeric' },
    };
    try {
        return new Intl.DateTimeFormat('es-AR', { ...opciones[formato], timeZone }).format(fecha).replace(',', ' ·');
    } catch {
        return fecha.toISOString();
    }
  }
}

/** Fecha local en texto "yyyy-MM-ddTHH:mm:ss" (hora de la sede) -> "16/03 15:00". */
@Pipe({ name: 'horaSede' })
export class HoraSedePipe implements PipeTransform {
  transform(local: string | null | undefined): string {
    if (!local) return '—';
    const [fecha, hora] = local.split('T');
    const [, mes, dia] = fecha.split('-');
    return `${dia}/${mes} ${hora.substring(0, 5)}`;
  }
}



/** Texto legible para los estados normalizados del backend. */
@Pipe({ name: 'estado' })
export class EstadoPipe implements PipeTransform {
    private static readonly TEXTOS: Record<string, string> = {
        PROGRAMADO: 'Programado',
        EN_CURSO: 'En vivo',
        FINALIZADO: 'Finalizado',
        CANCELADO: 'Cancelado',
        ABANDONO: 'Abandono',
        DESCALIFICADO: 'Descalificado',
        NO_LARGO: 'No largó',
        PRACTICA: 'Práctica',
        CLASIFICACION: 'Clasificación',
        SPRINT_CLASIFICACION: 'Clasif. Sprint',
        SPRINT: 'Sprint',
        SERIE: 'Serie',
        CARRERA: 'Carrera',
    };

    transform(valor: string | null | undefined): string {
        return valor ? (EstadoPipe.TEXTOS[valor] ?? valor) : '—';
    }
}


@Pipe({ name: 'tiempo' })
export class TiempoPipe implements PipeTransform {
    transform(segundos: number | null | undefined, diferencia = false): string {
        if (segundos === null || segundos === undefined) return '—';
        if (diferencia) {
            if (segundos === 0) return '—';
            return '+' + this.formatear(segundos);
        }
        return this.formatear(segundos);
    }

    private formatear(s: number): string {
        const h = Math.floor(s / 3600);
        const m = Math.floor((s % 3600) / 60);
        const seg = (s % 60).toFixed(3).padStart(6, '0');
        if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${seg}`;
        if (m > 0) return `${m}:${seg}`;
        return Number(seg).toFixed(3);
    }
}

