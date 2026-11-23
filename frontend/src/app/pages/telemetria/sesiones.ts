import { SesionTelemetria } from '../../core/modelos';

/** OpenF1 tiene datos desde la temporada 2023. */
export const PRIMER_ANIO_OPENF1 = 2023;

/** Sesiones del mismo circuito seguidas (un fin de semana), para agruparlas en el selector. */
export interface GrupoSesiones {
  clave: string;
  titulo: string;
  sesiones: { sessionKey: number; texto: string }[];
}

const NOMBRES: Record<string, string> = {
  Race: 'Carrera',
  Sprint: 'Sprint',
  Qualifying: 'Clasificación',
  'Sprint Qualifying': 'Clasificación sprint',
  'Sprint Shootout': 'Clasificación sprint',
};

/** Nombre de la sesión en castellano ("Practice 2" -> "Práctica 2"); si no se conoce, el de OpenF1. */
export function nombreSesion(sesion: SesionTelemetria): string {
  return traducirSesion(sesion.nombre ?? sesion.tipo);
}

/** Traduce el nombre de sesión que usa OpenF1 ("Race", "Practice 1", "Day 2"...). */
export function traducirSesion(nombreOpenF1: string | null): string {
  const nombre = nombreOpenF1 ?? 'Sesión';
  const traducido = NOMBRES[nombre];
  if (traducido) return traducido;
  const practica = /^Practice (\d)$/.exec(nombre);
  if (practica) return `Práctica ${practica[1]}`;
  const ensayo = /^Day (\d)$/.exec(nombre);
  if (ensayo) return `Pretemporada · día ${ensayo[1]}`;
  return nombre;
}

/** "Interlagos · Brazil" (OpenF1 informa el país en inglés). */
export function lugarSesion(sesion: SesionTelemetria): string {
  return [sesion.circuito ?? sesion.ubicacion, sesion.pais].filter((parte) => !!parte).join(' · ') || 'Circuito sin informar';
}

const MEDIA_HORA = 30 * 60_000;
const CUATRO_HORAS = 4 * 3_600_000;

/**
 * Si OpenF1 ya liberó los datos de la sesión (media hora después del final; sin fin informado, se toma
 * inicio + 4 h). Es el mismo criterio que DatosOpenF1.motivoSinDatosLibres del backend.
 */
export function datosLiberados(sesion: SesionTelemetria, ahora = Date.now()): boolean {
  const fin = sesion.fin ? Date.parse(sesion.fin) : sesion.inicio ? Date.parse(sesion.inicio) + CUATRO_HORAS : NaN;
  return Number.isFinite(fin) && fin + MEDIA_HORA <= ahora;
}

/** Sesión que se muestra al entrar: la última carrera con datos liberados (si no hay, la última sesión liberada). */
export function sesionPorDefecto(sesiones: SesionTelemetria[]): SesionTelemetria | null {
  const liberadas = sesiones.filter((s) => datosLiberados(s));
  return liberadas.find((s) => s.nombre === 'Race') ?? liberadas[0] ?? sesiones[0] ?? null;
}

/** Agrupa las sesiones (ya ordenadas de la más reciente a la más vieja) por fin de semana, para el selector. */
export function agruparSesiones(sesiones: SesionTelemetria[], zona: string): GrupoSesiones[] {
  const formato = formatoFecha(zona);
  const grupos: GrupoSesiones[] = [];
  for (const sesion of sesiones) {
    const clave = `${sesion.circuito ?? ''}|${sesion.pais ?? ''}`;
    let grupo = grupos[grupos.length - 1];
    if (!grupo || grupo.clave !== clave) {
      grupo = { clave, titulo: lugarSesion(sesion), sesiones: [] };
      grupos.push(grupo);
    }
    const fecha = sesion.inicio ? formato.format(new Date(sesion.inicio)) : '';
    grupo.sesiones.push({ sessionKey: sesion.sessionKey, texto: fecha ? `${nombreSesion(sesion)} · ${fecha}` : nombreSesion(sesion) });
  }
  // Un circuito puede aparecer dos veces en el año (ej: ensayos de pretemporada y Gran Premio en Baréin):
  // con el índice, la clave de cada grupo es única para el @for
  return grupos.map((g, i) => ({ ...g, clave: `${g.clave}|${i}` }));
}

function formatoFecha(zona: string): Intl.DateTimeFormat {
  const opciones: Intl.DateTimeFormatOptions = { weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', hour12: false };
  try {
    return new Intl.DateTimeFormat('es-AR', { ...opciones, timeZone: zona });
  } catch {
    return new Intl.DateTimeFormat('es-AR', opciones);
  }
}
