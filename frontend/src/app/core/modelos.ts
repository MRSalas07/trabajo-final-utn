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

export interface EventoDto {
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
    /** Diferencia sede - usuario en minutos (positiva = la sede va adelantada). */
    diferenciaSedeMinutos: number | null;
    diferenciaSedeTexto: string | null;
    estado: EstadoEvento; 
    sesiones: Sesion[]; 
    etapas: Etapa[];
}

export interface EventoResumen { 
    id: number; 
    nombre: string; 
    anio: number; 
    categoria: CategoriaResumen; 
}

export interface ResultadoSesion {
    posicion: number | null; 
    numero: number | null; 
    pilotoId: number; 
    piloto: string; 
    codigo: string | null;
    equipoId: number; 
    equipo: string; 
    puntos: number | null; 
    tiempo: number | null; 
    diferencia: number | null;
    vueltas: number | null; 
    mejorVuelta: number | null; 
    estado: string; 
    motivoAbandono: string | null;
}

export interface TablaSesion { 
    evento: EventoResumen; 
    sesion: Sesion; 
    resultados: ResultadoSesion[]; 
}

export interface ResultadoEtapa {
    posicion: number | null;
     numero: number | null; 
     pilotoId: number; 
     piloto: string;
    copilotoId: number; 
    copiloto: string; 
    equipoId: number; 
    equipo: string;
    tiempo: number | null; 
    diferencia: number | null; 
    puntos: number | null; 
    estado: string; 
    motivoAbandono: string | null;
}
export interface TablaEtapa { 
    evento: EventoResumen; 
    etapa: Etapa; resultados: 
    ResultadoEtapa[]; }


export interface Pagina<T> {            // respuesta paginada genérica del backend
    contenido: T[]; 
    pagina: number; 
    tamanio: number; 
    totalElementos: number; 
    totalPaginas: number;
}
export interface PilotoResumen { 
    id: number; 
    nombre: string | null; 
    apellido: string; 
    codigo: string | null; 
    numero: number | null;
    nacionalidad: string | null;
    pais: string | null; 
    fotoUrl: string | null; 
    equipoId: number | null; 
    equipoActual: string | null; 
}
export interface Palmares { 
    categoria: string; 
    carreras: number; 
    victorias: number; 
    podios: number; 
    poles: number;
    campeonatos: number; 
    puntosTotales: number; 
    abandonos: number; 
}
export interface Participacion { 
    anio: number; categoria: 
    string; equipoId: number; 
    equipo: string; 
    pilotoId: number;
    piloto: string | null; 
    numero: number | null; 
    rol: string; 
}
export interface ResultadoReciente { 
    eventoId: number; 
    evento: string; 
    categoria: string; 
    anio: number; 
    fecha: string;
    posicion: number | null; 
    estado: string | null; 
    puntos: number | null; 
    equipo: string; 
    piloto: string; 
}

export interface Piloto {
    id: number;
    nombre: string | null;
    apellido: string;
    codigo: string | null;
    numero: number | null;
    nacionalidad: string | null;
    pais: string | null;
    fechaNacimiento: string | null;
    edad: number | null;
    fotoUrl: string | null;
}


export interface PerfilPiloto {
    piloto: Piloto;
    palmares: Palmares[];
    trayectoria: Participacion[];
    ultimosResultados: ResultadoReciente[];
}

export interface EquipoResumen {
    id: number;
    nombre: string;
    nombreCorto: string | null;
    nacionalidad: string | null;
    logoUrl: string | null;
}

export interface PerfilEquipo {
    equipo: EquipoResumen;
    sede: string | null;
    fechaFundacion: string | null;
    categorias: string[];
    plantel: Participacion[];
    estadisticas: { eventos: number; victorias: number; podios: number; puntos: number };
    ultimosResultados: ResultadoReciente[];
}

export interface ResumenSincronizacion {
    categoria: string; 
    anio: number; 
    fuente: string;
    eventosProcesados: number; 
    eventosOmitidos: number; 
    sesionesProcesadas: number; 
    etapasProcesadas: number;
    resultadosProcesados: number; 
    pilotosNuevos: number; 
    equiposNuevos: number; 
    avisos: string[]; 
    duracionMs: number;
}

export interface TrabajoSincronizacion {  
    id: string; categoria: 
    string; anio: number;
    estado: 'EN_COLA' | 'EN_PROCESO' | 'COMPLETADO' | 'FALLIDO';
    creado: string; 
    finalizado: string | null; 
    resumen: ResumenSincronizacion | null; 
    error: string | null;
}
export interface FuenteDisponible { 
    categoria: string; 
    proveedor: string; 
    habilitada: boolean; 
}


