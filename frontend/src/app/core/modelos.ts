/* DTos para el modelo del backend */
export interface Horario {
    utc: string;
    local: string;
    zona: string;
    offset: string;
}

export interface CategoriaResumen {
    id: number;
    siglas: string;
    nombre: string;
}

export interface Categoria {
    id: number;
    nombre: string;
    siglas: string;
    descripcion: string | null;
    paisOrigen: string | null;
    logoUrl: string | null;
    activo: boolean;
}

export interface Salud {
    estado: string;
    hora: string;
    telemetria: boolean;
    wrcAutomatico: boolean;
}

export interface ErrorApi {
    timestamp: string;
    status: number;
    error: string;
    detalles: string[];
}

export interface Temporada {
    id: number; 
    categoriaId: number; 
    categoria: string; 
    anio: number; 
    nombre: string;
    fechaInicio: string | null; 
    fechaFin: string | null;
}

export type EstadoEvento = 'PROGRAMADO' | 'EN_CURSO' | 'FINALIZADO' | 'CANCELADO';

export interface Sesion {
    id: number; 
    nombre: string; 
    tipo: string; 
    numero: number | null;
    inicio: Horario | null; 
    fin: Horario | null; 
    estado: EstadoEvento; 
    cantidadResultados: number;
}

export interface Etapa {
    id: number; 
    numero: number | null; 
    nombre: string; 
    dia: number | null; 
    distancia: number | null;
    inicio: Horario | null; 
    estado: EstadoEvento; 
    cantidadResultados: number; 
    clasificacionGeneral: boolean;
}

export interface Evento {
    id: number; 
    categoria: CategoriaResumen; 
    temporadaId: number; 
    anio: number; 
    numeroFecha: number | null;
    nombre: string; 
    nombreCorto: string | null; 
    pais: string | null; 
    ciudad: string | null; 
    circuito: string | null;
    zonaHorariaSede: string; 
    inicio: Horario | null; 
    fin: Horario | null; 
    inicioHoraSede: string | null;
    estado: EstadoEvento; 
    sesiones: Sesion[]; 
    etapas: Etapa[];
}

