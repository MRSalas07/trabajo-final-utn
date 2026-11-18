import { Component,ChangeDetectionStrategy, input, output, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icono } from '../icono';
import { EventoDto } from '../../core/modelos';


interface Dia {
  numero: number;
  clave: string;
  eventos: EventoDto[];
  hoy: boolean;
}


@Component({
  imports: [RouterLink,Icono],
  selector: 'app-calendario-mes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './calendario-mes.css',
  templateUrl: './calendario-mes.html',
})
export class CalendarioMes {

  readonly anio = input.required<number>();
  readonly mes = input.required<number>();
  readonly eventos = input<EventoDto[]>([]);
  /** Fecha de hoy en la zona del usuario ("yyyy-MM-dd"), para marcarla. */
  readonly hoy = input<string>('');
  readonly mover = output<number>();

  protected readonly diasSemana = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

  protected readonly nombreMes = computed(() =>
    new Intl.DateTimeFormat('es-AR', { month: 'long', year: 'numeric' }).format(new Date(this.anio(), this.mes(), 1)),
  );

  /** Casilleros vacíos antes del día 1 (la semana empieza el lunes). */
  protected readonly huecos = computed(() => {
    const domingoCero = new Date(this.anio(), this.mes(), 1).getDay();
    return Array.from({ length: (domingoCero + 6) % 7 });
  });

  protected readonly dias = computed<Dia[]>(() => {
    const porDia = new Map<string, EventoDto[]>();
    for (const evento of this.eventos()) {
      const clave = evento.inicio?.local.substring(0, 10);
      if (clave) 
        porDia.set(clave, [...(porDia.get(clave) ?? []), evento]);
    }
    const cantidad = new Date(this.anio(), this.mes() + 1, 0).getDate();
    const prefijo = `${this.anio()}-${String(this.mes() + 1).padStart(2, '0')}-`;
    return Array.from({ length: cantidad }, (_, i) => {
      const clave = prefijo + String(i + 1).padStart(2, '0');
      return { numero: i + 1, clave, eventos: porDia.get(clave) ?? [], hoy: clave === this.hoy() };
    });
  });

  protected titulo(dia: Dia): string {
    return dia.eventos.map((evento) => 
      `${evento.categoria.siglas} · ${evento.nombreCorto ?? evento.nombre}`).join(' | ');
  }

}
