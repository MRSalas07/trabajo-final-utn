import { EventoDto, Horario } from '../core/modelos';

export function diaLocal(horario: Horario | null): string | null {
  return horario ? horario.local.substring(0, 10) : null;
}

export function diasDelEvento(evento: EventoDto): string[] {
  const dias = [...evento.sesiones.map((sesion) => diaLocal(sesion.inicio)), ...evento.etapas.map((etapa) => diaLocal(etapa.inicio))]
    .filter((dia): dia is string => dia !== null);
  if (dias.length) return [...new Set(dias)].sort();

  const inicio = diaLocal(evento.inicio);
  if (!inicio) return [];
  const fin = diaLocal(evento.fin) ?? inicio;
  const resultado: string[] = [];
  for (const dia = new Date(`${inicio}T00:00:00Z`); resultado.length < 31; dia.setUTCDate(dia.getUTCDate() + 1)) {
    const clave = dia.toISOString().substring(0, 10);
    if (clave > fin) break;
    resultado.push(clave);
  }
  return resultado;
}
