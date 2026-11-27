import { TestBed } from '@angular/core/testing';
import { PilotoVersus, Versus } from '../../../../core/modelos';
import { VersusPilotos } from './versus-pilotos';

/**
 * El versus solo muestra lo que calculó el backend: marca en verde al mejor de cada renglón,
 * en violeta el mejor sector de la sesión, y avisa al padre cuando se elige otro piloto.
 */
describe('VersusPilotos', () => {
  function piloto(numero: number, acronimo: string, ultimaVuelta: number, sector1: number): PilotoVersus {
    return {
      numero, acronimo, nombre: null, equipo: null, color: '#3671C6', foto: null, posicion: numero === 1 ? 1 : 2,
      vueltas: 30, vueltaEnCurso: { vuelta: 31, segundos: 40, sector1: 30.6, velocidadI1: 230, sector2: null, velocidadI2: null },
      ultimaVuelta: { vuelta: 30, tiempo: ultimaVuelta, sector1, sector2: 41, sector3: 23, velocidadI1: 230, velocidadI2: 260, velocidadTrampa: 300 },
      mejorVuelta: null, mejoresSectores: [sector1, 41, 23], velocidadMaxima: 305, paradas: 1, tiempoEnBoxes: 24.5,
      neumatico: 'HARD', edadNeumatico: 12,
    };
  }

  const versus: Versus = {
    a: piloto(1, 'VER', 95.5, 30.2),
    b: piloto(4, 'NOR', 95.9, 30.4),
    mejoresSectoresSesion: [30.2, 40.5, 22.9],
    vueltas: [{ vuelta: 30, tiempoA: 95.5, tiempoB: 95.9, masRapido: 1, diferencia: '0.400' }],
  };

  async function crear() {
    const fixture = TestBed.createComponent(VersusPilotos);
    fixture.componentRef.setInput('versus', versus);
    fixture.componentRef.setInput('filas', []);
    await fixture.whenStable();
    return fixture;
  }

  function celdasDelRenglon(elemento: HTMLElement, etiqueta: string): HTMLElement[] {
    const fila = [...elemento.querySelectorAll('table.comparacion tr')]
      .find((tr) => tr.querySelector('th')?.textContent?.trim() === etiqueta);
    return [...(fila?.querySelectorAll('td') ?? [])] as HTMLElement[];
  }

  it('marca en verde al que hizo la mejor última vuelta', async () => {
    const fixture = await crear();

    const [verstappen, norris] = celdasDelRenglon(fixture.nativeElement, 'Última vuelta');

    expect(verstappen.classList).toContain('gana');
    expect(norris.classList).not.toContain('gana');
  });

  it('marca en violeta el sector que es el mejor de la sesión', async () => {
    const fixture = await crear();

    const [verstappen, norris] = celdasDelRenglon(fixture.nativeElement, 'Mejor S1');

    expect(verstappen.classList).toContain('record');
    expect(norris.classList).not.toContain('record');
  });

  it('muestra la vuelta en curso y quién ganó cada vuelta', async () => {
    const fixture = await crear();
    const texto = fixture.nativeElement.textContent as string;

    expect(texto).toContain('Vuelta 31 · 40 s');
    expect(texto).toContain('VER por 0.400 s');
  });

  it('al elegir otro piloto avisa al padre con los dos números', async () => {
    const fixture = await crear();
    const elegidos: number[][] = [];
    fixture.componentInstance.elegir.subscribe((numeros) => elegidos.push(numeros));

    fixture.componentRef.setInput('filas', [
      { numero: 1, acronimo: 'VER' }, { numero: 4, acronimo: 'NOR' }, { numero: 16, acronimo: 'LEC' },
    ]);
    await fixture.whenStable();
    const selector = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    selector.value = '16';
    selector.dispatchEvent(new Event('change'));

    expect(elegidos).toEqual([[16, 4]]);
  });
});
