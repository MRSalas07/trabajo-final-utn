import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Trazos de cada ícono (SVG de 24x24, línea de 2 px, color = color del texto). */
const TRAZOS: Record<string, string[]> = {
  inicio: ['M3 11.5 12 4l9 7.5', 'M5.5 10v10h5v-6h3v6h5V10'],
  calendario: ['M4 6.5h16v14H4z', 'M4 10.5h16', 'M8.5 3.5v4', 'M15.5 3.5v4'],
  pilotos: ['M12 12.5a4 4 0 1 0 0-8 4 4 0 0 0 0 8z', 'M4.5 20.5c1.4-3.6 4.2-5.5 7.5-5.5s6.1 1.9 7.5 5.5'],
  equipos: ['M5.5 21V4', 'M5.5 4.5h12l-2.5 4 2.5 4h-12'],
  buscar: ['M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14z', 'M20 20l-4-4'],
  usuario: [
    'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z',
    'M12 13a3 3 0 1 0 0-6 3 3 0 0 0 0 6z',
    'M6.6 18.2c1.4-1.9 3.3-2.8 5.4-2.8s4 .9 5.4 2.8',
  ],
  menu: ['M4 7h16', 'M4 12h16', 'M4 17h16'],
  cerrar: ['M6 6l12 12', 'M18 6 6 18'],
  reloj: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z', 'M12 7.5V12l3 2'],
  izquierda: ['M15 5.5 8.5 12l6.5 6.5'],
  derecha: ['M9 5.5 15.5 12 9 18.5'],
  admin: ['M12 3.5 19 6v5.5c0 4.2-2.9 7.6-7 9-4.1-1.4-7-4.8-7-9V6z', 'M9 12l2 2 4-4'],
  flecha: ['M5 12h14', 'M13 6l6 6-6 6'],
};

/**
 * Ícono SVG propio (sin librerías de íconos). Hereda el color del texto.
 * Uso: <app-icono nombre="calendario" /> · decorativo por defecto (aria-hidden).
 */
@Component({
  selector: 'app-icono',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { 'aria-hidden': 'true', class: 'icono' },
  template: `
    <svg [attr.width]="tamanio()" [attr.height]="tamanio()" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         stroke-width="2" stroke-linecap="round" stroke-linejoin="round" focusable="false">
      @for (d of trazos(); track $index) {
        <path [attr.d]="d" />
      }
    </svg>
  `,
  styles: `
    :host { display: inline-flex; flex: none; line-height: 0; }
  `,
})
export class Icono {
  readonly nombre = input.required<string>();
  readonly tamanio = input(20);
  protected readonly trazos = computed(() => TRAZOS[this.nombre()] ?? []);
}
