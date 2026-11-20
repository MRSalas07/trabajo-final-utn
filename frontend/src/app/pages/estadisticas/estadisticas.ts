import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Categoria, ComparativaPilotos, EvolucionCampeonato, FilaCampeonatoPiloto, Temporada, TendenciaPiloto } from '../../core/modelos';
import { EstadisticaService } from '../../core/services/estadistica.service';
import { EventoService } from '../../core/services/evento.service';
import { SaludServices } from '../../core/services/salud.service';
import { mensajeError } from '../../core/errores/error';
import { EstadoCarga } from '../../shared/estado-carga/estado-carga';
import { Grafico, SerieGrafico } from '../../shared/grafico/grafico';
import { colorDe } from '../../shared/colores';

/** Máximo de pilotos que acepta el backend en la evolución. */
const MAXIMO_EVOLUCION = 20;
const SIN_SELECCION: number[] = [];

/** Etiqueta corta de cada ronda para el eje X ("F1", "F2"... o el nombre si no tiene número). */
function etiquetasDe(rondas: { numeroFecha: number | null; evento: string }[]): string[] {
  return rondas.map((r) => (r.numeroFecha !== null ? `F${r.numeroFecha}` : r.evento));
}

/** Nombre completo del evento, para el tooltip. */
function detallesDe(rondas: { evento: string }[]): string[] {
  return rondas.map((r) => r.evento);
}

/** Fila de la comparativa: un indicador de A contra B. */
interface FilaComparada {
  etiqueta: string;
  a: number;
  b: number;
  /** true cuando el número más chico es el mejor (promedio de posición). */
  menorEsMejor?: boolean;
}

/**
 * Estadísticas de una temporada: evolución del campeonato, tendencia de un piloto y comparativa A vs B.
 * Todo lo calcula el backend (MotorEstadistico); acá solo se elige qué mostrar y se grafica.
 * Acepta ?categoria=F1&anio=2025 en la URL (link desde la página de cada categoría).
 */
@Component({
  imports: [RouterLink, EstadoCarga, Grafico],
  selector: 'app-estadisticas',
  styleUrl: './estadisticas.css',
  templateUrl: './estadisticas.html',
})
export class Estadisticas {
  private readonly estadisticaService = inject(EstadisticaService);
  private readonly eventoService = inject(EventoService);
  private readonly serviceSalud = inject(SaludServices);

  // Query params (withComponentInputBinding)
  readonly categoria = input<string>();
  readonly anio = input<string>();

  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly siglas = signal<string | null>(null);
  protected readonly temporadas = signal<Temporada[]>([]);
  protected readonly anioElegido = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);

  /** Categoría + año elegidos: cada selección de pilotos vale solo para su temporada. */
  private readonly clave = computed(() => {
    const siglas = this.siglas();
    const anio = this.anioElegido();
    return siglas && anio ? `${siglas}-${anio}` : null;
  });

  // Campeonato de pilotos: alimenta los selectores
  private readonly campeonato = signal<{ clave: string; filas: FilaCampeonatoPiloto[] } | null>(null);
  protected readonly cargandoPilotos = signal(false);
  protected readonly pilotos = computed(() => {
    const c = this.campeonato();
    return c && c.clave === this.clave() ? c.filas : [];
  });
  protected readonly conPoles = computed(() => this.siglas()?.toUpperCase() === 'F1');

  // ---------------------------------------------------------------- evolución
  private readonly seleccionEvolucion = signal<{ clave: string | null; ids: number[] }>({ clave: null, ids: [] });
  protected readonly idsEvolucion = computed(() => {
    const s = this.seleccionEvolucion();
    return s.clave === this.clave() ? s.ids : SIN_SELECCION;
  });
  protected readonly metrica = signal<'puntos' | 'posicion'>('puntos');
  protected readonly evolucion = signal<EvolucionCampeonato | null>(null);
  protected readonly cargandoEvolucion = signal(false);
  protected readonly errorEvolucion = signal<string | null>(null);
  protected readonly maximoEvolucion = MAXIMO_EVOLUCION;
  protected readonly seriesEvolucion = computed<SerieGrafico[]>(() => {
    const evolucion = this.evolucion();
    if (!evolucion) return [];
    const porPuntos = this.metrica() === 'puntos';
    return evolucion.series.map((s) => ({
      nombre: s.codigo ?? s.piloto,
      valores: porPuntos ? s.puntosAcumulados : s.posicionCampeonato,
      color: colorDe(s.pilotoId),
    }));
  });

  protected readonly etiquetasEvolucion = computed(() => etiquetasDe(this.evolucion()?.rondas ?? []));
  protected readonly detallesEvolucion = computed(() => detallesDe(this.evolucion()?.rondas ?? []));

  // ---------------------------------------------------------------- tendencia
  private readonly eleccionTendencia = signal<{ clave: string | null; id: number | null }>({ clave: null, id: null });
  protected readonly tendenciaId = computed(() => {
    const e = this.eleccionTendencia();
    return (e.clave === this.clave() ? e.id : null) ?? this.pilotos()[0]?.pilotoId ?? null;
  });
  protected readonly tendencia = signal<TendenciaPiloto | null>(null);
  protected readonly cargandoTendencia = signal(false);
  protected readonly errorTendencia = signal<string | null>(null);
  protected readonly seriesTendencia = computed<SerieGrafico[]>(() => {
    const t = this.tendencia();
    if (!t) return [];
    return [
      { nombre: 'Posición final', valores: t.puntos.map((p) => p.posicion), color: colorDe(t.pilotoId) },
      { nombre: 'Media móvil (3 fechas)', valores: t.puntos.map((p) => p.mediaMovil), color: '#c9d1d9', punteada: true },
    ];
  });

  protected readonly etiquetasTendencia = computed(() => etiquetasDe(this.tendencia()?.puntos ?? []));
  protected readonly detallesTendencia = computed(() =>
    (this.tendencia()?.puntos ?? []).map((p) => (p.estado && p.estado !== 'FINALIZADO' ? `${p.evento} (${p.estado})` : p.evento)),
  );

  // ---------------------------------------------------------------- comparativa
  private readonly eleccionComparativa = signal<{ clave: string | null; a: number | null; b: number | null }>({ clave: null, a: null, b: null });
  protected readonly idA = computed(() => {
    const e = this.eleccionComparativa();
    return (e.clave === this.clave() ? e.a : null) ?? this.pilotos()[0]?.pilotoId ?? null;
  });
  protected readonly idB = computed(() => {
    const e = this.eleccionComparativa();
    return (e.clave === this.clave() ? e.b : null) ?? this.pilotos()[1]?.pilotoId ?? null;
  });
  protected readonly comparativa = signal<ComparativaPilotos | null>(null);
  protected readonly cargandoComparativa = signal(false);
  protected readonly errorComparativa = signal<string | null>(null);
  protected readonly filasComparadas = computed<FilaComparada[]>(() => {
    const c = this.comparativa();
    if (!c) return [];
    const filas: FilaComparada[] = [
      { etiqueta: 'Puntos', a: c.a.puntos, b: c.b.puntos },
      { etiqueta: 'Victorias', a: c.a.victorias, b: c.b.victorias },
      { etiqueta: 'Podios', a: c.a.podios, b: c.b.podios },
      { etiqueta: 'Eventos disputados', a: c.a.eventosDisputados, b: c.b.eventosDisputados },
    ];
    if (this.conPoles()) filas.splice(3, 0, { etiqueta: 'Poles', a: c.a.poles, b: c.b.poles });
    if (c.a.promedioPosicion !== null && c.b.promedioPosicion !== null) {
      filas.push({ etiqueta: 'Promedio de posición', a: c.a.promedioPosicion, b: c.b.promedioPosicion, menorEsMejor: true });
    }
    return filas;
  });
  protected readonly serieDiferencia = computed<SerieGrafico[]>(() => {
    const c = this.comparativa();
    if (!c) return [];
    // Mismo rojo que la barra de A en el duelo
    return [{ nombre: `${c.a.piloto} − ${c.b.piloto}`, valores: c.diferenciaPuntos, color: '#e50914', relleno: true }];
  });

  protected readonly etiquetasComparativa = computed(() => etiquetasDe(this.comparativa()?.rondas ?? []));
  protected readonly detallesComparativa = computed(() => detallesDe(this.comparativa()?.rondas ?? []));

  // Contadores para descartar respuestas viejas (si el usuario cambia rápido de selección)
  private pedidoEvolucion = 0;
  private pedidoTendencia = 0;
  private pedidoComparativa = 0;

  constructor() {
    this.serviceSalud.categorias().subscribe({
      next: (c) => {
        this.categorias.set(c);
        const pedida = this.categoria()?.toUpperCase();
        const inicial = c.find((x) => x.siglas.toUpperCase() === pedida) ?? c[0];
        if (inicial) this.siglas.set(inicial.siglas);
      },
      error: (e) => this.error.set(mensajeError(e)),
    });

    // Al cambiar de categoría: sus temporadas (vienen ordenadas de la más nueva a la más vieja)
    effect(() => {
      const siglas = this.siglas();
      if (!siglas) return;
      untracked(() => {
        this.anioElegido.set(null);
        this.eventoService.buscarTemporadas(siglas).subscribe({
          next: (t) => {
            this.temporadas.set(t);
            const pedido = Number(this.anio());
            this.anioElegido.set(t.find((x) => x.anio === pedido)?.anio ?? t[0]?.anio ?? null);
          },
          error: (e) => this.error.set(mensajeError(e)),
        });
      });
    });

    // Campeonato de la temporada (para los selectores de pilotos)
    effect(() => {
      const clave = this.clave();
      const siglas = this.siglas();
      const anio = this.anioElegido();
      if (!clave || !siglas || !anio) return;
      untracked(() => {
        this.cargandoPilotos.set(true);
        this.estadisticaService.campeonatoPilotos(siglas, anio).subscribe({
          next: (filas) => { this.campeonato.set({ clave, filas }); this.cargandoPilotos.set(false); },
          error: (e) => { this.campeonato.set({ clave, filas: [] }); this.error.set(mensajeError(e)); this.cargandoPilotos.set(false); },
        });
      });
    });

    // Evolución: top 5 por defecto o los pilotos elegidos
    effect(() => {
      const siglas = this.siglas();
      const anio = this.anioElegido();
      const ids = this.idsEvolucion();
      if (!this.clave() || !siglas || !anio) return;
      untracked(() => {
        const pedido = ++this.pedidoEvolucion;
        this.cargandoEvolucion.set(true);
        this.errorEvolucion.set(null);
        this.estadisticaService.evolucion(siglas, anio, ids).subscribe({
          next: (r) => { if (pedido === this.pedidoEvolucion) { this.evolucion.set(r); this.cargandoEvolucion.set(false); } },
          error: (e) => { if (pedido === this.pedidoEvolucion) { this.evolucion.set(null); this.errorEvolucion.set(mensajeError(e)); this.cargandoEvolucion.set(false); } },
        });
      });
    });

    // Tendencia del piloto elegido (por defecto, el líder)
    effect(() => {
      const siglas = this.siglas();
      const anio = this.anioElegido();
      const id = this.tendenciaId();
      if (!siglas || !anio || id === null) { untracked(() => this.tendencia.set(null)); return; }
      untracked(() => {
        const pedido = ++this.pedidoTendencia;
        this.cargandoTendencia.set(true);
        this.errorTendencia.set(null);
        this.estadisticaService.tendencia(siglas, anio, id).subscribe({
          next: (r) => { if (pedido === this.pedidoTendencia) { this.tendencia.set(r); this.cargandoTendencia.set(false); } },
          error: (e) => { if (pedido === this.pedidoTendencia) { this.tendencia.set(null); this.errorTendencia.set(mensajeError(e)); this.cargandoTendencia.set(false); } },
        });
      });
    });

    // Comparativa A vs B (por defecto, líder contra segundo)
    effect(() => {
      const siglas = this.siglas();
      const anio = this.anioElegido();
      const a = this.idA();
      const b = this.idB();
      if (!siglas || !anio || a === null || b === null || a === b) {
        untracked(() => this.comparativa.set(null));
        return;
      }
      untracked(() => {
        const pedido = ++this.pedidoComparativa;
        this.cargandoComparativa.set(true);
        this.errorComparativa.set(null);
        this.estadisticaService.comparar(siglas, anio, a, b).subscribe({
          next: (r) => { if (pedido === this.pedidoComparativa) { this.comparativa.set(r); this.cargandoComparativa.set(false); } },
          error: (e) => { if (pedido === this.pedidoComparativa) { this.comparativa.set(null); this.errorComparativa.set(mensajeError(e)); this.cargandoComparativa.set(false); } },
        });
      });
    });
  }

  // ---------------------------------------------------------------- acciones de la plantilla
  protected elegirCategoria(siglas: string): void {
    this.siglas.set(siglas);
  }

  protected elegirAnio(valor: string): void {
    this.anioElegido.set(Number(valor));
  }

  protected alternarPiloto(id: number): void {
    const actuales = this.idsEvolucion();
    const ids = actuales.includes(id)
      ? actuales.filter((x) => x !== id)
      : actuales.length < MAXIMO_EVOLUCION ? [...actuales, id] : actuales;
    this.seleccionEvolucion.set({ clave: this.clave(), ids });
  }

  protected volverAlTop(): void {
    this.seleccionEvolucion.set({ clave: this.clave(), ids: [] });
  }

  protected elegirTendencia(valor: string): void {
    this.eleccionTendencia.set({ clave: this.clave(), id: Number(valor) });
  }

  protected elegirComparado(lado: 'a' | 'b', valor: string): void {
    const id = Number(valor);
    this.eleccionComparativa.set({
      clave: this.clave(),
      a: lado === 'a' ? id : this.idA(),
      b: lado === 'b' ? id : this.idB(),
    });
  }

  /** Ancho (%) de la barra de A o de B en la comparativa. */
  protected porcentaje(valor: number, fila: FilaComparada): number {
    const total = fila.a + fila.b;
    if (total === 0) return 50;
    // Si el número más chico es el mejor, se invierte la proporción
    const proporcion = fila.menorEsMejor ? (total - valor) / total : valor / total;
    return Math.round(proporcion * 100);
  }

  /** true si A está mejor que B en esa fila (para resaltar al ganador). */
  protected ganaA(fila: FilaComparada): boolean {
    return fila.menorEsMejor ? fila.a < fila.b : fila.a > fila.b;
  }

  protected ganaB(fila: FilaComparada): boolean {
    return fila.menorEsMejor ? fila.b < fila.a : fila.b > fila.a;
  }
}
