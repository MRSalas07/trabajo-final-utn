import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EstadoCarga } from '../../shared/estado-carga/estado-carga';
import { EstadoPipe, HoraPipe } from '../../shared/pipe/formato';
import { CalendarioMes } from '../../shared/calendario-mes/calendario-mes';
import { CalendarioService } from '../../core/services/calendario.service';
import { ZonaHorariaService } from '../../core/services/zona-horaria.service';
import { EventoDto } from '../../core/modelos';
import { mensajeError } from '../../core/errores/error';

const CATEGORIAS = ['F1', 'WRC', 'TC'];

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

  /** Agrupa los eventos por día LOCAL del usuario (según la hora de inicio convertida). */
  protected readonly dias = computed(() => {
    const zona = this.zonaService.zona();
    const grupos = new Map<string, { clave: string; titulo: string; eventos: EventoDto[] }>();
    const fmtClave = new Intl.DateTimeFormat('en-CA', { timeZone: zona, year: 'numeric', month: '2-digit', day: '2-digit' });
    const fmtTitulo = new Intl.DateTimeFormat('es-AR', { timeZone: zona, weekday: 'long', day: 'numeric', month: 'long' });
    for (const evento of this.eventos()) {
      if (!evento.inicio) continue;
      const fecha = new Date(evento.inicio.utc);
      const clave = fmtClave.format(fecha);
      if (!grupos.has(clave)) grupos.set(clave, { clave, titulo: fmtTitulo.format(fecha), eventos: [] });
      grupos.get(clave)!.eventos.push(evento);
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

  protected tramos(evento: EventoDto): number {
    return evento.etapas.filter((x) => !x.clasificacionGeneral).length;
  }


  private mesActual() {
    const hoy = new Date();
    return { anio: hoy.getFullYear(), mes: hoy.getMonth() };
  }


}
