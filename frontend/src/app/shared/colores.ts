/**
 * Paleta para los gráficos, pensada para el fondo oscuro de la app.
 * Cada piloto toma siempre el mismo color (según su id), así se lo reconoce
 * igual en la evolución, la tendencia y la comparativa.
 */
export const PALETA = [
  '#e50914', // rojo (acento de la app)
  '#2f7cf6', // azul
  '#f5c518', // amarillo
  '#2ecc71', // verde
  '#b06cff', // violeta
  '#ff8c1a', // naranja
  '#1fd1c1', // turquesa
  '#ff5fa2', // rosa
  '#c9d1d9', // gris claro
  '#8bc34a', // lima
] as const;

/** Color estable para un id (piloto o equipo). */
export function colorDe(id: number): string {
  return PALETA[Math.abs(id) % PALETA.length];
}

/** Color por posición en una lista (para series sin id). */
export function colorPorIndice(indice: number): string {
  return PALETA[indice % PALETA.length];
}
