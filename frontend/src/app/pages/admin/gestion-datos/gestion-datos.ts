import { Component, signal,inject, effect, untracked,computed  } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AbmTabla, CampoAbm, ColumnaAbm } from './../abm-tabla/abm-tabla';
import { sign } from 'chart.js/helpers';
import { AbmService, RegistroGenerico } from '../../../core/services/abm.service';   // ← tu ruta
import { mensajeError } from '../../../core/errores/error';    

type PestanaGestion = 'categorias' | 'pilotos' | 'equipos' | 'calendario';


const ESTADOS_EVENTO = ['PROGRAMADO', 'EN_CURSO', 'FINALIZADO', 'CANCELADO'].map((estado) => ({
  valor: estado,
  texto: estado,
}));

const ESTADOS_RESULTADO = ['FINALIZADO', 'ABANDONO', 'DESCALIFICADO', 'NO_LARGO'].map((estado) => ({
  valor: estado,
  texto: estado,
}));

const TIPOS_SESION = ['PRACTICA', 'CLASIFICACION', 'SPRINT_CLASIFICACION', 'SPRINT', 'SERIE', 'CARRERA'].map(
  (tipo) => ({ valor: tipo, texto: tipo }),
);

/** Fecha-hora UTC (ISO) -> texto corto en la hora local del dispositivo. */
const formatearFechaHora = (valor: unknown) =>
  valor ? new Date(String(valor)).toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'short' }) : '';


@Component({
  imports: [FormsModule, AbmTabla],
  selector: 'app-gestion-datos',
  styleUrl: './gestion-datos.css',
  templateUrl: './gestion-datos.html',
})
export class GestionDatos {

  private readonly abmService = inject(AbmService);

  protected readonly pestanas: { id: PestanaGestion; texto: string }[] = [
    { id: 'calendario', texto: 'Temporadas y eventos' },
    { id: 'pilotos', texto: 'Pilotos' },
    { id: 'equipos', texto: 'Equipos' },
    { id: 'categorias', texto: 'Categorías' },
  ];

  protected readonly pestanaActiva = signal<PestanaGestion>('pilotos');
  protected readonly errorGeneral = signal<string | null>(null);

  protected readonly categorias = signal<RegistroGenerico[]>([]);
  protected readonly categoriaId = signal<number | null>(null);
  protected readonly temporadaId = signal<number | null>(null);
  protected readonly anioTemporada = signal<number | null>(null);
  protected readonly eventoId = signal<number | null>(null);
  protected readonly sesionId = signal<number | null>(null);
  protected readonly etapaId = signal<number | null>(null);


  protected readonly textoBusquedaPiloto = signal('');
  protected readonly textoBusquedaEquipo = signal('');

  private readonly listaPilotos = signal<RegistroGenerico[]>([]);
  private readonly listaEquipos = signal<RegistroGenerico[]>([]);
  private readonly listaTripulaciones = signal<RegistroGenerico[]>([]);

  private readonly opcionesPilotos = computed(() =>
    this.listaPilotos().map((piloto) => ({
      valor: piloto.id,
      texto: `${piloto['apellido']}, ${piloto['nombre'] ?? ''}${piloto['codigo'] ? ' (' + piloto['codigo'] + ')' : ''}`,
    })),
  );
  private readonly opcionesEquipos = computed(() =>
    this.listaEquipos().map((equipo) => ({ 
      valor: equipo.id, texto: String(equipo['nombre']) 
    })),
  );
  private readonly opcionesTripulaciones = computed(() =>
    this.listaTripulaciones().map((tripulacion) => ({
      valor: tripulacion.id,
      texto: `#${tripulacion['numero'] ?? '-'} ${tripulacion['piloto']} / ${tripulacion['copiloto']}`,
    })),
  );

    constructor() {
    this.cargarCategorias();
    this.cargarOpciones();
    effect(() => {
      const categoriaId = this.categoriaId();
      const anio = this.anioTemporada();
      if (categoriaId) {
        untracked(() =>
          this.abmService.listar('tripulaciones', { categoriaId, anio }).subscribe({
            next: (tripulaciones) => this.listaTripulaciones.set(tripulaciones),
            error: (errorHttp) => this.errorGeneral.set(mensajeError(errorHttp)),
          }),
        );
      }
    });
  }

  /** Carga las categorías y, si la elegida ya no existe o todavía no hay ninguna, elige la primera. */
  cargarCategorias(): void {
    this.abmService.listar('categorias').subscribe({
      next: (categorias) => {
        this.categorias.set(categorias);
        const laElegidaSigueExistiendo = categorias.some((categoria) => categoria.id === this.categoriaId());
        if (!laElegidaSigueExistiendo) {
          this.elegirCategoria(categorias.length ? categorias[0].id : null);
        }
      },
      error: (errorHttp) => this.errorGeneral.set(mensajeError(errorHttp)),
    });
  }

  cargarOpciones(): void {
    this.abmService.listar('pilotos').subscribe({
      next: (pilotos) => this.listaPilotos.set(pilotos),
      error: (errorHttp) => this.errorGeneral.set(mensajeError(errorHttp)),
    });
    this.abmService.listar('equipos').subscribe({
      next: (equipos) => this.listaEquipos.set(equipos),
      error: (errorHttp) => this.errorGeneral.set(mensajeError(errorHttp)),
    });
  }

  protected elegirCategoria(categoriaId: number | null): void {
    this.categoriaId.set(categoriaId);
    this.temporadaId.set(null);   
    this.deseleccionarEvento();
  }

  protected elegirTemporada(temporada: RegistroGenerico): void {
    this.temporadaId.set(temporada.id);
    this.deseleccionarEvento();
  }

  protected elegirEvento(evento: RegistroGenerico): void {
    this.eventoId.set(evento.id);
  }

  /** Al subir de nivel (cambiar de categoría o temporada), lo que estaba más abajo deja de tener sentido. */
  private deseleccionarEvento(): void {
    this.eventoId.set(null);
  }

  protected readonly columnasTemporadas: ColumnaAbm[] = [
    { clave: 'anio', etiqueta: 'Año' },
    { clave: 'nombre', etiqueta: 'Nombre' },
    { clave: 'fechaInicio', etiqueta: 'Desde' },
    { clave: 'fechaFin', etiqueta: 'Hasta' },
  ];
  protected readonly camposTemporadas: CampoAbm[] = [
    { clave: 'anio', etiqueta: 'Año', tipo: 'numero', requerido: true, porDefecto: new Date().getFullYear() },
    { clave: 'nombre', etiqueta: 'Nombre', tipo: 'texto', ayuda: 'Si se deja vacío: "SIGLAS AÑO"' },
    { clave: 'fechaInicio', etiqueta: 'Fecha de inicio', tipo: 'fecha' },
    { clave: 'fechaFin', etiqueta: 'Fecha de fin', tipo: 'fecha' },
  ];

  protected readonly columnasEventos: ColumnaAbm[] = [
    { clave: 'numeroFecha', etiqueta: '#' },
    { clave: 'nombre', etiqueta: 'Evento' },
    { clave: 'circuito', etiqueta: 'Circuito / sede' },
    { clave: 'inicio', etiqueta: 'Inicio', formato: (registro) => formatearFechaHora(registro['inicio']) },
    { clave: 'estado', etiqueta: 'Estado' },
    // Un evento con id externo vino de una sincronización con una API; si no, se cargó a mano
    { clave: 'idExterno', etiqueta: 'Origen', formato: (registro) => (registro['idExterno'] ? 'API' : 'Manual') },
  ];
  protected readonly camposEventos: CampoAbm[] = [
    { clave: 'nombre', etiqueta: 'Nombre', tipo: 'texto', requerido: true },
    { clave: 'nombreCorto', etiqueta: 'Nombre corto', tipo: 'texto' },
    { clave: 'numeroFecha', etiqueta: 'Número de fecha', tipo: 'numero' },
    { clave: 'circuito', etiqueta: 'Circuito / sede', tipo: 'texto' },
    { clave: 'ciudad', etiqueta: 'Ciudad', tipo: 'texto' },
    { clave: 'pais', etiqueta: 'País', tipo: 'texto' },
    {
      clave: 'zonaHoraria',
      etiqueta: 'Zona horaria de la sede',
      tipo: 'texto',
      porDefecto: 'America/Argentina/Buenos_Aires',
      ayuda: 'Formato IANA',
    },
    { clave: 'inicio', etiqueta: 'Inicio (hora de este dispositivo)', tipo: 'fechahora', requerido: true },
    { clave: 'fin', etiqueta: 'Fin (hora de este dispositivo)', tipo: 'fechahora' },
    { clave: 'estado', etiqueta: 'Estado', tipo: 'select', opciones: ESTADOS_EVENTO, porDefecto: 'PROGRAMADO' },
  ];

  protected readonly columnasSesiones: ColumnaAbm[] = [
    { clave: 'nombre', etiqueta: 'Sesión' },
    { clave: 'tipo', etiqueta: 'Tipo' },
    { clave: 'inicio', etiqueta: 'Inicio', formato: (registro) => formatearFechaHora(registro['inicio']) },
    { clave: 'estado', etiqueta: 'Estado' },
  ];
  protected readonly camposSesiones: CampoAbm[] = [
    { clave: 'nombre', etiqueta: 'Nombre', tipo: 'texto', requerido: true },
    { clave: 'tipo', etiqueta: 'Tipo', tipo: 'select', requerido: true, opciones: TIPOS_SESION, porDefecto: 'CARRERA' },
    { clave: 'numero', etiqueta: 'Número', tipo: 'numero' },
    { clave: 'inicio', etiqueta: 'Inicio', tipo: 'fechahora' },
    { clave: 'fin', etiqueta: 'Fin', tipo: 'fechahora' },
    { clave: 'vueltasProgramadas', etiqueta: 'Vueltas programadas', tipo: 'numero' },
    { clave: 'distancia', etiqueta: 'Distancia (km)', tipo: 'decimal' },
    { clave: 'estado', etiqueta: 'Estado', tipo: 'select', opciones: ESTADOS_EVENTO, porDefecto: 'PROGRAMADO' },
  ];

  protected readonly columnasEtapas: ColumnaAbm[] = [
    { clave: 'numero', etiqueta: '#' },
    { clave: 'nombre', etiqueta: 'Tramo' },
    { clave: 'distancia', etiqueta: 'Km' },
    { clave: 'clasificacionGeneral', etiqueta: 'General', formato: (registro) => (registro['clasificacionGeneral'] ? 'Sí' : '') },
  ];
  protected readonly camposEtapas: CampoAbm[] = [
    { clave: 'nombre', etiqueta: 'Nombre', tipo: 'texto', requerido: true },
    { clave: 'numero', etiqueta: 'Número', tipo: 'numero' },
    { clave: 'dia', etiqueta: 'Día', tipo: 'numero' },
    { clave: 'distancia', etiqueta: 'Distancia (km)', tipo: 'decimal' },
    { clave: 'inicio', etiqueta: 'Largada', tipo: 'fechahora' },
    { clave: 'estado', etiqueta: 'Estado', tipo: 'select', opciones: ESTADOS_EVENTO, porDefecto: 'PROGRAMADO' },
    { clave: 'clasificacionGeneral', etiqueta: 'Es la clasificación general del rally', tipo: 'checkbox', soloAlta: true },
  ];

  protected readonly columnasResultadosSesion: ColumnaAbm[] = [
    { clave: 'posicion', etiqueta: 'Pos' },
    { clave: 'piloto', etiqueta: 'Piloto' },
    { clave: 'equipo', etiqueta: 'Equipo' },
    { clave: 'puntos', etiqueta: 'Pts' },
    { clave: 'estado', etiqueta: 'Estado' },
  ];
  // Es un computed (y no una constante) porque las opciones de los selects se cargan después, desde el backend
  protected readonly camposResultadosSesion = computed<CampoAbm[]>(() => [
    { clave: 'pilotoId', etiqueta: 'Piloto', tipo: 'select', requerido: true, opciones: this.opcionesPilotos(), soloAlta: true },
    { clave: 'equipoId', etiqueta: 'Equipo', tipo: 'select', requerido: true, opciones: this.opcionesEquipos() },
    { clave: 'posicion', etiqueta: 'Posición', tipo: 'numero' },
    { clave: 'numero', etiqueta: 'Número de auto', tipo: 'numero' },
    { clave: 'puntos', etiqueta: 'Puntos', tipo: 'decimal' },
    { clave: 'tiempo', etiqueta: 'Tiempo (segundos)', tipo: 'decimal' },
    { clave: 'diferencia', etiqueta: 'Diferencia (segundos)', tipo: 'decimal' },
    { clave: 'vueltas', etiqueta: 'Vueltas', tipo: 'numero' },
    { clave: 'mejorVuelta', etiqueta: 'Mejor vuelta (segundos)', tipo: 'decimal' },
    { clave: 'estado', etiqueta: 'Estado', tipo: 'select', opciones: ESTADOS_RESULTADO, porDefecto: 'FINALIZADO' },
    { clave: 'motivoAbandono', etiqueta: 'Motivo de abandono', tipo: 'texto' },
  ]);

  protected readonly columnasResultadosEtapa: ColumnaAbm[] = [
    { clave: 'posicion', etiqueta: 'Pos' },
    { clave: 'tripulacion', etiqueta: 'Tripulación' },
    { clave: 'tiempo', etiqueta: 'Tiempo (s)' },
    { clave: 'puntos', etiqueta: 'Pts' },
    { clave: 'estado', etiqueta: 'Estado' },
  ];
  protected readonly camposResultadosEtapa = computed<CampoAbm[]>(() => [
    {
      clave: 'tripulacionId',
      etiqueta: 'Tripulación',
      tipo: 'select',
      requerido: true,
      opciones: this.opcionesTripulaciones(),
      soloAlta: true,
      ayuda: 'Debe ser de la misma categoría y temporada que el rally',
    },
    { clave: 'posicion', etiqueta: 'Posición', tipo: 'numero' },
    { clave: 'tiempo', etiqueta: 'Tiempo (segundos)', tipo: 'decimal' },
    { clave: 'diferencia', etiqueta: 'Diferencia (segundos)', tipo: 'decimal' },
    { clave: 'puntos', etiqueta: 'Puntos', tipo: 'decimal' },
    { clave: 'estado', etiqueta: 'Estado', tipo: 'select', opciones: ESTADOS_RESULTADO, porDefecto: 'FINALIZADO' },
    { clave: 'motivoAbandono', etiqueta: 'Motivo de abandono', tipo: 'texto' },
  ]);
  
  
  protected readonly columnasPilotos: ColumnaAbm[] = [
    { clave: 'apellido', etiqueta: 'Apellido' },
    { clave: 'nombre', etiqueta: 'Nombre' },
    { clave: 'codigo', etiqueta: 'Código' },
    { clave: 'numero', etiqueta: '#' },
    { clave: 'nacionalidad', etiqueta: 'Nacionalidad' },
    {
      clave: 'idExterno',
      etiqueta: 'Origen',
      formato: (registro) => (registro['idExterno'] ? String(registro['idExterno']).split(':')[0] : 'Manual'),
    },
  ];
  protected readonly camposPilotos: CampoAbm[] = [
    { clave: 'nombre', etiqueta: 'Nombre', tipo: 'texto' },
    { clave: 'apellido', etiqueta: 'Apellido', tipo: 'texto', requerido: true },
    { clave: 'codigo', etiqueta: 'Código (3 letras)', tipo: 'texto' },
    { clave: 'numero', etiqueta: 'Número', tipo: 'numero' },
    { clave: 'fechaNacimiento', etiqueta: 'Fecha de nacimiento', tipo: 'fecha' },
    { clave: 'nacionalidad', etiqueta: 'Nacionalidad', tipo: 'texto' },
    { clave: 'pais', etiqueta: 'País', tipo: 'texto' },
    { clave: 'fotoUrl', etiqueta: 'URL de la foto', tipo: 'texto' },
    { clave: 'fechaInicioCarrera', etiqueta: 'Inicio de carrera', tipo: 'fecha' },
    { clave: 'fechaFinCarrera', etiqueta: 'Retiro', tipo: 'fecha' },
  ];

  protected readonly columnasEquipos: ColumnaAbm[] = [
    { clave: 'nombre', etiqueta: 'Equipo' },
    { clave: 'nombreCorto', etiqueta: 'Marca' },
    { clave: 'nacionalidad', etiqueta: 'Nacionalidad' },
    { clave: 'sede', etiqueta: 'Sede' },
  ];
  protected readonly camposEquipos: CampoAbm[] = [
    { clave: 'nombre', etiqueta: 'Nombre', tipo: 'texto', requerido: true },
    { clave: 'nombreCorto', etiqueta: 'Marca / nombre corto', tipo: 'texto', ayuda: 'Se usa en las estadísticas por marca' },
    { clave: 'nacionalidad', etiqueta: 'Nacionalidad', tipo: 'texto' },
    { clave: 'sede', etiqueta: 'Sede', tipo: 'texto' },
    { clave: 'fechaFundacion', etiqueta: 'Fundación', tipo: 'fecha' },
    { clave: 'logoUrl', etiqueta: 'URL del logo', tipo: 'texto' },
  ];

  protected readonly columnasCategorias: ColumnaAbm[] = [
    { clave: 'siglas', etiqueta: 'Siglas' },
    { clave: 'nombre', etiqueta: 'Nombre' },
    { clave: 'paisOrigen', etiqueta: 'Origen' },
    { clave: 'activo', etiqueta: 'Activa', formato: (registro) => (registro['activo'] ? 'Sí' : 'No') },
  ];
  protected readonly camposCategorias: CampoAbm[] = [
    { clave: 'nombre', etiqueta: 'Nombre', tipo: 'texto', requerido: true },
    { clave: 'siglas', etiqueta: 'Siglas', tipo: 'texto', requerido: true, ayuda: 'Hasta 10 letras o números' },
    { clave: 'descripcion', etiqueta: 'Descripción', tipo: 'texto' },
    { clave: 'paisOrigen', etiqueta: 'País de origen', tipo: 'texto' },
    { clave: 'logoUrl', etiqueta: 'URL del logo', tipo: 'texto' },
    { clave: 'activo', etiqueta: 'Activa (visible en el calendario)', tipo: 'checkbox', porDefecto: true },
  ];
}
