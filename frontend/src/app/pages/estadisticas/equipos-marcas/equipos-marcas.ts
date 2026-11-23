import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EvolucionGrupos, FilaCampeonatoEquipo, FilaMarca, TendenciaGrupo, TipoGrupo } from '../../../core/modelos';
import { EstadisticaService } from '../../../core/services/estadistica.service';
import { mensajeError } from '../../../core/errores/error';
import { EstadoCarga } from '../../../shared/estado-carga/estado-carga';
import { Grafico, SerieGrafico } from '../../../shared/grafico/grafico';
import { colorPorIndice } from '../../../shared/colores';

function etiquetasDe(rondas: { numeroFecha: number | null; evento: string }[]): string[] {
  return rondas.map((r) => (r.numeroFecha !== null ? `F${r.numeroFecha}` : r.evento));
}

/**
 * Pestaña "Equipos y marcas" de Estadísticas (MVP 6): ranking de marcas, evolución de puntos
 * por equipo o marca, y tendencia de un equipo o una marca. Todo lo calcula el backend.
 */
@Component({
  imports: [RouterLink, EstadoCarga, Grafico],
  selector: 'app-equipos-marcas',
  styleUrl: './equipos-marcas.css',
  templateUrl: './equipos-marcas.html',
})
export class EquiposMarcas {
  private readonly estadisticaService = inject(EstadisticaService);

  readonly siglas = input.required<string>();
  readonly anio = input.required<number>();

  private readonly clave = computed(() => `${this.siglas()}-${this.anio()}`);

  /**
   * En TC la marca es el motor de cada piloto y se carga en su participación (Admin → Participaciones):
   * un mismo equipo puede correr autos de distintas marcas. El backend la toma de ahí; si la temporada
   * no tiene marcas cargadas, agrupa por equipo. En pantalla se aclara.
   */
  protected readonly esTc = computed(() => this.siglas().toUpperCase() === 'TC');

  // ---------------------------------------------------------------- ranking de marcas
  protected readonly marcas = signal<FilaMarca[]>([]);
  protected readonly cargandoMarcas = signal(false);
  protected readonly errorMarcas = signal<string | null>(null);

  // Equipos de la temporada (para elegir la tendencia de un equipo)
  protected readonly equipos = signal<FilaCampeonatoEquipo[]>([]);

  // ---------------------------------------------------------------- evolución por grupos
  protected readonly tipoEvolucion = signal<TipoGrupo>('EQUIPO');
  protected readonly evolucion = signal<EvolucionGrupos | null>(null);
  protected readonly cargandoEvolucion = signal(false);
  protected readonly errorEvolucion = signal<string | null>(null);
  protected readonly etiquetasEvolucion = computed(() => etiquetasDe(this.evolucion()?.rondas ?? []));
  protected readonly detallesEvolucion = computed(() => (this.evolucion()?.rondas ?? []).map((r) => r.evento));
  protected readonly seriesEvolucion = computed<SerieGrafico[]>(() =>
    (this.evolucion()?.series ?? []).map((s, i) => ({ nombre: s.nombre, valores: s.puntosAcumulados, color: colorPorIndice(i) })),
  );

  // ---------------------------------------------------------------- tendencia de un grupo
  protected readonly tipoTendencia = signal<TipoGrupo>('EQUIPO');
  private readonly eleccion = signal<{ clave: string | null; equipoId: number | null; marca: string | null }>({ clave: null, equipoId: null, marca: null });
  protected readonly equipoId = computed(() => {
    const e = this.eleccion();
    return (e.clave === this.clave() ? e.equipoId : null) ?? this.equipos()[0]?.equipoId ?? null;
  });
  protected readonly marca = computed(() => {
    const e = this.eleccion();
    return (e.clave === this.clave() ? e.marca : null) ?? this.marcas()[0]?.marca ?? null;
  });
  protected readonly tendencia = signal<TendenciaGrupo | null>(null);
  protected readonly cargandoTendencia = signal(false);
  protected readonly errorTendencia = signal<string | null>(null);
  protected readonly etiquetasTendencia = computed(() => etiquetasDe(this.tendencia()?.fechas ?? []));
  protected readonly detallesTendencia = computed(() => (this.tendencia()?.fechas ?? []).map((f) => f.evento));
  protected readonly seriesTendencia = computed<SerieGrafico[]>(() => {
    const t = this.tendencia();
    if (!t) return [];
    return [
      { nombre: 'Puntos de la fecha', valores: t.fechas.map((f) => f.puntos), color: colorPorIndice(1), relleno: true },
      { nombre: 'Media móvil (3 fechas)', valores: t.fechas.map((f) => f.mediaMovilPuntos), color: '#c9d1d9', punteada: true },
    ];
  });

  private pedidoBase = 0;
  private pedidoEvolucion = 0;
  private pedidoTendencia = 0;

  constructor() {
    // Ranking de marcas y equipos de la temporada
    effect(() => {
      const siglas = this.siglas();
      const anio = this.anio();
      untracked(() => {
        const pedido = ++this.pedidoBase;
        this.cargandoMarcas.set(true);
        this.errorMarcas.set(null);
        this.marcas.set([]);
        this.equipos.set([]);
        this.estadisticaService.marcas(siglas, anio).subscribe({
          next: (m) => { if (pedido === this.pedidoBase) { this.marcas.set(m); this.cargandoMarcas.set(false); } },
          error: (e) => { if (pedido === this.pedidoBase) { this.errorMarcas.set(mensajeError(e)); this.cargandoMarcas.set(false); } },
        });
        this.estadisticaService.campeonatoEquipos(siglas, anio).subscribe({
          next: (eq) => { if (pedido === this.pedidoBase) this.equipos.set(eq); },
          error: () => { if (pedido === this.pedidoBase) this.equipos.set([]); },
        });
      });
    });

    // Evolución de puntos acumulados por equipo o por marca (top 6)
    effect(() => {
      const siglas = this.siglas();
      const anio = this.anio();
      const tipo = this.tipoEvolucion();
      untracked(() => {
        const pedido = ++this.pedidoEvolucion;
        this.cargandoEvolucion.set(true);
        this.errorEvolucion.set(null);
        this.estadisticaService.evolucionGrupos(siglas, anio, tipo).subscribe({
          next: (r) => { if (pedido === this.pedidoEvolucion) { this.evolucion.set(r); this.cargandoEvolucion.set(false); } },
          error: (e) => { if (pedido === this.pedidoEvolucion) { this.evolucion.set(null); this.errorEvolucion.set(mensajeError(e)); this.cargandoEvolucion.set(false); } },
        });
      });
    });

    // Tendencia del equipo o de la marca elegida
    effect(() => {
      const siglas = this.siglas();
      const anio = this.anio();
      const tipo = this.tipoTendencia();
      const equipoId = this.equipoId();
      const marca = this.marca();
      const pedidoDe = tipo === 'EQUIPO'
        ? (equipoId !== null ? this.estadisticaService.tendenciaEquipo(siglas, anio, equipoId) : null)
        : (marca ? this.estadisticaService.tendenciaMarca(siglas, anio, marca) : null);
      untracked(() => {
        const pedido = ++this.pedidoTendencia;
        if (!pedidoDe) { this.tendencia.set(null); this.cargandoTendencia.set(false); return; }
        this.cargandoTendencia.set(true);
        this.errorTendencia.set(null);
        pedidoDe.subscribe({
          next: (r) => { if (pedido === this.pedidoTendencia) { this.tendencia.set(r); this.cargandoTendencia.set(false); } },
          error: (e) => { if (pedido === this.pedidoTendencia) { this.tendencia.set(null); this.errorTendencia.set(mensajeError(e)); this.cargandoTendencia.set(false); } },
        });
      });
    });
  }

  protected elegirEquipo(valor: string): void {
    this.eleccion.set({ clave: this.clave(), equipoId: Number(valor), marca: this.marca() });
  }

  protected elegirMarca(valor: string): void {
    this.eleccion.set({ clave: this.clave(), equipoId: this.equipoId(), marca: valor });
  }

  protected elegirTipoTendencia(valor: string): void {
    this.tipoTendencia.set(valor === 'MARCA' ? 'MARCA' : 'EQUIPO');
  }
}
