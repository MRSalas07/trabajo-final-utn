import { Component, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { mensajeError } from '../../core/errores/error';
import { ZonaHorariaService } from '../../core/services/zona-horaria.service';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-login',
  styleUrl: './login.css',
  templateUrl: './login.html',
})
export class Login {
  private readonly authService = inject(AuthService);

  private readonly router = inject(Router);

  private readonly zonaService = inject(ZonaHorariaService)

  readonly volver = input<string>();
  readonly expirada = input<string>();



  protected readonly modo = signal<'login' | 'registro'>('login');
  protected readonly error = signal<string| null>(null);
  protected readonly enviando = signal(false);


  protected readonly form = inject(FormBuilder).nonNullable.group({
    nombre: [''],
    apellido: [''],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });


  protected enviar(): void {
    const userForm = this.form.getRawValue();
    if (this.form.invalid || (this.modo() === 'registro' && !userForm.nombre.trim())) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);

    const peticion =
      this.modo() === 'login'
        ? this.authService.login(userForm.email, userForm.password)
        : this.authService.registrar({ nombre: userForm.nombre, apellido: userForm.apellido ,email: userForm.email, password: userForm.password, zonaHoraria: this.zonaService.zona() });
    peticion.subscribe({
      next: () => this.router.navigateByUrl(this.volver() ?? '/'),
      error: (e) => {
        this.error.set(mensajeError(e));
        this.enviando.set(false);
      },
    });

  }

protected cambiarModo(modo: 'login' | 'registro'): void {
    if (this.modo() === modo) return;
    this.modo.set(modo);
    this.error.set(null);
    this.form.reset();   // vuelve a los valores iniciales ('')

    // el nombre solo es obligatorio al registrarse
    const nombre = this.form.controls.nombre;
    nombre.setValidators(modo === 'registro' ? Validators.required : null);
    nombre.updateValueAndValidity();
  }
}

