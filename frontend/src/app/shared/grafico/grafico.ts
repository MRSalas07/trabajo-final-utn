import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, afterRenderEffect, inject, input, viewChild } from '@angular/core';
import {
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  ChartConfiguration,
  Filler,
  Legend,
  LinearScale,
  LineController,
  LineElement,
  PointElement,
  Tooltip,
} from 'chart.js';

// Se registran solo las piezas de Chart.js que se usan (bundle más chico).
Chart.register(LineController, BarController, LineElement, BarElement, PointElement, CategoryScale, LinearScale, Tooltip, Legend, Filler);

/** Una serie del gráfico: un valor por etiqueta (null = sin dato en esa ronda). */
export interface SerieGrafico {
  nombre: string;
  valores: (number | null)[];
  color: string;
  /** Línea punteada (por ejemplo, una media móvil). */
  punteada?: boolean;
  /** Rellena el área bajo la línea. */
  relleno?: boolean;
  /** Línea en escalones: cada valor se mantiene hasta el siguiente (por ejemplo, la posición en pista). */
  escalonada?: boolean;
}

/**
 * Envoltorio propio de Chart.js (sin ng2-charts): recibe etiquetas y series por input()
 * y redibuja cuando cambian. El backend calcula todo; este componente solo grafica.
 *
 * Uso: <app-grafico tipo="line" [etiquetas]="['F1','F2']" [series]="series" [invertirY]="true" />
 */
@Component({
  selector: 'app-grafico',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="lienzo" [style.height.px]="alto()">
      <canvas #lienzo role="img" [attr.aria-label]="descripcion()"></canvas>
    </div>
  `,
  styles: `
    :host { display: block; min-width: 0; }
    .lienzo { position: relative; width: 100%; }
  `,
})
export class Grafico {
  readonly tipo = input<'line' | 'bar'>('line');
  readonly etiquetas = input<string[]>([]);
  readonly series = input<SerieGrafico[]>([]);
  /** Texto del tooltip para cada etiqueta (por ejemplo, el nombre completo del evento). */
  readonly detalles = input<string[]>([]);
  /** Eje Y invertido: para posiciones, donde 1 es lo mejor y va arriba. */
  readonly invertirY = input(false);
  readonly tituloY = input('');
  readonly tituloX = input('');
  /** 'tiempo': los valores son segundos y se muestran como m:ss (eje) y m:ss.mmm (tooltip), por ejemplo tiempos de vuelta. */
  readonly formatoY = input<'numero' | 'tiempo'>('numero');
  /** Dibuja un punto en cada dato de las líneas. Con series largas conviene apagarlo. */
  readonly puntos = input(true);
  readonly alto = input(320);
  /** Descripción accesible del gráfico (lectores de pantalla). */
  readonly descripcion = input('Gráfico');

  private readonly lienzo = viewChild.required<ElementRef<HTMLCanvasElement>>('lienzo');
  private grafico?: Chart;

  constructor() {
    // afterRenderEffect: corre cuando el <canvas> ya existe en el DOM y cada vez que cambia un input.
    afterRenderEffect(() => this.dibujar());
    inject(DestroyRef).onDestroy(() => this.grafico?.destroy());
  }

  private dibujar(): void {
    const tipo = this.tipo();
    const config = this.configuracion(tipo);
    if (this.grafico && (this.grafico.config as { type?: string }).type === tipo) {
      this.grafico.data = config.data;
      this.grafico.options = config.options ?? {};
      this.grafico.update();
      return;
    }
    this.grafico?.destroy();
    this.grafico = new Chart(this.lienzo().nativeElement, config);
  }

  private configuracion(tipo: 'line' | 'bar'): ChartConfiguration {
    const estilos = getComputedStyle(document.documentElement);
    const textoSuave = estilos.getPropertyValue('--texto-suave').trim() || '#a7adb5';
    const grilla = 'rgba(255, 255, 255, 0.08)';
    const detalles = this.detalles();
    const enTiempo = this.formatoY() === 'tiempo';
    const conPuntos = tipo === 'line' && this.puntos();
    const datasets = this.series().map((s) => ({
      label: s.nombre,
      data: s.valores,
      borderColor: s.color,
      backgroundColor: s.relleno || tipo === 'bar' ? s.color + '55' : s.color,
      borderDash: s.punteada ? [6, 4] : [],
      borderWidth: 2,
      pointRadius: conPuntos ? 3 : 0,
      pointHoverRadius: 5,
      // 'before': la línea sigue horizontal con el valor anterior y sube o baja al llegar al nuevo dato
      stepped: s.escalonada ? 'before' : false,
      tension: s.escalonada ? 0 : 0.25,
      fill: !!s.relleno,
      spanGaps: true,
    }));

    // Chart.js tipa cada clase de gráfico por separado; este componente maneja dos (línea y barras)
    // con la misma configuración, por eso se convierte el objeto al tipo general.
    return {
      type: tipo,
      data: { labels: this.etiquetas(), datasets },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 250 },
        interaction: { mode: 'index', intersect: false },
        color: textoSuave,
        font: { family: estilos.getPropertyValue('--fuente').trim() || undefined },
        plugins: {
          legend: { labels: { color: textoSuave, boxWidth: 12 } },
          tooltip: {
            callbacks: {
              title: (items: { dataIndex: number; label: string }[]) =>
                items.length ? (detalles[items[0].dataIndex] ?? items[0].label) : '',
              ...(enTiempo && {
                label: (item: { dataset: { label?: string }; parsed: { y: number | null } }) =>
                  `${item.dataset.label ?? ''}: ${minutosSegundos(item.parsed.y, 3)}`,
              }),
            },
          },
        },
        scales: {
          x: {
            ticks: { color: textoSuave },
            grid: { color: grilla },
            title: { display: !!this.tituloX(), text: this.tituloX(), color: textoSuave },
          },
          y: {
            reverse: this.invertirY(),
            ticks: {
              color: textoSuave,
              precision: 0,
              ...(enTiempo && {
                callback: (valor: number | string) => (typeof valor === 'number' ? minutosSegundos(valor, 0) : valor),
              }),
            },
            grid: { color: grilla },
            title: { display: !!this.tituloY(), text: this.tituloY(), color: textoSuave },
          },
        },
      },
    } as unknown as ChartConfiguration;
  }
}

/** Segundos en formato de cronómetro: 92.345 -> "1:32.345" (3 decimales) o "1:32" (sin decimales). */
function minutosSegundos(segundos: number | null, decimales: number): string {
  if (segundos === null || !Number.isFinite(segundos)) return '—';
  const total = Number(segundos.toFixed(decimales));
  const minutos = Math.floor(total / 60);
  const resto = (total - minutos * 60).toFixed(decimales);
  return minutos ? `${minutos}:${resto.padStart(decimales ? decimales + 3 : 2, '0')}` : resto;
}
