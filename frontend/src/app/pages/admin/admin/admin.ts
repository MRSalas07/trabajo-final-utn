import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { interval, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SyncServices } from '../../../core/services/sync.service';
import { AuthService } from '../../../core/services/auth.service';

import { mensajeError } from '../../../core/errores/error';
import { FuenteDisponible, ResumenSincronizacion, CargaSincronizacion } from '../../../core/modelos';

import { HoraPipe } from '../../../shared/pipe/formato';

@Component({
  imports: [FormsModule, HoraPipe],
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
  protected readonly cargaDatos = signal<CargaSincronizacion[]>([]);
  protected readonly mensaje = signal<string | null>(null);
  protected readonly importacion = signal<ResumenSincronizacion | null>(null);
  protected readonly errorImportacion = signal<string | null>(null);

  constructor() {
    this.syncServices.obtenerFuentes().subscribe((fuente) => {
      this.fuentes.set(fuente);
      
    })
    this.refrescar();
    // Consulta el avance cada 3 segundos mientras la página está abierta
    interval(3000)
      .pipe(switchMap(() => this.syncServices.trabajos()), takeUntilDestroyed(this.destroyRef))
      .subscribe({ 
        next: (t) => this.cargaDatos.set(t), 
        error: () => {} 
      });

  }

  private refrescar(): void {
    this.syncServices.trabajos().subscribe((t) => this.cargaDatos.set(t));
  }

    protected sincronizar(): void {
    this.mensaje.set(null);
    this.syncServices.sincronizarDatosxCategoria(this.categoria, this.anio).subscribe({
      next: () => { this.mensaje.set(`Sincronización de ${this.categoria} ${this.anio} encolada.`); this.refrescar(); },
      error: (e) => this.mensaje.set(mensajeError(e)),
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
        next: (response) => this.importacion.set(response),
        error : (error) => this.errorImportacion.set(mensajeError(error))
      });
    });

  }


}
