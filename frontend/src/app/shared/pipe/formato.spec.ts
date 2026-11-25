import { EstadoPipe, HoraPipe, HoraSedePipe, TiempoPipe } from './formato';

/** Los pipes son funciones puras: se prueban creándolos y llamando a transform(). */
describe('pipes de formato', () => {
  describe('TiempoPipe', () => {
    const tiempo = new TiempoPipe();

    it('muestra tiempos de vuelta como en la TV', () => {
      expect(tiempo.transform(95.123)).toBe('1:35.123');
      expect(tiempo.transform(30.5)).toBe('30.500');
    });

    it('muestra horas en carreras largas', () => {
      expect(tiempo.transform(5504.742)).toBe('1:31:44.742');
    });

    it('en modo diferencia agrega el "+" y oculta el cero', () => {
      expect(tiempo.transform(0.25, true)).toBe('+0.250');
      expect(tiempo.transform(0, true)).toBe('—');
    });

    it('sin dato muestra una raya', () => {
      expect(tiempo.transform(null)).toBe('—');
      expect(tiempo.transform(undefined)).toBe('—');
    });
  });

  describe('HoraPipe', () => {
    const hora = new HoraPipe();
    const largada = '2026-03-08T04:00:00Z';

    it('convierte un instante UTC a la zona horaria del usuario', () => {
      expect(hora.transform(largada, 'hora', 'America/Argentina/Buenos_Aires')).toBe('01:00');
      expect(hora.transform(largada, 'hora', 'Europe/Madrid')).toBe('05:00');
    });

    it('con el formato "segundos" muestra también los segundos', () => {
      expect(hora.transform('2026-03-08T04:00:37Z', 'segundos', 'America/Argentina/Buenos_Aires')).toBe('01:00:37');
    });

    it('sin dato muestra una raya', () => {
      expect(hora.transform(null)).toBe('—');
    });
  });

  describe('HoraSedePipe', () => {
    it('muestra la hora local de la sede tal como viene (sin convertir)', () => {
      expect(new HoraSedePipe().transform('2026-03-08T15:00:00')).toBe('08/03 15:00');
    });
  });

  describe('EstadoPipe', () => {
    const estado = new EstadoPipe();

    it('traduce los estados del backend y deja igual los desconocidos', () => {
      expect(estado.transform('NO_LARGO')).toBe('No largó');
      expect(estado.transform('EN_CURSO')).toBe('En vivo');
      expect(estado.transform('OTRO')).toBe('OTRO');
      expect(estado.transform(null)).toBe('—');
    });
  });
});
