import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EstadoCarga } from '../../shared/estado-carga/estado-carga';
import { EstadoPipe, HoraPipe } from '../../shared/pipe/formato';

@Component({
  imports: [RouterLink, EstadoCarga, HoraPipe, EstadoPipe],
  selector: 'app-piloto',
  styleUrl: './piloto.css',
  templateUrl: './piloto.html',
})
export class Piloto {}
