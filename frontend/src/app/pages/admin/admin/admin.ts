import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, concatMap, from, map, of, toArray } from 'rxjs';
import { SyncServices } from '../../../core/services/sync.service';
import { mensajeError } from '../../../core/errores/error';
import { FuenteDisponible, ResumenSincronizacion } from '../../../core/modelos';
import { HoraPipe } from '../../../shared/pipe/formato';
import { NotificacionService } from '../../../core/services/notificacion.service';
import { SeguimientoService } from '../../../core/services/seguimiento.service';
import { GestionDatos } from './../gestion-datos/gestion-datos';

const TEMPORADAS_MAXIMAS_POR_TANDA = 40;

@Component({
  imports: [FormsModule, HoraPipe, GestionDatos],
  selector: 'app-admin',
  styleUrl: './admin.css',
  templateUrl: './admin.html',
})
export class Admin {
  private readonly syncServices = inject(SyncServices);
  private readonly notificacionService = inject(NotificacionService);
  private readonly seguimientoService = inject(SeguimientoService);

  protected readonly anioMaximo = new Date().getFullYear() + 1;
  protected readonly maximoPorTanda = TEMPORADAS_MAXIMAS_POR_TANDA;
  protected categoria = 'F1';
  protected anioDesde = new Date().getFullYear();
  protected anioHasta = new Date().getFullYear();
  protected readonly fuentes = signal<FuenteDisponible[]>([]);
  protected readonly encolando = signal(false);
  protected readonly mensaje = signal<string | null>(null);
  protected readonly importacion = signal<ResumenSincronizacion | null>(null);
  protected readonly errorImportacion = signal<string | null>(null);

  protected readonly cargaDatos = this.seguimientoService.trabajos;

  protected readonly vista = signal<'datos' | 'sync'>('datos');

  constructor() {
    this.syncServices.obtenerFuentes().subscribe({
      next: (fuente) => this.fuentes.set(fuente),
      error: (e) => this.mensaje.set(`No se pudieron consultar las fuentes de datos: ${mensajeError(e)}`),
    });
    this.seguimientoService.consultar();
  }

  protected sincronizar(): void {
    this.mensaje.set(null);
    const desde = Number(this.anioDesde);
    const hasta = Number(this.anioHasta);
    if (!desde || !hasta || desde > hasta) {
      this.mensaje.set('Revise el rango: «Desde» tiene que ser menor o igual que «Hasta».');
      return;
    }
    const cantidad = hasta - desde + 1;
    if (cantidad > TEMPORADAS_MAXIMAS_POR_TANDA) {
      this.mensaje.set(`Como máximo ${TEMPORADAS_MAXIMAS_POR_TANDA} temporadas por tanda (pidió ${cantidad}). Divida el rango.`);
      return;
    }

    const categoria = this.categoria;
    const anios = Array.from({ length: cantidad }, (_, indice) => desde + indice);
    this.encolando.set(true);
    from(anios)
      .pipe(
        concatMap((anio) =>
          this.syncServices.sincronizarDatosxCategoria(categoria, anio).pipe(
            map(() => null),
            catchError((errorHttp) => of(`${anio}: ${mensajeError(errorHttp)}`)),
          ),
        ),
        toArray(),
      )
      .subscribe((resultados) => {
        this.encolando.set(false);
        const rechazadas = resultados.filter((resultado): resultado is string => resultado !== null);
        const rango = desde === hasta ? `${desde}` : `${desde}–${hasta}`;
        if (rechazadas.length) {
          this.mensaje.set(
            `Se encolaron ${cantidad - rechazadas.length} de ${cantidad} temporadas de ${categoria} (${rango}). ` +
              `Rechazadas: ${rechazadas.join('; ')}`,
          );
        } else if (cantidad === 1) {
          this.mensaje.set(`Sincronización de ${categoria} ${desde} encolada.`);
        } else {
          this.mensaje.set(`Se encolaron ${cantidad} temporadas de ${categoria} (${rango}). Se procesan de a una.`);
        }
        this.seguimientoService.consultar();
      });
  }

  protected importarDatos(evento: Event): void {
    const archivo = (evento.target as HTMLInputElement).files?.[0];
    if (!archivo) return;
    this.importacion.set(null);
    this.errorImportacion.set(null);

    archivo.text().then((texto) => {
      let json: unknown;
      try {
        json = JSON.parse(texto);
      } catch {
        this.errorImportacion.set('El archivo no es un json válido.');
        return;
      }
      this.syncServices.importarDatosxJson(json).subscribe({
        next: (response) => {
          this.importacion.set(response);
          this.notificacionService.exito(
            `Importación de ${response.categoria} ${response.anio} completada (${response.eventosProcesados} eventos, ${response.resultadosProcesados} resultados).`);
        },
        error: (errorHttp) => {
          this.errorImportacion.set(mensajeError(errorHttp));
          this.notificacionService.error(`No se pudo importar el archivo: ${mensajeError(errorHttp)}`);
        },
      });
    });
  }
}
