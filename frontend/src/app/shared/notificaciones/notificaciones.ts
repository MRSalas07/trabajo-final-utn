import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NotificacionService } from '../../core/services/notificacion.service';

@Component({
  imports: [RouterLink],
  selector: 'app-notificaciones',
  styleUrl: './notificaciones.css',
  templateUrl: './notificaciones.html',
})
export class Notificaciones {
  protected readonly notificacionService = inject(NotificacionService);
}
