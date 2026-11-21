import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SyncServices } from '../../../core/services/sync.service';
import { AuthService } from '../../../core/services/auth.service';
import { mensajeError } from '../../../core/errores/error';
import { FuenteDisponible, ResumenSincronizacion } from '../../../core/modelos';
import { HoraPipe } from '../../../shared/pipe/formato';
import { NotificacionService } from '../../../core/services/notificacion.service';
import { SeguimientoService } from '../../../core/services/seguimiento.service';
import { GestionDatos } from './../gestion-datos/gestion-datos';
@Component({
  imports: [FormsModule, HoraPipe, GestionDatos],
  selector: 'app-admin',
  styleUrl: './admin.css',
  templateUrl: './admin.html',
})
export class Admin {
  private readonly admin = inject(AuthService);
  private readonly syncServices = inject(SyncServices);

  private readonly destroyRef = inject(DestroyRef);

  protected readonly anioMaximo = new Date().getFullYear() + 1;
  protected categoria = 'F1';
  protected anio = new Date().getFullYear();
  protected readonly fuentes = signal<FuenteDisponible[]>([]);
  protected readonly mensaje = signal<string | null>(null);
  protected readonly importacion = signal<ResumenSincronizacion | null>(null);
  protected readonly errorImportacion = signal<string | null>(null);

  private readonly notificacionService = inject(NotificacionService);
  private readonly seguimientoService = inject(SeguimientoService);
  
  protected readonly cargaDatos = this.seguimientoService.trabajos;

  protected readonly vista = signal<'datos' | 'sync'>('datos');

  constructor() {
    this.syncServices.obtenerFuentes().subscribe((fuente) => {
      this.fuentes.set(fuente);
      
    })
    this.seguimientoService.consultar();

  }

  private refrescar(): void {
    this.syncServices.trabajos().subscribe((t) => this.cargaDatos.set(t));
  }

  protected sincronizar(): void {
    this.mensaje.set(null);
    this.syncServices.sincronizarDatosxCategoria(this.categoria, this.anio).subscribe({
      next: () => { this.mensaje.set(`Sincronización de ${this.categoria} ${this.anio} encolada.`); this.seguimientoService.consultar();; },
      error: (errorHttp) => this.mensaje.set(mensajeError(errorHttp)),
    });
  }


  protected importarDatos (evento:Event) : void {
    const archivo = (evento.target as HTMLInputElement).files?.[0];
    if (!archivo) return;
    this.importacion.set(null);
    this.errorImportacion.set(null);

    archivo.text().then((texto) => {
      let json : unknown;
      try {
        json = JSON.parse(texto);
      } catch{
        this.errorImportacion.set('El archivo no es un json válido.');
        return;
      }
      this.syncServices.importarDatosxJson(json).subscribe({
        next: (response) => {
          this.importacion.set(response);
          this.notificacionService.exito(
            `Importación de ${response.categoria} ${response.anio} completada (${response.eventosProcesados} eventos, ${response.resultadosProcesados} resultados).`);
        },
        error : (errorHttp) => {
          this.errorImportacion.set(mensajeError(errorHttp));
          this.notificacionService.error(`No se pudo importar el archivo: ${mensajeError(errorHttp)}`);
        }
      });
    });
  }


}
