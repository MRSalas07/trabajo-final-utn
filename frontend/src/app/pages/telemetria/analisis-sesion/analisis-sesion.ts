import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { SeriePosiciones, SerieVueltas, SesionTelemetria, TelemetriaSesion } from '../../../core/modelos';
import { TelemetriaService } from '../../../core/services/telemetria.service';
import { mensajeError } from '../../../core/errores/error';
import { EstadoCarga } from '../../../shared/estado-carga/estado-carga';
import { Grafico, SerieGrafico } from '../../../shared/grafico/grafico';
import { colorPorIndice } from '../../../shared/colores';
import { TiempoPipe } from '../../../shared/pipe/formato';

/** Pilotos por gráfico: el backend compara hasta 6 (TelemetriaServiceImpl.MAX_PILOTOS_VUELTAS). */
const MAX_PILOTOS = 6;
/** Regla del 107 %: una vuelta más lenta que eso es de entrada a boxes, auto de seguridad o enfriamiento. */
const LIMITE_VUELTA_LENTA = 1.07;
/** Misma referencia siempre: así el effect no vuelve a pedir datos si nada cambió. */
const SIN_ELECCION: number[] = [];

interface PilotoSesion {
  numero: number;
  acronimo: string;
  nombre: string;
  equipo: string | null;
  color: string;
  /** Última posición que informó OpenF1 (la final), o null si no hay posiciones. */
  posicion: number | null;
}

interface FilaResumen extends PilotoSesion {
  mejor: number | null;
  vueltaMejor: number | null;
  cronometradas: number;
  punteada: boolean;
}

/**
 * Análisis de una sesión terminada de F1 (MVP 7): tiempos por vuelta y posiciones minuto a minuto de
 * hasta 6 pilotos, más un resumen con la mejor vuelta de cada uno. El backend arma las series con los
 * datos de OpenF1; acá solo se elige qué pilotos ver y se acomodan los datos para los gráficos.
 */
@Component({
  imports: [EstadoCarga, Grafico, TiempoPipe],
  selector: 'app-analisis-sesion',
  styleUrl: './analisis-sesion.css',
  templateUrl: './analisis-sesion.html',
})
export class AnalisisSesion {
  private readonly telemetriaService = inject(TelemetriaService);

  readonly sesion = input.required<SesionTelemetria>();
  private readonly sessionKey = computed(() => this.sesion().sessionKey);

  protected readonly maximo = MAX_PILOTOS;
  protected readonly datos = signal<TelemetriaSesion | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly ocultarLentas = signal(true);

  /** Pilotos que eligió el usuario: valen solo para la sesión en la que los eligió. */
  private readonly eleccion = signal<{ sessionKey: number; numeros: number[] } | null>(null);
  protected readonly eleccionPropia = computed(() => this.eleccion()?.sessionKey === this.sessionKey());
  /** Pilotos que se le piden al backend (vacío = que elija él los mejor ubicados). */
  private readonly pedidos = computed(() => {
    const e = this.eleccion();
    return e && e.sessionKey === this.sessionKey() ? e.numeros : SIN_ELECCION;
  });

  /** Todos los pilotos de la sesión, con su color y su posición final. */
  private readonly pilotos = computed(() => {
    const d = this.datos();
    const mapa = new Map<number, PilotoSesion>();
    if (!d) return mapa;
    const finales = new Map<number, number>();
    for (const s of d.posiciones) {
      if (s.posiciones.length) finales.set(s.numero, s.posiciones[s.posiciones.length - 1]);
    }
    const agregar = (numero: number, acronimo: string | null, nombre: string | null, equipo: string | null, color: string | null) => {
      if (mapa.has(numero)) return;
      mapa.set(numero, {
        numero,
        acronimo: acronimo ?? String(numero),
        nombre: nombre ?? '',
        equipo,
        color: color ?? colorPorIndice(mapa.size),
        posicion: finales.get(numero) ?? null,
      });
    };
    for (const p of d.pilotos) agregar(p.numero, p.acronimo, p.nombre, p.equipo, p.color);
    // Por si OpenF1 informa vueltas o posiciones de un auto que no figura en la lista de pilotos
    for (const s of [...d.vueltas, ...d.posiciones]) agregar(s.numero, s.acronimo, null, null, s.color);
    return mapa;
  });

  /** Números de los pilotos elegidos (los del usuario o, si no eligió, los que mandó el backend). */
  protected readonly seleccionados = computed<number[]>(() => {
    const e = this.eleccion();
    if (e && e.sessionKey === this.sessionKey()) return e.numeros;
    const d = this.datos();
    if (!d) return [];
    if (d.vueltas.length) return d.vueltas.map((v) => v.numero);
    // Sin vueltas (OpenF1 no las tiene): los mejor ubicados según las posiciones, como hace el backend
    return porPosicion([...this.pilotos().values()].filter((p) => p.posicion !== null))
      .slice(0, MAX_PILOTOS)
      .map((p) => p.numero);
  });

  protected readonly chips = computed(() => {
    const elegidos = new Set(this.seleccionados());
    return porPosicion([...this.pilotos().values()]).map((p) => ({ ...p, activo: elegidos.has(p.numero) }));
  });

  /** Los pilotos elegidos, ordenados por posición final (es el orden de las leyendas y del resumen). */
  private readonly elegidos = computed(() => {
    const pilotos = this.pilotos();
    return porPosicion(this.seleccionados().flatMap((n) => pilotos.get(n) ?? []));
  });

  /** Los dos pilotos de un equipo tienen el mismo color: el segundo va con línea punteada. */
  private readonly punteados = computed(() => {
    const equipos = new Set<string>();
    const punteados = new Set<number>();
    for (const p of this.elegidos()) {
      if (!p.equipo) continue;
      if (equipos.has(p.equipo)) punteados.add(p.numero);
      else equipos.add(p.equipo);
    }
    return punteados;
  });

  protected readonly graficoVueltas = computed(() => {
    const porNumero = new Map<number, SerieVueltas>((this.datos()?.vueltas ?? []).map((s) => [s.numero, s]));
    const pares = this.elegidos().flatMap((piloto) => {
      const serie = porNumero.get(piloto.numero);
      return serie ? [{ piloto, serie }] : [];
    });
    let ultima = 0;
    let mejor = Infinity;
    for (const { serie } of pares) {
      for (const v of serie.vueltas) ultima = Math.max(ultima, v);
      for (const t of serie.tiempos) mejor = Math.min(mejor, t);
    }
    const limite = this.ocultarLentas() ? mejor * LIMITE_VUELTA_LENTA : Infinity;
    const punteados = this.punteados();
    const etiquetas = Array.from({ length: ultima }, (_, i) => String(i + 1));
    const series: SerieGrafico[] = pares.map(({ piloto, serie }) => {
      const valores: (number | null)[] = Array.from({ length: ultima }, () => null);
      serie.vueltas.forEach((vuelta, i) => {
        const tiempo = serie.tiempos[i];
        if (vuelta >= 1 && tiempo !== undefined && tiempo <= limite) valores[vuelta - 1] = tiempo;
      });
      return { nombre: piloto.acronimo, valores, color: piloto.color, punteada: punteados.has(piloto.numero) };
    });
    return { etiquetas, detalles: etiquetas.map((v) => `Vuelta ${v}`), series };
  });

  protected readonly graficoPosiciones = computed(() => {
    const posiciones = this.datos()?.posiciones ?? [];
    // Mismo eje para cualquier elección: de 0 al último minuto con datos de toda la sesión
    let ultimo = 0;
    for (const s of posiciones) for (const m of s.minutos) ultimo = Math.max(ultimo, m);
    const minutos = Math.ceil(ultimo);
    const etiquetas = Array.from({ length: minutos + 1 }, (_, m) => String(m));
    const porNumero = new Map<number, SeriePosiciones>(posiciones.map((s) => [s.numero, s]));
    const punteados = this.punteados();
    const series: SerieGrafico[] = this.elegidos().flatMap((piloto) => {
      const serie = porNumero.get(piloto.numero);
      if (!serie || !serie.minutos.length) return [];
      return [{
        nombre: piloto.acronimo,
        valores: posicionPorMinuto(serie, minutos),
        color: piloto.color,
        punteada: punteados.has(piloto.numero),
        escalonada: true,
      }];
    });
    return { etiquetas, detalles: etiquetas.map((m) => `Minuto ${m}`), series };
  });

  protected readonly resumen = computed<FilaResumen[]>(() => {
    const porNumero = new Map<number, SerieVueltas>((this.datos()?.vueltas ?? []).map((s) => [s.numero, s]));
    const punteados = this.punteados();
    return this.elegidos().map((piloto) => {
      const serie = porNumero.get(piloto.numero);
      let mejor: number | null = null;
      let vueltaMejor: number | null = null;
      for (let i = 0; serie && i < serie.tiempos.length; i++) {
        if (mejor === null || serie.tiempos[i] < mejor) {
          mejor = serie.tiempos[i];
          vueltaMejor = serie.vueltas[i] ?? null;
        }
      }
      return { ...piloto, mejor, vueltaMejor, cronometradas: serie?.tiempos.length ?? 0, punteada: punteados.has(piloto.numero) };
    });
  });

  /** La mejor vuelta entre los pilotos elegidos (se marca en violeta, como en las transmisiones de F1). */
  protected readonly mejorVuelta = computed(() => {
    const tiempos = this.resumen().flatMap((f) => (f.mejor === null ? [] : [f.mejor]));
    return tiempos.length ? Math.min(...tiempos) : null;
  });

  private pedido = 0;

  constructor() {
    effect(() => {
      const sessionKey = this.sessionKey();
      const pilotos = this.pedidos();
      untracked(() => this.cargar(sessionKey, pilotos));
    });
  }

  protected alternar(numero: number): void {
    const actuales = this.seleccionados();
    const nuevos = actuales.includes(numero) ? actuales.filter((n) => n !== numero) : [...actuales, numero];
    if (!nuevos.length || nuevos.length > MAX_PILOTOS) return;
    this.eleccion.set({ sessionKey: this.sessionKey(), numeros: nuevos });
  }

  /** Vuelve a los pilotos que elige el backend (los mejor ubicados). */
  protected restablecer(): void {
    this.eleccion.set(null);
  }

  protected reintentar(): void {
    this.cargar(this.sessionKey(), this.pedidos());
  }

  private cargar(sessionKey: number, pilotos: number[]): void {
    const pedido = ++this.pedido;
    // Al cambiar de sesión se limpia; al cambiar de pilotos se siguen viendo los datos anteriores mientras carga
    if (this.datos()?.sesion.sessionKey !== sessionKey) this.datos.set(null);
    this.cargando.set(true);
    this.error.set(null);
    this.telemetriaService.sesion(sessionKey, pilotos).subscribe({
      next: (d) => {
        if (pedido !== this.pedido) return;
        this.datos.set(d);
        this.cargando.set(false);
      },
      error: (e) => {
        if (pedido !== this.pedido) return;
        this.error.set(mensajeError(e));
        this.cargando.set(false);
      },
    });
  }
}

/** Por posición final (los que no tienen posición, al final) y, a igual posición, por número de auto. */
function porPosicion<T extends { numero: number; posicion: number | null }>(lista: T[]): T[] {
  return [...lista].sort((a, b) => (a.posicion ?? Infinity) - (b.posicion ?? Infinity) || a.numero - b.numero);
}

/** La posición en cada minuto: la del último cambio informado hasta ese minuto (null antes del primero). */
function posicionPorMinuto(serie: SeriePosiciones, ultimo: number): (number | null)[] {
  const valores: (number | null)[] = [];
  let i = 0;
  let actual: number | null = null;
  for (let minuto = 0; minuto <= ultimo; minuto++) {
    while (i < serie.minutos.length && serie.minutos[i] <= minuto) {
      actual = serie.posiciones[i] ?? actual;
      i++;
    }
    valores.push(actual);
  }
  return valores;
}
