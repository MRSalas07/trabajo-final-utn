import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { ZonaHorariaService } from '../../core/services/zona-horaria.service';
import { Router } from '@angular/router';
import { mensajeError } from '../../core/errores/error';

@Component({
  imports: [FormsModule],
  selector: 'app-perfil',
  styleUrl: './perfil.css',
  templateUrl: './perfil.html',
})
export class Perfil {
  protected readonly authService = inject(AuthService);
  protected readonly zonaService = inject(ZonaHorariaService);
  private readonly router = inject(Router);
  protected zona = this.authService.usuario()?.zonaHoraria ?? this.zonaService.zona()
  protected readonly mensaje = signal<string | null>(null);

  protected guardarCambio() : void {
    this.authService.guardarZona(this.zona).subscribe({
      next: () => this.mensaje.set('Preferencia guardada.'),
      error: (e) => this.mensaje.set(mensajeError(e))
    });
  }
  protected salir(): void {
    this.authService.cerrarSesion();
    this.router.navigateByUrl('/')
  }
}
