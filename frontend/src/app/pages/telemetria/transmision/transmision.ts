import { Component, DestroyRef, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { Subscription } from 'rxjs';
import { EstadoTransmision, FaseTransmision } from '../../../core/modelos';
import { AjusteTransmision, TelemetriaService } from '../../../core/services/telemetria.service';
import { ZonaHorariaService } from '../../../core/services/zona-horaria.service';
import { mensajeError } from '../../../core/errores/error';
import { HoraPipe } from '../../../shared/pipe/formato';
import { traducirSesion } from '../sesiones';
import { MapaPista } from './mapa-pista/mapa-pista';
import { PanelFoco } from './panel-foco/panel-foco';
import { TorreTiempos } from './torre-tiempos/torre-tiempos';

/** Velocidades de la repetición (el backend acepta de 0.5x a 10x). */
export const VELOCIDADES = [0.5, 1, 2, 5, 10];

const TEXTO_PISTA: Record<string, string> = {
  VERDE: 'Pista libre',
  AMARILLA: 'Bandera amarilla',
  SC: 'Auto de seguridad',
  VSC: 'Auto de seguridad virtual',
  ROJA: 'Bandera roja',
  CUADROS: 'Bandera a cuadros',
};

const TEXTO_FASE: Record<FaseTransmision, string> = {
  CARGANDO: 'Cargando los datos de la sesión…',
  EN_CURSO: 'En curso',
  PAUSADA: 'En pausa',
  ESPERANDO_DATOS: 'Descargando telemetría…',
  FINALIZADA: 'Terminó la sesión',
  ERROR: 'No se pudo transmitir',
};

/** conectando: esperando la primera foto · abierta: llegan fotos · cortada: se perdió sin aviso · cerrada: terminó. */
type Conexion = 'conectando' | 'abierta' | 'cortada' | 'cerrada';

/**
 * Transmisión de telemetría F1 (MVP 7): se conecta por SSE a la dirección recibida y muestra la foto que
 * llega cada segundo (torre de tiempos, mapa, telemetría del auto seguido, dirección de carrera y clima).
 * El formato es el mismo en vivo y en repetición; en la repetición además se puede pausar y cambiar la velocidad.
 */
@Component({
  imports: [HoraPipe, TorreTiempos, MapaPista, PanelFoco],
  selector: 'app-transmision',
  styleUrl: './transmision.css',
  templateUrl: './transmision.html',
})
export class Transmision {
  private readonly telemetriaService = inject(TelemetriaService);
  protected readonly zona = inject(ZonaHorariaService).zona;

  /** Dirección del stream (la arma TelemetriaService). Si cambia, se reconecta. */
  readonly url = input.required<string>();
  /** El espectador tocó "Detener" (o "Volver" después de un error). */
  readonly detenida = output<void>();

  protected readonly velocidades = VELOCIDADES;
  protected readonly estado = signal<EstadoTransmision | null>(null);
  /** El contorno llega solo cuando cambia: se conserva el último recibido. */
  protected readonly trazado = signal<[number, number][]>([]);
  protected readonly conexion = signal<Conexion>('conectando');
  /** Motivo que mandó el servidor al no poder transmitir. */
  protected readonly errorServidor = signal<string | null>(null);
  /** Falla de un control (pausa, velocidad o piloto). */
  protected readonly errorControl = signal<string | null>(null);
  /** Piloto elegido por el espectador (null = el líder). */
  protected readonly seguido = signal<number | null>(null);

  protected readonly fase = computed<FaseTransmision>(() => this.estado()?.estado ?? 'CARGANDO');
  protected readonly esVivo = computed(() => this.estado()?.modo === 'VIVO');
  protected readonly pausada = computed(() => this.fase() === 'PAUSADA');
  protected readonly terminada = computed(() => this.fase() === 'FINALIZADA' || this.fase() === 'ERROR');
  /** Pausa y velocidad: solo en una repetición abierta que todavía no terminó. */
  protected readonly puedeControlar = computed(
    () => !!this.estado() && !this.esVivo() && !this.terminada() && this.conexion() === 'abierta',
  );
  protected readonly cargando = computed(
    () => (this.conexion() === 'conectando' && !this.estado()) || this.fase() === 'CARGANDO' || this.fase() === 'ESPERANDO_DATOS',
  );
  protected readonly titulo = computed(() => {
    const e = this.estado();
    if (!e) return 'Conectando…';
    return e.circuito ? `${traducirSesion(e.sesion)} · ${e.circuito}` : traducirSesion(e.sesion);
  });
  protected readonly textoPista = computed(() => TEXTO_PISTA[this.estado()?.estadoPista ?? 'VERDE'] ?? 'Pista');
  /** Lo que está pasando: el aviso del backend si mandó uno ("Descargando telemetría…") o el de la fase. */
  protected readonly textoFase = computed(() => {
    const e = this.estado();
    if (!e) return this.conexion() === 'conectando' ? 'Conectando con el servidor…' : '';
    return e.aviso ?? TEXTO_FASE[this.fase()];
  });
  private readonly formatoReloj = computed(() => formatoHora(this.zona()));
  /** Hora de la sesión que se está viendo, con segundos, en la zona horaria elegida. */
  protected readonly reloj = computed(() => {
    const hora = this.estado()?.hora;
    return hora ? this.formatoReloj().format(new Date(hora)) : '—';
  });
  protected readonly torre = computed(() => this.estado()?.torre ?? []);
  protected readonly foco = computed(() => this.estado()?.foco ?? null);
  protected readonly numeroFoco = computed(() => this.foco()?.numero ?? null);
  protected readonly mapa = computed(() => this.estado()?.mapa ?? []);
  protected readonly mensajes = computed(() => this.estado()?.mensajes ?? []);
  protected readonly clima = computed(() => this.estado()?.clima ?? null);
  /** Los autos se deslizan en el mapa solo a velocidades bajas: a 5x o 10x cruzarían el circuito en diagonal. */
  protected readonly animarMapa = computed(() => {
    const e = this.estado();
    return !!e && e.estado === 'EN_CURSO' && e.velocidad <= 2;
  });

  private suscripcion: Subscription | null = null;
  private urlActual = '';

  constructor() {
    effect(() => {
      const url = this.url();
      untracked(() => this.conectar(url));
    });
    inject(DestroyRef).onDestroy(() => this.suscripcion?.unsubscribe());
  }

  protected alternarPausa(): void {
    this.ajustar({ pausa: !this.pausada() });
  }

  protected cambiarVelocidad(valor: string): void {
    this.ajustar({ velocidad: Number(valor) });
  }

  /** Sigue a un piloto; tocar de nuevo al que ya se sigue (o elegir null) vuelve al líder. */
  protected seguir(numero: number | null): void {
    const nuevo = numero !== null && numero === this.seguido() ? null : numero;
    this.ajustar({ piloto: nuevo ?? 0 }, () => this.seguido.set(nuevo));
  }

  /** Vuelve a conectar después de un corte. Una repetición sigue desde el último minuto que se vio. */
  protected reanudar(): void {
    const e = this.estado();
    const url = new URL(this.urlActual, document.baseURI);
    if (e && e.modo === 'REPETICION') {
      url.searchParams.delete('inicioVuelta');
      url.searchParams.set('inicioMin', String(Math.max(0, e.minuto ?? 0)));
      url.searchParams.set('velocidad', String(e.velocidad));
    }
    const seguido = this.seguido();
    if (seguido) url.searchParams.set('piloto', String(seguido));
    else url.searchParams.delete('piloto');
    this.conectar(url.toString());
  }

  protected detener(): void {
    this.suscripcion?.unsubscribe();
    this.suscripcion = null;
    this.conexion.set('cerrada');
    this.detenida.emit();
  }

  private conectar(url: string): void {
    this.suscripcion?.unsubscribe();
    this.urlActual = url;
    this.conexion.set('conectando');
    this.errorServidor.set(null);
    this.errorControl.set(null);
    this.suscripcion = this.telemetriaService.transmitir(url).subscribe((evento) => {
      switch (evento.tipo) {
        case 'estado':
          if (evento.estado.trazado) this.trazado.set(evento.estado.trazado);
          this.estado.set(evento.estado);
          this.conexion.set(evento.estado.estado === 'FINALIZADA' || evento.estado.estado === 'ERROR' ? 'cerrada' : 'abierta');
          break;
        case 'error':
          this.errorServidor.set(evento.mensaje);
          this.conexion.set('cerrada');
          break;
        case 'desconexion':
          this.conexion.set('cortada');
          break;
      }
    });
  }

  private ajustar(cambios: AjusteTransmision, alAplicar?: () => void): void {
    const id = this.estado()?.id;
    if (!id) return;
    this.errorControl.set(null);
    this.telemetriaService.ajustar(id, cambios).subscribe({
      next: () => alAplicar?.(),
      error: (e) => this.errorControl.set(mensajeError(e)),
    });
  }
}

function formatoHora(zona: string): Intl.DateTimeFormat {
  const opciones: Intl.DateTimeFormatOptions = { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false };
  try {
    return new Intl.DateTimeFormat('es-AR', { ...opciones, timeZone: zona });
  } catch {
    return new Intl.DateTimeFormat('es-AR', opciones);
  }
}
