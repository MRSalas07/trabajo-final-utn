import { Component, computed, input, output } from '@angular/core';
import { FilaFoto, Versus } from '../../../../core/modelos';
import { TiempoPipe } from '../../../../shared/pipe/formato';

/** Cómo se muestra un dato y qué valor gana: el menor (tiempos), el mayor (velocidades) o ninguno. */
type Formato = 'tiempo' | 'velocidad' | 'numero';
type Gana = 'menor' | 'mayor' | null;

interface Renglon {
  etiqueta: string;
  a: number | null;
  b: number | null;
  formato: Formato;
  gana: Gana;
  /** Sector (0, 1 o 2) si es un tiempo de sector: se marca en violeta si es el mejor de la sesión. */
  sector?: number;
}

/**
 * Versus de dos pilotos (MVP 7): sus datos uno al lado del otro y sus últimas vueltas comparadas.
 * Los datos vienen calculados del backend; acá solo se elige qué mostrar y se resalta el mejor de cada renglón.
 */
@Component({
  imports: [TiempoPipe],
  selector: 'app-versus-pilotos',
  styleUrl: './versus-pilotos.css',
  templateUrl: './versus-pilotos.html',
})
export class VersusPilotos {
  readonly versus = input.required<Versus>();
  /** La torre: de ahí salen los pilotos que se pueden elegir. */
  readonly filas = input<FilaFoto[]>([]);
  /** Los dos números de auto elegidos. */
  readonly elegir = output<number[]>();

  protected readonly renglones = computed<Renglon[]>(() => {
    const { a, b } = this.versus();
    const sectores: Renglon[] = [0, 1, 2].map((indice) => ({
      etiqueta: `Última · S${indice + 1}`, a: sectorDe(a.ultimaVuelta, indice), b: sectorDe(b.ultimaVuelta, indice), formato: 'tiempo', gana: 'menor', sector: indice,
    }));
    const mejoresSectores: Renglon[] = [0, 1, 2].map((indice) => ({
      etiqueta: `Mejor S${indice + 1}`, a: a.mejoresSectores[indice], b: b.mejoresSectores[indice], formato: 'tiempo', gana: 'menor', sector: indice,
    }));
    const enCursoA = a.vueltaEnCurso;
    const enCursoB = b.vueltaEnCurso;
    return [
      { etiqueta: 'Posición', a: a.posicion, b: b.posicion, formato: 'numero', gana: 'menor' },
      // Lo que está pasando ahora: cada sector aparece recién cuando el piloto lo completa
      { etiqueta: 'En curso · S1', a: enCursoA?.sector1 ?? null, b: enCursoB?.sector1 ?? null, formato: 'tiempo', gana: 'menor', sector: 0 },
      { etiqueta: 'En curso · velocidad I1', a: enCursoA?.velocidadI1 ?? null, b: enCursoB?.velocidadI1 ?? null, formato: 'velocidad', gana: 'mayor' },
      { etiqueta: 'En curso · S2', a: enCursoA?.sector2 ?? null, b: enCursoB?.sector2 ?? null, formato: 'tiempo', gana: 'menor', sector: 1 },
      { etiqueta: 'En curso · velocidad I2', a: enCursoA?.velocidadI2 ?? null, b: enCursoB?.velocidadI2 ?? null, formato: 'velocidad', gana: 'mayor' },
      { etiqueta: 'Vueltas', a: a.vueltas, b: b.vueltas, formato: 'numero', gana: null },
      { etiqueta: 'Última vuelta', a: a.ultimaVuelta?.tiempo ?? null, b: b.ultimaVuelta?.tiempo ?? null, formato: 'tiempo', gana: 'menor' },
      ...sectores,
      { etiqueta: 'Mejor vuelta', a: a.mejorVuelta?.tiempo ?? null, b: b.mejorVuelta?.tiempo ?? null, formato: 'tiempo', gana: 'menor' },
      ...mejoresSectores,
      { etiqueta: 'Velocidad en la trampa (última)', a: a.ultimaVuelta?.velocidadTrampa ?? null, b: b.ultimaVuelta?.velocidadTrampa ?? null, formato: 'velocidad', gana: 'mayor' },
      { etiqueta: 'Velocidad máxima', a: a.velocidadMaxima, b: b.velocidadMaxima, formato: 'velocidad', gana: 'mayor' },
      { etiqueta: 'Paradas en boxes', a: a.paradas, b: b.paradas, formato: 'numero', gana: null },
      { etiqueta: 'Tiempo en boxes (última parada)', a: a.tiempoEnBoxes, b: b.tiempoEnBoxes, formato: 'tiempo', gana: 'menor' },
    ];
  });

  /** Cuántas de las últimas vueltas ganó cada uno. */
  protected readonly ganadas = computed(() => {
    const { a, b, vueltas } = this.versus();
    return {
      a: vueltas.filter((vuelta) => vuelta.masRapido === a.numero).length,
      b: vueltas.filter((vuelta) => vuelta.masRapido === b.numero).length,
    };
  });

  protected elegirA(valor: string): void {
    this.elegir.emit([Number(valor), this.versus().b.numero]);
  }

  protected elegirB(valor: string): void {
    this.elegir.emit([this.versus().a.numero, Number(valor)]);
  }

  /** Qué lado gana el renglón ('a', 'b' o null si empatan, falta un dato o no se compara). */
  protected ganador(renglon: Renglon): 'a' | 'b' | null {
    if (!renglon.gana || renglon.a === null || renglon.b === null || renglon.a === renglon.b) return null;
    const aEsMenor = renglon.a < renglon.b;
    return (renglon.gana === 'menor') === aEsMenor ? 'a' : 'b';
  }

  /** El tiempo de sector es el mejor de la sesión (violeta, como en la TV). */
  protected esRecord(renglon: Renglon, valor: number | null): boolean {
    if (renglon.sector === undefined || valor === null) return false;
    return valor === this.versus().mejoresSectoresSesion[renglon.sector];
  }

  protected acronimo(numero: number | null): string {
    const { a, b } = this.versus();
    if (numero === a.numero) return a.acronimo ?? String(a.numero);
    if (numero === b.numero) return b.acronimo ?? String(b.numero);
    return '—';
  }
}

function sectorDe(vuelta: { sector1: number | null; sector2: number | null; sector3: number | null } | null, indice: number): number | null {
  if (!vuelta) return null;
  return [vuelta.sector1, vuelta.sector2, vuelta.sector3][indice];
}
