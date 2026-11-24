import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FotoSesion as Foto, SesionTelemetria } from '../../../core/modelos';
import { PedidoFoto, TelemetriaService } from '../../../core/services/telemetria.service';
import { ZonaHorariaService } from '../../../core/services/zona-horaria.service';
import { mensajeError } from '../../../core/errores/error';
import { EstadoCarga } from '../../../shared/estado-carga/estado-carga';
import { HoraPipe } from '../../../shared/pipe/formato';
import { TorreTiempos } from './torre-tiempos/torre-tiempos';
import { VersusPilotos } from './versus-pilotos/versus-pilotos';

/** En vivo se pide una foto nueva cada 5 segundos (el cupo de OpenF1 no permite menos). */
export const REFRESCO_VIVO_MS = 5000;
/** En la repetición se pide una foto por segundo: el backend la calcula en memoria, sin consultar a OpenF1. */
export const PASO_REPETICION_MS = 1000;
/** Segundos de sesión que avanza la repetición por cada segundo real. */
export const VELOCIDADES = [1, 5, 10, 30];

/**
 * Foto de la sesión: torre de tiempos, versus de dos pilotos, dirección de carrera y clima en un instante.
 * - Sesión en curso: el instante es "ahora" y se pide una foto nueva cada 5 s.
 * - Sesión terminada: se elige el momento con la línea de tiempo (en minutos) o por vuelta, o se reproduce:
 *   cada segundo el reloj avanza "velocidad" segundos y se pide la foto de ese instante.
 * No hay conexiones abiertas: cada foto es un GET común; el ritmo lo maneja este componente con setInterval.
 */
@Component({
  imports: [EstadoCarga, HoraPipe, TorreTiempos, VersusPilotos],
  selector: 'app-foto-sesion',
  styleUrl: './foto-sesion.css',
  templateUrl: './foto-sesion.html',
})
export class FotoSesion {
  private readonly telemetriaService = inject(TelemetriaService);
  protected readonly zona = inject(ZonaHorariaService).zona;
  protected readonly velocidades = VELOCIDADES;

  readonly sesion = input.required<SesionTelemetria>();

  protected readonly foto = signal<Foto | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly reproduciendo = signal(false);
  protected readonly velocidad = signal(VELOCIDADES[1]);
  /** Pilotos del versus: al llegar la primera foto se fijan los dos que eligió el backend (los dos primeros). */
  private readonly pilotos = signal<number[]>([]);

  // computed solo avisa cuando el valor cambia: el efecto del vivo no se reinicia con cada foto
  protected readonly enVivo = computed(() => this.foto()?.enVivo === true);
  protected readonly porVueltas = computed(() => (this.foto()?.totalVueltas ?? 0) > 0);
  protected readonly vueltaMostrada = computed(() => this.foto()?.vuelta ?? 0);
  /** La vuelta que está dando el líder, como en la TV: completadas + 1 (sin pasarse del total). */
  protected readonly vueltaEnCurso = computed(() => Math.min(this.vueltaMostrada() + 1, this.foto()?.totalVueltas ?? 0));
  /** Instantes de la foto en milisegundos, para la línea de tiempo. */
  private readonly instanteMs = computed(() => milisegundos(this.foto()?.instante));
  private readonly inicioMs = computed(() => milisegundos(this.foto()?.inicioDatos));
  private readonly finMs = computed(() => milisegundos(this.foto()?.finDatos));
  /** Segundos desde el inicio de la sesión: la línea de tiempo se mueve de a segundos. */
  protected readonly segundoActual = computed(() => Math.floor((this.instanteMs() - this.inicioMs()) / 1000));
  protected readonly segundoFinal = computed(() => Math.max(1, Math.ceil((this.finMs() - this.inicioMs()) / 1000)));
  protected readonly enElFinal = computed(() => this.instanteMs() >= this.finMs());
  /** Tiempo transcurrido de la tanda de la clasificación, como en la TV ("07:12"). */
  protected readonly relojTanda = computed(() => {
    const inicioTanda = milisegundos(this.foto()?.inicioTanda);
    if (!inicioTanda) return null;
    const segundos = Math.max(0, Math.floor((this.instanteMs() - inicioTanda) / 1000));
    return `${String(Math.floor(segundos / 60)).padStart(2, '0')}:${String(segundos % 60).padStart(2, '0')}`;
  });

  /** Descarta las respuestas viejas: si se pidió otra foto mientras tanto, la anterior ya no sirve. */
  private pedidoFoto = 0;

  constructor() {
    // Cambió la sesión: se empieza de cero con la foto del final (o de ahora, si está en curso)
    effect(() => {
      this.sesion();
      untracked(() => {
        this.reproduciendo.set(false);
        this.pilotos.set([]);
        this.foto.set(null);
        this.cargarFoto({});
      });
    });

    // En vivo: una foto nueva cada 5 s (si la anterior ya llegó). onCleanup frena el intervalo si deja de estar
    // en vivo o se sale de la página
    effect((onCleanup) => {
      if (!this.enVivo()) return;
      const intervalo = setInterval(() => {
        if (!this.cargando()) this.cargarFoto({});
      }, REFRESCO_VIVO_MS);
      onCleanup(() => clearInterval(intervalo));
    });

    // Repetición: cada segundo el reloj avanza "velocidad" segundos de la sesión
    effect((onCleanup) => {
      if (!this.reproduciendo()) return;
      const intervalo = setInterval(() => this.avanzar(), PASO_REPETICION_MS);
      onCleanup(() => clearInterval(intervalo));
    });
  }

  protected alternarReproduccion(): void {
    if (this.reproduciendo()) {
      this.reproduciendo.set(false);
      return;
    }
    // Si está en el final, la repetición empieza desde el principio
    if (this.enElFinal()) this.irAlInstante(this.inicioMs());
    this.reproduciendo.set(true);
  }

  protected elegirVelocidad(valor: string): void {
    this.velocidad.set(Number(valor));
  }

  protected irAlSegundo(valor: string): void {
    this.irAlInstante(this.inicioMs() + Number(valor) * 1000);
  }

  /** Segundos como reloj de sesión: "1:05:09" o "58:12". */
  protected reloj(segundos: number): string {
    const horas = Math.floor(segundos / 3600);
    const minutos = Math.floor((segundos % 3600) / 60);
    const resto = String(segundos % 60).padStart(2, '0');
    return horas > 0 ? `${horas}:${String(minutos).padStart(2, '0')}:${resto}` : `${minutos}:${resto}`;
  }

  protected irAVuelta(vuelta: number): void {
    const total = this.foto()?.totalVueltas ?? 0;
    if (vuelta < 1 || vuelta > total) return;
    this.cargarFoto({ vuelta });
  }

  protected verFinal(): void {
    this.reproduciendo.set(false);
    this.cargarFoto({});
  }

  /** El versus cambió de pilotos: se vuelve a pedir la misma foto con los nuevos. */
  protected elegirPilotos(numeros: number[]): void {
    this.pilotos.set(numeros);
    if (this.enVivo() || this.enElFinal()) this.cargarFoto({});
    else this.irAlInstante(this.instanteMs());
  }

  private avanzar(): void {
    // Si la foto anterior todavía no llegó, se espera al próximo segundo: si el servidor tarda más de un segundo,
    // los pedidos no se acumulan ni se descartan sus respuestas (la repetición quedaría quieta)
    if (this.cargando()) return;
    if (this.enElFinal()) {
      this.reproduciendo.set(false);
      return;
    }
    this.irAlInstante(Math.min(this.instanteMs() + this.velocidad() * 1000, this.finMs()));
  }

  private irAlInstante(milis: number): void {
    if (!Number.isFinite(milis)) return;
    this.cargarFoto({ instante: new Date(milis).toISOString() });
  }

  private cargarFoto(pedido: PedidoFoto): void {
    const numero = ++this.pedidoFoto;
    this.cargando.set(true);
    this.error.set(null);
    this.telemetriaService.foto(this.sesion().sessionKey, { ...pedido, pilotos: this.pilotos() }).subscribe({
      next: (foto) => {
        if (numero !== this.pedidoFoto) return;
        this.cargando.set(false);
        this.foto.set(foto);
        // La primera vez el backend elige a los dos mejor ubicados: se fijan para que el versus no cambie
        // de pilotos solo cuando hay un sobrepaso (hasta que el usuario elija otros o cambie de sesión)
        if (!this.pilotos().length && foto.versus) this.pilotos.set([foto.versus.a.numero, foto.versus.b.numero]);
      },
      error: (e) => {
        if (numero !== this.pedidoFoto) return;
        this.cargando.set(false);
        this.reproduciendo.set(false);
        this.error.set(mensajeError(e));
      },
    });
  }
}

function milisegundos(fecha: string | null | undefined): number {
  return fecha ? Date.parse(fecha) : NaN;
}
