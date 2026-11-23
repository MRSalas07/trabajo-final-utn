import { Component, computed, input, output } from '@angular/core';
import { FilaTorre, FocoTelemetria } from '../../../../core/modelos';

/** Escala de la traza de velocidad: un F1 no pasa de ~360 km/h. */
const VELOCIDAD_MAXIMA = 360;

/**
 * Telemetría del piloto seguido: velocidad, marcha, rpm, DRS, pedales y la traza de los últimos ~30 s.
 * Desde acá también se elige a quién seguir (o volver al líder).
 */
@Component({
  selector: 'app-panel-foco',
  styleUrl: './panel-foco.css',
  templateUrl: './panel-foco.html',
})
export class PanelFoco {
  readonly foco = input<FocoTelemetria | null>(null);
  /** Pilotos de la torre, para el selector. */
  readonly pilotos = input<FilaTorre[]>([]);
  /** Piloto elegido por el espectador (null = sigue al líder). */
  readonly seguido = input<number | null>(null);
  /** El número del piloto a seguir, o null para volver al líder. */
  readonly elegir = output<number | null>();

  /** Orden fijo (por sigla) para que el selector no se reacomode con cada cambio de posición. */
  protected readonly opciones = computed(() =>
    [...this.pilotos()].sort((a, b) => (a.acronimo ?? String(a.numero)).localeCompare(b.acronimo ?? String(b.numero))),
  );

  protected readonly marcha = computed(() => {
    const marcha = this.foco()?.marcha;
    return marcha === null || marcha === undefined ? '—' : marcha === 0 ? 'N' : String(marcha);
  });

  /** Las tres trazas en un mismo SVG de 100 x 64: velocidad arriba, acelerador y freno debajo. */
  protected readonly trazas = computed(() => {
    const f = this.foco();
    if (!f || f.trazaVelocidad.length < 2) return null;
    return {
      velocidad: camino(f.trazaVelocidad, VELOCIDAD_MAXIMA, 2, 34),
      acelerador: camino(f.trazaAcelerador, 100, 40, 50),
      freno: camino(f.trazaFreno, 100, 54, 62),
    };
  });

  protected cambiar(valor: string): void {
    this.elegir.emit(valor === '' ? null : Number(valor));
  }
}

/** Path SVG de una serie entre "arriba" (valor máximo) y "abajo" (cero). Los huecos (null) cortan la línea. */
function camino(valores: (number | null)[], maximo: number, arriba: number, abajo: number): string {
  if (valores.length < 2) return '';
  const paso = 100 / (valores.length - 1);
  let d = '';
  let nuevoTramo = true;
  valores.forEach((valor, i) => {
    if (valor === null || !Number.isFinite(valor)) {
      nuevoTramo = true;
      return;
    }
    const y = abajo - (Math.min(Math.max(valor, 0), maximo) / maximo) * (abajo - arriba);
    d += `${nuevoTramo ? 'M' : 'L'}${(i * paso).toFixed(2)} ${y.toFixed(2)}`;
    nuevoTramo = false;
  });
  return d;
}
