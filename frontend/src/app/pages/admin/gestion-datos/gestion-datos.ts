import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AbmTabla, CampoAbm, ColumnaAbm } from './../abm-tabla/abm-tabla';
import { sign } from 'chart.js/helpers';


type PestanaGestion = 'categorias' | 'pilotos' | 'equipos';

@Component({
  imports: [FormsModule, AbmTabla],
  selector: 'app-gestion-datos',
  styleUrl: './gestion-datos.css',
  templateUrl: './gestion-datos.html',
})
export class GestionDatos {

  protected readonly pestanas: { id: PestanaGestion; texto: string }[] = [
    { id: 'pilotos', texto: 'Pilotos' },
    { id: 'equipos', texto: 'Equipos' },
    { id: 'categorias', texto: 'Categorías' },
  ];

  protected readonly pestanaActiva = signal<PestanaGestion>('pilotos');
  protected readonly textoBusquedaPiloto = signal('');
  protected readonly textoBusquedaEquipo = signal('');

  
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
