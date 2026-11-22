import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EstadoCarga } from '../../shared/estado-carga/estado-carga';
import { EstadoPipe, HoraPipe } from '../../shared/pipe/formato';
import { CalendarioMes } from '../../shared/calendario-mes/calendario-mes';
import { CalendarioService } from '../../core/services/calendario.service';
import { ZonaHorariaService } from '../../core/services/zona-horaria.service';
import { EventoDto, Sesion } from '../../core/modelos';
import { diaLocal, diasDelEvento } from '../../shared/dias-evento';
import { mensajeError } from '../../core/errores/error';

const CATEGORIAS = ['F1', 'WRC', 'TC'];

interface EventoDelDia {
  evento: EventoDto;
  sesiones: Sesion[];
  tramos: number;
  primerDia: boolean;
}

interface DiaCalendario {
  clave: string;
  titulo: string;
  eventos: EventoDelDia[];
}

@Component({
  imports: [RouterLink, EstadoCarga, CalendarioMes,HoraPipe, EstadoPipe],
  selector: 'app-calendario',
  styleUrl: './calendario.css',
  templateUrl: './calendario.html',
})
export class Calendario {

  protected readonly calendarioService = inject(CalendarioService);
  protected readonly zonaService = inject(ZonaHorariaService);
  protected readonly categorias = CATEGORIAS;

  /** Primer día del mes mostrado (año, mes 0-11). */
  protected readonly mes = signal(this.mesActual());
  protected readonly anio = computed(() => this.mes().anio);
  protected readonly seleccionCategoria = signal<string[]>([...CATEGORIAS]);
  protected readonly eventos = signal<EventoDto[]>([]);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly hoy = computed(() =>
    new Intl.DateTimeFormat('en-CA', { timeZone: this.zonaService.zona(), year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date()),
  );

  protected readonly nombreMes = computed(() =>
    new Intl.DateTimeFormat('es-AR', { month: 'long', year: 'numeric' }).format(new Date(this.mes().anio, this.mes().mes, 1)),
  );

  protected readonly dias = computed(() => {
    const prefijo = `${this.mes().anio}-${String(this.mes().mes + 1).padStart(2, '0')}-`;
    const fmtTitulo = new Intl.DateTimeFormat('es-AR', { timeZone: 'UTC', weekday: 'long', day: 'numeric', month: 'long' });
    const grupos = new Map<string, DiaCalendario>();
    for (const evento of this.eventos()) {
      const diasEvento = diasDelEvento(evento);
      for (const clave of diasEvento.filter((dia) => dia.startsWith(prefijo))) {
        if (!grupos.has(clave)) grupos.set(clave, { clave, titulo: fmtTitulo.format(new Date(`${clave}T12:00:00Z`)), eventos: [] });
        grupos.get(clave)!.eventos.push({
          evento,
          sesiones: evento.sesiones.filter((sesion) => diaLocal(sesion.inicio) === clave),
          tramos: evento.etapas.filter((etapa) => !etapa.clasificacionGeneral && diaLocal(etapa.inicio) === clave).length,
          primerDia: clave === diasEvento[0],
        });
      }
    }
    return [...grupos.values()].sort((a, b) => a.clave.localeCompare(b.clave));
  });

  constructor() {
    // Se consulta de nuevo al cambiar el mes, las categorías o la zona horaria
    effect(() => {
      const mes = this.mes();
      const categorias = this.seleccionCategoria();
      this.zonaService.zona();
      untracked(() => this.cargar(mes.anio, mes.mes, categorias));
    });
  }

  private cargar(anio: number, mes: number, categorias : string[]): void {
    if (!categorias.length) {
      this.eventos.set([]);
      return;
    }
    const desde = `${anio}-${String(mes + 1).padStart(2, '0')}-01`;
    const ultimo = new Date(anio, mes + 1, 0).getDate();
    const hasta = `${anio}-${String(mes + 1).padStart(2, '0')}-${ultimo}`;
    this.cargando.set(true);
    this.error.set(null);
    this.calendarioService.calendario(desde, hasta, categorias).subscribe({
      next: (response) => {
        this.eventos.set(response);
        this.cargando.set(false);
      },
      error: (error) => {
        this.error.set(mensajeError(error));
        this.cargando.set(false);
      },
    });
  }

  protected moverMes(delta: number): void {
    const { anio, mes } = this.mes();
    const d = new Date(anio, mes + delta, 1);
    this.mes.set({ anio: d.getFullYear(), mes: d.getMonth() });
  }

  protected irAHoy(): void {
    this.mes.set(this.mesActual());
  }

  protected alternar(c: string): void {
    this.seleccionCategoria.update((s) => (s.includes(c) ? s.filter((x) => x !== c) : [...s, c]));
  }

  protected difCorta(min: number): string {
    const abs = Math.abs(min);
    const h = Math.floor(abs / 60);
    const m = abs % 60;
    return (min > 0 ? '+' : '−') + h + (m ? ':' + String(m).padStart(2, '0') : '') + ' h';
  }

  private mesActual() {
    const hoy = new Date();
    return { anio: hoy.getFullYear(), mes: hoy.getMonth() };
  }


}
