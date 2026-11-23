import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ComparativaHistorica, EquipoResumen, FilaCampeonatoPiloto, Historico as HistoricoDto } from '../../../core/modelos';
import { EstadisticaService } from '../../../core/services/estadistica.service';
import { EquipoService } from '../../../core/services/equipo.service';
import { mensajeError } from '../../../core/errores/error';
import { EstadoCarga } from '../../../shared/estado-carga/estado-carga';
import { Grafico, SerieGrafico } from '../../../shared/grafico/grafico';
import { colorDe } from '../../../shared/colores';
import { BuscadorPiloto, PilotoElegido } from '../buscador-piloto/buscador-piloto';

type TipoHistorico = 'PILOTO' | 'EQUIPO';

/**
 * Pestaña "Histórico" de Estadísticas (MVP 6): la trayectoria temporada por temporada de un piloto
 * o de un equipo en la categoría, y la comparativa histórica entre dos pilotos.
 * No depende de la temporada elegida: recorre todas las temporadas cargadas.
 */
@Component({
  imports: [RouterLink, EstadoCarga, Grafico, BuscadorPiloto],
  selector: 'app-historico',
  styleUrl: './historico.css',
  templateUrl: './historico.html',
})
export class Historico {
  private readonly estadisticaService = inject(EstadisticaService);
  private readonly equipoService = inject(EquipoService);

  readonly siglas = input.required<string>();
  /** Campeonato de la temporada elegida en la página: da el piloto y el equipo por defecto. */
  readonly pilotosTemporada = input<FilaCampeonatoPiloto[]>([]);

  protected readonly conPoles = computed(() => this.siglas().toUpperCase() === 'F1');
  protected readonly tipo = signal<TipoHistorico>('PILOTO');

  // Elecciones del usuario: valen para la categoría en la que se hicieron
  private readonly eleccion = signal<{ siglas: string | null; piloto: PilotoElegido | null; equipoId: number | null; rival: PilotoElegido | null }>(
    { siglas: null, piloto: null, equipoId: null, rival: null });

  protected readonly piloto = computed<PilotoElegido | null>(() => {
    const e = this.eleccion();
    const lider = this.pilotosTemporada()[0];
    return (e.siglas === this.siglas() ? e.piloto : null) ?? (lider ? { id: lider.pilotoId, nombre: lider.piloto } : null);
  });
  protected readonly rival = computed<PilotoElegido | null>(() => {
    const e = this.eleccion();
    const segundo = this.pilotosTemporada()[1];
    return (e.siglas === this.siglas() ? e.rival : null) ?? (segundo ? { id: segundo.pilotoId, nombre: segundo.piloto } : null);
  });
  protected readonly equipoId = computed<number | null>(() => {
    const e = this.eleccion();
    return (e.siglas === this.siglas() ? e.equipoId : null) ?? this.pilotosTemporada()[0]?.equipoId ?? this.equipos()[0]?.id ?? null;
  });

  // Todos los equipos de la categoría (de cualquier temporada), ordenados por nombre
  protected readonly equipos = signal<EquipoResumen[]>([]);

  // ---------------------------------------------------------------- historial
  protected readonly historico = signal<HistoricoDto | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly etiquetasHistorico = computed(() => (this.historico()?.temporadas ?? []).map((t) => String(t.anio)));
  protected readonly seriesHistorico = computed<SerieGrafico[]>(() => {
    const h = this.historico();
    if (!h) return [];
    return [{ nombre: 'Posición en el campeonato', valores: h.temporadas.map((t) => t.posicionCampeonato), color: colorDe(h.id) }];
  });

  // ---------------------------------------------------------------- comparativa histórica
  protected readonly comparativa = signal<ComparativaHistorica | null>(null);
  protected readonly cargandoComparativa = signal(false);
  protected readonly errorComparativa = signal<string | null>(null);
  protected readonly etiquetasComparativa = computed(() => (this.comparativa()?.anios ?? []).map(String));
  protected readonly seriesComparativa = computed<SerieGrafico[]>(() => {
    const c = this.comparativa();
    if (!c) return [];
    return [
      { nombre: c.a.nombre, valores: c.posicionesA, color: '#e50914' },
      { nombre: c.b.nombre, valores: c.posicionesB, color: '#2f7cf6' },
    ];
  });

  private pedidoHistorico = 0;
  private pedidoComparativa = 0;

  constructor() {
    // Equipos de la categoría (para el selector)
    effect(() => {
      const siglas = this.siglas();
      untracked(() => {
        this.equipos.set([]);
        this.equipoService.buscarEquipos(siglas).subscribe({
          next: (lista) => this.equipos.set([...lista].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))),
          error: () => this.equipos.set([]),
        });
      });
    });

    // Historial del piloto o equipo elegido
    effect(() => {
      const siglas = this.siglas();
      const tipo = this.tipo();
      const pilotoId = this.piloto()?.id ?? null;
      const equipoId = this.equipoId();
      const id = tipo === 'PILOTO' ? pilotoId : equipoId;
      untracked(() => {
        const pedido = ++this.pedidoHistorico;
        if (id === null) { this.historico.set(null); this.cargando.set(false); return; }
        this.cargando.set(true);
        this.error.set(null);
        const consulta = tipo === 'PILOTO'
          ? this.estadisticaService.historicoPiloto(siglas, id)
          : this.estadisticaService.historicoEquipo(siglas, id);
        consulta.subscribe({
          next: (h) => { if (pedido === this.pedidoHistorico) { this.historico.set(h); this.cargando.set(false); } },
          error: (e) => { if (pedido === this.pedidoHistorico) { this.historico.set(null); this.error.set(mensajeError(e)); this.cargando.set(false); } },
        });
      });
    });

    // Comparativa histórica entre el piloto elegido y su rival
    effect(() => {
      const siglas = this.siglas();
      const a = this.piloto()?.id ?? null;
      const b = this.rival()?.id ?? null;
      untracked(() => {
        const pedido = ++this.pedidoComparativa;
        if (a === null || b === null || a === b) { this.comparativa.set(null); this.cargandoComparativa.set(false); return; }
        this.cargandoComparativa.set(true);
        this.errorComparativa.set(null);
        this.estadisticaService.comparativaHistorica(siglas, a, b).subscribe({
          next: (c) => { if (pedido === this.pedidoComparativa) { this.comparativa.set(c); this.cargandoComparativa.set(false); } },
          error: (e) => { if (pedido === this.pedidoComparativa) { this.comparativa.set(null); this.errorComparativa.set(mensajeError(e)); this.cargandoComparativa.set(false); } },
        });
      });
    });
  }

  protected elegirTipo(valor: string): void {
    this.tipo.set(valor === 'EQUIPO' ? 'EQUIPO' : 'PILOTO');
  }

  protected elegirPiloto(p: PilotoElegido): void {
    this.eleccion.set({ siglas: this.siglas(), piloto: p, equipoId: this.equipoId(), rival: this.rival() });
  }

  protected elegirRival(p: PilotoElegido): void {
    this.eleccion.set({ siglas: this.siglas(), piloto: this.piloto(), equipoId: this.equipoId(), rival: p });
  }

  protected elegirEquipo(valor: string): void {
    this.eleccion.set({ siglas: this.siglas(), piloto: this.piloto(), equipoId: Number(valor), rival: this.rival() });
  }
}
