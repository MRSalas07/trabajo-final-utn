import { Component, ChangeDetectionStrategy, input, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EventoDto } from '../../core/modelos';

const NOMBRE_CATEGORIA: Record<string, string> = {
  F1: 'Fórmula 1',
  WRC: 'WRC (Rally)',
  TC: 'Turismo Carretera',
};


@Component({
  imports: [RouterLink],
  selector: 'app-fila-evento',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './fila-evento.css',
  templateUrl: './fila-evento.html',
})
export class FilaEvento {
  readonly eventoEntrada = input.required<EventoDto>();
  readonly modo = input<'proximo' | 'reciente'>('proximo');
  readonly ahora = input<number>(Date.now());
  
  protected readonly categoria = computed(() => {
    const c = this.evento().categoria;
    return NOMBRE_CATEGORIA[c.siglas] ?? c.nombre;
  });

  /** Día y mes en la zona horaria con la que el backend convirtió el horario. */
  protected readonly dia = computed(() => {
    const horario = this.evento().inicio;
    if (!horario) return { numero: '—', mes: '' };
    const fecha = new Date(horario.utc);
    const fmt = (o: Intl.DateTimeFormatOptions) => {
      try {
        return new Intl.DateTimeFormat('es-AR', { ...o, timeZone: horario.zona }).format(fecha);
      } catch {
        return new Intl.DateTimeFormat('es-AR', o).format(fecha);
      }
    };
    return { numero: fmt({ day: 'numeric' }), mes: fmt({ month: 'short' }).replace('.', '') };
  });

  protected readonly calculaHorarioFaltante = computed(() => {
    const horario = this.evento().inicio;
    if (!horario) return '—';
    const ms = new Date(horario.utc).getTime() - this.ahora();
    if (ms <= 0) return null;
    const min = Math.floor(ms / 60000);
    if (min < 60) return `${min} min`;
    const horas = Math.floor(min / 60);
    if (horas < 48) return `${horas} h`;
    return `${Math.floor(horas / 24)} d`;
  });

}
