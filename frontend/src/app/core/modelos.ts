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
