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
    sesion: string;
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

export interface CargaSincronizacion {  
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

// ---------- MVP 4: estadísticas (espejo de EstadisticaDto.java) ----------
export interface FilaCampeonatoPiloto {
  posicion: number; pilotoId: number; piloto: string; codigo: string | null;
  equipoId: number | null; equipo: string | null; puntos: number;
  victorias: number; podios: number; poles: number; eventosDisputados: number; abandonos: number;
  mejorResultado: number | null; promedioPosicion: number | null; puntosPorEvento: number | null;
  victoriasEtapa: number; // solo WRC: tramos ganados
}
export interface FilaCampeonatoEquipo {
  posicion: number; equipoId: number; equipo: string; puntos: number;
  victorias: number; podios: number; pilotos: string[];
}
export interface Ronda { eventoId: number; numeroFecha: number | null; evento: string; }
export interface GanadorEvento {
  eventoId: number; numeroFecha: number | null; evento: string; fecha: string;
  pilotoId: number; piloto: string; equipo: string | null;
}
export interface ResumenTemporada {
  temporadaId: number; categoria: string; anio: number;
  eventosTotales: number; eventosDisputados: number;
  lider: FilaCampeonatoPiloto | null; equipoLider: FilaCampeonatoEquipo | null;
  ganadoresDistintos: number; ganadores: GanadorEvento[];
}
export interface SerieEvolucion {
  pilotoId: number; piloto: string; codigo: string | null;
  puntosAcumulados: number[]; posicionCampeonato: (number | null)[]; posicionEvento: (number | null)[];
}
export interface EvolucionCampeonato { rondas: Ronda[]; series: SerieEvolucion[]; }
export interface PuntoTendencia {
  numeroFecha: number | null; evento: string; posicion: number | null;
  estado: string | null; puntos: number | null; mediaMovil: number | null;
}
export interface TendenciaPiloto {
  pilotoId: number; piloto: string; anio: number; puntos: PuntoTendencia[];
  promedioPosicion: number | null; desviacionEstandar: number | null;
  pendiente: number | null; // negativa = el piloto mejora
  interpretacion: string | null; tasaFinalizacion: number | null;
  mejorResultado: number | null; peorResultado: number | null;
}
export interface ResumenComparado {
  pilotoId: number; piloto: string; equipo: string | null; puntos: number;
  victorias: number; podios: number; poles: number;
  promedioPosicion: number | null; eventosDisputados: number;
}
export interface ComparativaPilotos {
  anio: number; a: ResumenComparado; b: ResumenComparado;
  carrerasDelanteA: number; carrerasDelanteB: number;
  clasificacionesDelanteA: number; clasificacionesDelanteB: number;
  rondas: Ronda[]; diferenciaPuntos: number[]; // A − B, ronda a ronda
}
export interface PerfilCategoria {
  categoria: Categoria; temporadas: Temporada[]; temporadaActual: Temporada | null;
  resumen: ResumenTemporada | null; proximoEvento: EventoDto | null; ultimoEvento: EventoDto | null;
}
// ---------- MVP 6: equipos, marcas e histórico (espejo de EstadisticaDto.java) ----------
export type TipoGrupo = 'EQUIPO' | 'MARCA';

export interface FilaMarca {
  posicion: number; marca: string; puntos: number; victorias: number; podios: number;
  eventosConResultados: number; mejorResultado: number | null; equipos: string[]; pilotos: number;
}
export interface PuntoGrupo {
  numeroFecha: number | null; evento: string; puntos: number; puntosAcumulados: number;
  mejorPosicion: number | null; mediaMovilPuntos: number | null;
}
export interface TendenciaGrupo {
  tipo: TipoGrupo; clave: string; nombre: string; anio: number; fechas: PuntoGrupo[];
  promedioPuntos: number | null;
  pendiente: number | null; // regresión de los puntos por fecha: positiva = MEJORA (al revés que en pilotos)
  interpretacion: string | null; victorias: number; podios: number; pilotos: string[];
}
export interface SerieGrupo { clave: string; nombre: string; puntosAcumulados: number[]; puntosPorFecha: number[]; }
export interface EvolucionGrupos { tipo: TipoGrupo; rondas: Ronda[]; series: SerieGrupo[]; }
export interface TemporadaHistorica {
  anio: number; posicionCampeonato: number | null; participantes: number; puntos: number;
  victorias: number; podios: number; poles: number; eventosDisputados: number;
  promedioPosicion: number | null; puntosPorEvento: number | null; detalle: string | null;
}
export interface Historico {
  tipo: string; id: number; nombre: string; categoria: string; temporadas: TemporadaHistorica[];
  puntosTotales: number; victoriasTotales: number; podiosTotales: number;
  mejorPosicionCampeonato: number | null; variacionPuntosPorEvento: number | null;
}
export interface ComparativaHistorica {
  categoria: string; anios: number[]; a: Historico; b: Historico;
  posicionesA: (number | null)[]; posicionesB: (number | null)[];
  puntosA: (number | null)[]; puntosB: (number | null)[];
  carrerasDelanteA: number; carrerasDelanteB: number;
  temporadasDelanteA: number; temporadasDelanteB: number;
}

// ---------- Panel de administración: auditoría (espejo de AuditoriaDto.java) ----------
/** Quién creó y modificó por última vez un registro. Usuario null = lo cargó el sistema (sincronización, importación o scripts). */
export interface Auditoria {
  creadoPor: string | null;
  fechaCreacion: string | null;
  actualizadoPor: string | null;
  fechaActualizacion: string | null;
}

// ---------- MVP 7: telemetría F1 con datos de OpenF1 (espejo de TelemetriaDtos.java) ----------
/** Una sesión de F1 en OpenF1 (práctica, clasificación, sprint o carrera). inicio/fin: instantes UTC (ISO 8601). */
export interface SesionTelemetria {
  sessionKey: number;
  /** Nombre en OpenF1: "Race", "Qualifying", "Sprint", "Practice 1"... */
  nombre: string | null;
  /** Tipo en OpenF1: "Race", "Qualifying" o "Practice" (la sprint es de tipo "Race"). */
  tipo: string | null;
  inicio: string | null;
  fin: string | null;
  pais: string | null;
  circuito: string | null;
  ubicacion: string | null;
}
/** color: el del equipo en hexadecimal ("#3671C6"), o null si OpenF1 no lo informa. */
export interface PilotoTelemetria {
  numero: number; nombre: string | null; acronimo: string | null; equipo: string | null; color: string | null;
}
/** Tiempos de vuelta de un piloto: vueltas[i] -> tiempos[i] en segundos (sin las vueltas de salida de boxes). */
export interface SerieVueltas {
  numero: number; acronimo: string; color: string | null; vueltas: number[]; tiempos: number[];
}
/** Posición durante la sesión: minutos[i] desde el inicio -> posiciones[i]. Solo trae los cambios, en orden. */
export interface SeriePosiciones {
  numero: number; acronimo: string; color: string | null; minutos: number[]; posiciones: number[];
}
/**
 * Análisis de una sesión terminada. vueltas: solo los pilotos pedidos (hasta 6; por defecto, los mejor ubicados).
 * posiciones: todos los pilotos. aviso: por qué todavía no hay datos (la sesión no terminó o OpenF1 no los liberó), o null.
 */
export interface TelemetriaSesion {
  sesion: SesionTelemetria;
  pilotos: PilotoTelemetria[];
  vueltas: SerieVueltas[];
  posiciones: SeriePosiciones[];
  aviso: string | null;
}

// ---------- MVP 7: foto de la sesión en un instante (espejo de TelemetriaDtos.java) ----------
/** Fila de la torre en el instante de la foto. Tiempos en segundos. */
export interface FilaFoto {
  /** null = todavía sin posición. */
  posicion: number | null;
  numero: number;
  acronimo: string | null;
  nombre: string | null;
  equipo: string | null;
  color: string | null;
  /** Con el líder ("+12.345", "+1 VUELTA") o, si no es carrera, con la mejor vuelta del primero. null para el primero. */
  diferencia: string | null;
  /** Con el de adelante. */
  intervalo: string | null;
  vueltas: number | null;
  ultimaVuelta: number | null;
  /** En la clasificación, la mejor de la tanda en curso. */
  mejorVuelta: number | null;
  /** Tiene la vuelta más rápida (de la sesión o de la tanda) hasta ese momento. */
  vueltaRapida: boolean;
  /** Compuesto según OpenF1: SOFT, MEDIUM, HARD, INTERMEDIATE o WET. */
  neumatico: string | null;
  edadNeumatico: number | null;
  paradas: number;
}
/** Una vuelta con sus 3 sectores (segundos) y las velocidades (km/h) en los intermedios y en la trampa. */
export interface VueltaDetalle {
  vuelta: number | null;
  tiempo: number | null;
  sector1: number | null;
  sector2: number | null;
  sector3: number | null;
  velocidadI1: number | null;
  velocidadI2: number | null;
  velocidadTrampa: number | null;
}
/**
 * La vuelta que el piloto está dando en el instante de la foto: solo traen valor los sectores que ya completó
 * (con la velocidad de su intermedio). segundos: cuánto lleva de vuelta.
 */
export interface VueltaEnCurso {
  vuelta: number;
  segundos: number;
  sector1: number | null;
  velocidadI1: number | null;
  sector2: number | null;
  velocidadI2: number | null;
}
/** Un piloto del versus hasta el instante de la foto. */
export interface PilotoVersus {
  numero: number;
  acronimo: string | null;
  nombre: string | null;
  equipo: string | null;
  color: string | null;
  /** Dirección de la foto oficial: el navegador la carga directo desde Fórmula 1 (o null si OpenF1 no la tiene). */
  foto: string | null;
  posicion: number | null;
  vueltas: number | null;
  /** null si no está en pista (todavía no largó, abandonó o está en boxes). */
  vueltaEnCurso: VueltaEnCurso | null;
  ultimaVuelta: VueltaDetalle | null;
  mejorVuelta: VueltaDetalle | null;
  /** Su mejor tiempo en cada sector (pueden ser de vueltas distintas): [S1, S2, S3]. */
  mejoresSectores: (number | null)[];
  /** La velocidad más alta que marcó en los intermedios o en la trampa (km/h). */
  velocidadMaxima: number | null;
  paradas: number;
  /** Segundos en la calle de boxes en su última parada. */
  tiempoEnBoxes: number | null;
  neumatico: string | null;
  edadNeumatico: number | null;
}
/** La misma vuelta de los dos pilotos: quién fue más rápido (número de auto) y por cuánto ("0.234"). */
export interface VueltaComparada {
  vuelta: number; tiempoA: number; tiempoB: number; masRapido: number | null; diferencia: string;
}
/** Dos pilotos frente a frente. mejoresSectoresSesion: el mejor de cada sector entre todos (violeta, como en la TV). */
export interface Versus {
  a: PilotoVersus;
  b: PilotoVersus;
  mejoresSectoresSesion: (number | null)[];
  /** Las últimas vueltas que dieron los dos, la más reciente primero. */
  vueltas: VueltaComparada[];
}
/** Mensaje de la dirección de carrera (en inglés, como lo publica la FIA). */
export interface MensajeCarrera {
  fecha: string; vuelta: number | null; bandera: string | null; mensaje: string | null;
}
/** Temperaturas en °C, humedad en % y viento en m/s. */
export interface ClimaFoto {
  aire: number | null; pista: number | null; humedad: number | null; lluvia: boolean; viento: number | null;
}
/**
 * Cómo estaba la sesión en un instante: cada dato es el último que OpenF1 informó hasta ese momento.
 * En vivo el instante es "ahora"; en una sesión terminada, el que se pidió (por vuelta o por hora exacta).
 */
export interface FotoSesion {
  sesion: SesionTelemetria;
  enVivo: boolean;
  instante: string | null;
  /** Entre qué instantes hay datos (los límites de la línea de tiempo). */
  inicioDatos: string | null;
  finDatos: string | null;
  /** Vueltas que completó el líder (solo carrera y sprint). */
  vuelta: number | null;
  totalVueltas: number | null;
  /** Tanda de la clasificación ("Q1", "Q2", "Q3" o "SQ1"...) y cuándo empezó; null en el resto. */
  tanda: string | null;
  inicioTanda: string | null;
  filas: FilaFoto[];
  versus: Versus | null;
  /** Los más recientes primero. */
  mensajes: MensajeCarrera[];
  clima: ClimaFoto | null;
  /** Por qué no hay datos (ej: sesión en curso sin suscripción), o null. */
  aviso: string | null;
}
