import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { mensajeError } from '../../core/errores/error';
import { ZonaHorariaService } from '../../core/services/zona-horaria.service';


@Component({
  imports: [FormsModule],
  selector: 'app-login',
  styleUrl: './login.css',
  templateUrl: './login.html',
})
export class Login {
  private readonly authService = inject(AuthService);

  private readonly router = inject(Router);

  private readonly zonaService = inject(ZonaHorariaService)

  protected readonly modo = signal<'login' | 'registro'>('login');
  protected readonly error = signal<string| null>(null);
  protected nombre = '';
  protected email = '';
  protected password = '';

  protected enviar(): void {
    const peticion =
      this.modo() === 'login'
        ? this.authService.login(this.email, this.password)
        : this.authService.registrar({ nombre: this.nombre, apellido : '',email: this.email, password: this.password, zonaHoraria: this.zonaService.zona() });
    peticion.subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: (e) => this.error.set(mensajeError(e)),
    });
  }
}

