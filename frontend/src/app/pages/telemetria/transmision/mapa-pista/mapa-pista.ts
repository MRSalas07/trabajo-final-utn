import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { PuntoMapa } from '../../../../core/modelos';

/**
 * Mapa del circuito en SVG, con las coordenadas de OpenF1 tal cual (sin imágenes del circuito):
 * el contorno es la nube de posiciones que el backend juntó de todos los autos y encima va cada auto.
 * En SVG el eje Y apunta hacia abajo, por eso se dibuja con -y.
 */
@Component({
  selector: 'app-mapa-pista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './mapa-pista.css',
  templateUrl: './mapa-pista.html',
})
export class MapaPista {
  /** Contorno del circuito: puntos [x, y]. */
  readonly trazado = input<[number, number][]>([]);
  readonly puntos = input<PuntoMapa[]>([]);
  /** Auto seguido: se dibuja más grande, con su sigla y por encima del resto. */
  readonly foco = input<number | null>(null);
  /** Desliza los autos de una foto a la siguiente (a velocidades altas saltarían en diagonal). */
  readonly animar = input(true);

  /** Caja que contiene el circuito y los tamaños de los dibujos, proporcionales al circuito. */
  protected readonly caja = computed(() => {
    let minX = Infinity;
    let maxX = -Infinity;
    let minY = Infinity;
    let maxY = -Infinity;
    const incluir = (x: number, y: number) => {
      if (!Number.isFinite(x) || !Number.isFinite(y)) return;
      minX = Math.min(minX, x);
      maxX = Math.max(maxX, x);
      minY = Math.min(minY, y);
      maxY = Math.max(maxY, y);
    };
    for (const [x, y] of this.trazado()) incluir(x, y);
    for (const p of this.puntos()) incluir(p.x, p.y);
    if (!Number.isFinite(minX)) return null;
    const lado = Math.max(maxX - minX, maxY - minY, 1);
    const margen = lado * 0.06;
    return {
      viewBox: `${minX - margen} ${-maxY - margen} ${maxX - minX + 2 * margen} ${maxY - minY + 2 * margen}`,
      linea: lado * 0.012,
      auto: lado * 0.016,
      texto: lado * 0.04,
    };
  });

  /** Cada punto del contorno como un trazo de largo cero: con punta redonda se ve como un punto (un solo path). */
  protected readonly camino = computed(() => this.trazado().map(([x, y]) => `M${x} ${-y}h0`).join(''));

  /** Cada auto con su desplazamiento; el seguido va último para quedar dibujado arriba de los demás. */
  protected readonly autos = computed(() => {
    const foco = this.foco();
    return [...this.puntos()]
      .sort((a, b) => Number(a.numero === foco) - Number(b.numero === foco))
      .map((p) => ({ ...p, transform: `translate(${p.x}px, ${-p.y}px)` }));
  });
}
