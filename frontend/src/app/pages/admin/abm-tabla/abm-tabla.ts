import { Component, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { AbmService, Filtros, NombreRecurso, RegistroGenerico } from '../../../core/services/abm.service';
import { mensajeError } from '../../../core/errores/error';
import { FormsModule } from '@angular/forms';

/** Definición de un campo del formulario. */
export interface CampoAbm {
  clave: string;
  etiqueta: string;
  tipo: 'texto' | 'numero' | 'decimal' | 'fecha' | 'fechahora' | 'select' | 'checkbox';
  requerido?: boolean;
  opciones?: { valor: string | number; texto: string }[];
  ayuda?: string;
  /** Valor inicial al crear un registro nuevo. */
  porDefecto?: unknown;
  /** Solo se puede cargar al crear (ej: el piloto de un resultado). */
  soloAlta?: boolean;
}

/** Columna del listado. */
export interface ColumnaAbm {
  clave: string;
  etiqueta: string;
  formato?: (r: RegistroGenerico) => string;
}

@Component({
  imports: [FormsModule],                    
  selector: 'app-abm-tabla',
  styleUrl: './abm-tabla.css',
  templateUrl: './abm-tabla.html',
})
export class AbmTabla {
  private readonly abmService = inject(AbmService);

  readonly titulo = input.required<string>();
  readonly nombreRecurso = input.required<NombreRecurso>(); 
  
  readonly columnas = input.required<ColumnaAbm[]>();
  readonly campos = input<CampoAbm[]>([]);
  
  readonly filtros = input<Filtros>({});
  
  readonly valoresFijos = input<Record<string, unknown>>({});
  readonly habilitado = input(true);
  readonly mensajeDeshabilitado = input('Seleccione el contexto para ver los registros.');
  readonly soloLectura = input(false);
  readonly seleccionable = input(false);
  readonly seleccionadoId = input<number | null>(null);

  readonly seleccionado = output<RegistroGenerico>();

  readonly cambio = output<void>();                    

  protected readonly filas = signal<RegistroGenerico[]>([]);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly mensaje = signal<string | null>(null);
  protected readonly formulario = signal<Record<string, unknown> | null>(null);
  protected readonly editandoId = signal<number | null>(null);
  protected readonly errorForm = signal<string | null>(null);
  protected readonly guardando = signal(false);

  private readonly claveRecarga = computed(() => JSON.stringify(this.filtros()) + '|' + this.habilitado());

  constructor() {
    effect(() => {
      this.claveRecarga();
      this.nombreRecurso();
      untracked(() => {
        this.formulario.set(null);
        this.recargar();
      });
    });
  }

  recargar(): void {
    if (!this.habilitado()) {
      this.filas.set([]);
      return;
    }
    this.cargando.set(true);
    this.error.set(null);
    this.abmService.listar(this.nombreRecurso(), this.filtros()).subscribe({
      next: (response) => {
        this.filas.set(response);
        this.cargando.set(false);
      },
      error: (er) => {
        this.error.set(mensajeError(er));
        this.cargando.set(false);
      },
    });
  }

  protected nuevo(): void {
    const valoresIniciales : Record<string, unknown> = {};
    for (const campo of this.campos()) {
      valoresIniciales[campo.clave] = campo.porDefecto ?? (campo.tipo === 'checkbox' ? false : null);
    } 
    this.editandoId.set(null);
    this.errorForm.set(null);
    this.formulario.set(valoresIniciales);
  }

  protected editar(registro: RegistroGenerico): void {
    const formData: Record<string, unknown> = {};
    for (const campo of this.campos()) {
      const valor = registro[campo.clave];
      formData[campo.clave] = campo.tipo === 'fechahora' ? aInputLocal(valor as string | null) 
                  : (valor ?? (campo.tipo === 'checkbox' ? false : null));
    }
    this.editandoId.set(registro.id);
    this.errorForm.set(null);
    this.formulario.set(formData);
    setTimeout(() => document.querySelector('.abm-form')?.scrollIntoView({ behavior: 'smooth', block: 'center' }));
  }

  protected cancelar(): void {
    this.formulario.set(null);
  }

  protected guardar(): void {
    const formData = this.formulario();
    if (!formData) return;
    const faltantes = this.campos().filter((c) => 
        c.requerido && !(c.soloAlta && this.editandoId()) && estaVacio(formData[c.clave]));
    if (faltantes.length) {
      this.errorForm.set('Complete: ' + faltantes.map((c) => c.etiqueta).join(', '));
      return;
    }
    const cuerpo: Record<string, unknown> = {};

    for (const campo of this.campos()) {
      cuerpo[campo.clave] = convertirValorAPI(campo, formData[campo.clave]);
    }
    Object.assign(cuerpo, this.valoresFijos());

    this.guardando.set(true);
    this.errorForm.set(null);
    const id = this.editandoId();
    const peticion = id                                                      
      ? this.abmService.modificar(this.nombreRecurso(), id, cuerpo)
      : this.abmService.crear(this.nombreRecurso(), cuerpo);
    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.formulario.set(null);
        this.mensaje.set(id ? 'Registro modificado.' : 'Registro creado.');
        this.recargar();
        this.cambio.emit();
      },
      error: (errorHttp) => {
        this.guardando.set(false);
        this.errorForm.set(mensajeError(errorHttp));
      },
    });
  }

  protected eliminar(registro: RegistroGenerico): void {                            
    const descripcion = this.columnas().map((columna) => 
      (columna.formato ? columna.formato(registro) : registro[columna.clave]))
                    .filter(Boolean).slice(0, 2).join(' · ');
    if (!confirm(`¿Dar de baja "${descripcion}"?`)) return;
    this.error.set(null);
    this.abmService.eliminar(this.nombreRecurso(), registro.id).subscribe({
      next: () => {
        this.mensaje.set('Registro dado de baja.');
        this.recargar();
        this.cambio.emit();
      },
      error: (errorHttp) => this.error.set(mensajeError(errorHttp)),
    });
  }

  protected tipoInput(campo: CampoAbm): string {
    return { numero: 'number', decimal: 'number', fecha: 'date', fechahora: 'datetime-local' }[campo.tipo as string] ?? 'text';
  }
}

function estaVacio(valor: unknown): boolean {
  return valor === null || valor === undefined || (typeof valor === 'string' && valor.trim() === '');
}

function convertirValorAPI(campo: CampoAbm, valor: unknown): unknown {
  if (campo.tipo === 'checkbox') return !!valor;
  if (estaVacio(valor)) return null;
  switch (campo.tipo) {
    case 'numero':
      return Math.trunc(Number(valor));
    case 'decimal':
      return Number(valor);
    case 'fechahora':
      return new Date(String(valor)).toISOString(); // hora local del dispositivo -> UTC
    case 'texto':
      return String(valor).trim();
    default:
      return valor;
  }
}

/** ISO UTC -> "yyyy-MM-ddTHH:mm" en la hora local del dispositivo (formato de datetime-local). */
function aInputLocal(iso: string | null): string | null {
  if (!iso) return null;
  const fecha = new Date(iso);
  const conDosDigitos = (n: number) => String(n).padStart(2, '0');
  return `${fecha.getFullYear()}-${conDosDigitos(fecha.getMonth() + 1)}-${conDosDigitos(date.getDate())}T${p(date.getHours())}:${p(fecha.getMinutes())}`;
}