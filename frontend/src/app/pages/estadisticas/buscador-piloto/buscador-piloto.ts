import { Component, DestroyRef, inject, input, output, signal } from '@angular/core';
import { Subject, catchError, debounceTime, distinctUntilChanged, map, of, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PilotoService } from '../../../core/services/piloto.service';
import { Pagina, PilotoResumen } from '../../../core/modelos';

/** Piloto elegido en el buscador. */
export interface PilotoElegido {
  id: number;
  nombre: string;
}

/** Letras mínimas para buscar (con menos, los resultados serían demasiados). */
const LETRAS_MINIMAS = 3;

/**
 * Buscador de pilotos de una categoría, para el histórico (puede ser cualquier piloto de cualquier
 * temporada, no solo los de la temporada elegida). Usa GET /api/pilotos con filtroCategoria.
 */
@Component({
  selector: 'app-buscador-piloto',
  template: `
    <div class="buscador">
      <label>{{ etiqueta() }}
        <input type="search" [placeholder]="'Buscar piloto (mín. ' + letrasMinimas + ' letras)'" autocomplete="off"
               [value]="texto()" (input)="escribir($any($event.target).value)" />
      </label>
      @if (resultados().length) {
        <ul class="sugerencias" role="listbox" [attr.aria-label]="'Resultados para ' + etiqueta()">
          @for (p of resultados(); track p.id) {
            <li><button type="button" role="option" (click)="elegir(p)">
              {{ nombreDe(p) }} @if (p.codigo) { <span class="suave chico">{{ p.codigo }}</span> }
            </button></li>
          }
        </ul>
      } @else if (sinResultados()) {
        <p class="suave chico">No se encontraron pilotos.</p>
      }
    </div>
  `,
  styles: `
    .buscador { position: relative; display: flex; flex-direction: column; gap: 0.25rem; }
    label { display: flex; flex-direction: column; gap: 0.25rem; font-size: 0.85rem; }
    .sugerencias { list-style: none; margin: 0; padding: 0.25rem; position: absolute; top: 100%; left: 0; right: 0; z-index: 5;
                   background: var(--superficie-2); border: 1px solid var(--borde); border-radius: var(--radio-chico); max-height: 260px; overflow-y: auto; }
    .sugerencias button { width: 100%; text-align: left; background: none; border: none; min-height: 36px; padding: 0.35rem 0.5rem; color: var(--texto); cursor: pointer; }
    .sugerencias button:hover, .sugerencias button:focus-visible { background: var(--borde); }
  `,
})
export class BuscadorPiloto {
  private readonly pilotoService = inject(PilotoService);

  readonly siglas = input.required<string>();
  readonly etiqueta = input('Piloto');
  readonly elegido = output<PilotoElegido>();

  protected readonly letrasMinimas = LETRAS_MINIMAS;
  protected readonly texto = signal('');
  protected readonly resultados = signal<PilotoResumen[]>([]);
  protected readonly sinResultados = signal(false);
  private readonly busquedas = new Subject<string>();

  constructor() {
    this.busquedas.pipe(
      map((t: string) => t.trim()),
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((t: string) => t.length < LETRAS_MINIMAS
        ? of(null)
        : this.pilotoService.buscarPilotos({ filPiloto: t, filtroCategoria: this.siglas(), filtroAnio: null, paginado: 0, cantidad: 10 })
            .pipe(map((pagina: Pagina<PilotoResumen>) => pagina.contenido), catchError(() => of([] as PilotoResumen[])))),
      takeUntilDestroyed(inject(DestroyRef)),
    ).subscribe((lista: PilotoResumen[] | null) => {
      this.resultados.set(lista ?? []);
      this.sinResultados.set(lista !== null && lista.length === 0);
    });
  }

  protected escribir(valor: string): void {
    this.texto.set(valor);
    this.busquedas.next(valor);
  }

  protected elegir(p: PilotoResumen): void {
    const nombre = this.nombreDe(p);
    this.elegido.emit({ id: p.id, nombre });
    this.texto.set('');
    this.resultados.set([]);
    this.sinResultados.set(false);
    this.busquedas.next('');
  }

  protected nombreDe(p: PilotoResumen): string {
    return [p.nombre, p.apellido].filter(Boolean).join(' ');
  }
}
