import { Component, input, output } from '@angular/core';
import { FilaTorre } from '../../../../core/modelos';
import { TiempoPipe } from '../../../../shared/pipe/formato';

/** Compuestos que informa OpenF1: la letra que se ve en la TV y su nombre en castellano. */
const NEUMATICOS: Record<string, { letra: string; nombre: string }> = {
  SOFT: { letra: 'S', nombre: 'Blando' },
  MEDIUM: { letra: 'M', nombre: 'Medio' },
  HARD: { letra: 'H', nombre: 'Duro' },
  INTERMEDIATE: { letra: 'I', nombre: 'Intermedio' },
  WET: { letra: 'W', nombre: 'Lluvia' },
};

/** Torre de tiempos de la transmisión: tocar un piloto muestra su telemetría. */
@Component({
  imports: [TiempoPipe],
  selector: 'app-torre-tiempos',
  styleUrl: './torre-tiempos.css',
  templateUrl: './torre-tiempos.html',
})
export class TorreTiempos {
  readonly filas = input<FilaTorre[]>([]);
  /** Piloto cuya telemetría se está mostrando (se resalta). */
  readonly foco = input<number | null>(null);
  readonly elegir = output<number>();

  protected neumatico(compuesto: string | null): { letra: string; nombre: string } | null {
    if (!compuesto) return null;
    return NEUMATICOS[compuesto] ?? { letra: '?', nombre: compuesto };
  }
}
