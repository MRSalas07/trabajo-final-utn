import { effect, inject, Injectable, signal } from '@angular/core';
import { AuthService } from './auth.service';
import { NotificacionService } from './notificacion.service';
import { SyncServices } from './sync.service';
import { CargaSincronizacion } from '../modelos';

const INTERVALO_MS = 3000;

const esActivo = (t: CargaSincronizacion) => t.estado === 'EN_COLA' || t.estado === 'EN_PROCESO';

/**
 * Sigue las sincronizaciones en segundo plano durante TODA la sesión del administrador,
 * no solo mientras está abierta la pantalla Admin. Cuando un trabajo termina avisa con
 * una notificación emergente, esté el usuario en la página que esté.
 *
 * Solo consulta al backend mientras hay trabajos en curso (o tras pedir una sincronización),
 * así no genera tráfico inútil el resto del tiempo.
 */
@Injectable({ providedIn: 'root' })
export class SeguimientoService {
    
    private readonly sync = inject(SyncServices);
    private readonly notif = inject(NotificacionService);
    private readonly auth = inject(AuthService);

    readonly trabajos = signal<CargaSincronizacion[]>([]);

    private conocidos: Map<string, CargaSincronizacion['estado']> | null = null;
    private temporizador: ReturnType<typeof setTimeout> | null = null;

    constructor() {
        // Al iniciar sesión como admin (o recargar la página con la sesión abierta) se hace una
        // primera consulta: si quedó algo en curso, el seguimiento continúa solo.
        effect(() => {
        if (this.auth.esAdmin()) {
            this.consultar();
        } else {
            this.detener();
            this.trabajos.set([]);
            this.conocidos = null;
        }
        });
    }

    consultar(): void {
        this.detener();
        this.sync.trabajos().subscribe({
        next: (lista) => {
            this.avisarTerminados(lista);
            this.trabajos.set(lista);
            if (lista.some(esActivo)) this.programar();
        },
        // Fallo de red o sesión vencida: si había algo en curso se reintenta; si no, se calla
        error: () => {
            if (this.trabajos().some(esActivo) && this.auth.esAdmin()) this.programar();
        },
        });
    }

    private programar(): void {
        this.temporizador = setTimeout(() => this.consultar(), INTERVALO_MS);
    }

    private detener(): void {
        if (this.temporizador) clearTimeout(this.temporizador);
        this.temporizador = null;
    }

    private avisarTerminados(lista: CargaSincronizacion[]): void {
        const previos = this.conocidos;
        this.conocidos = new Map(lista.map((t) => [t.id, t.estado]));
        if (!previos) return; // primera carga: lo ya terminado no es una novedad

        for (const t of lista) {
        const antes = previos.get(t.id);
        const terminoAhora = t.estado === 'COMPLETADO' || t.estado === 'FALLIDO';
        // Terminó mientras lo seguíamos, o apareció ya terminado (fue muy rápido)
        if (!terminoAhora || (antes !== undefined && !(antes === 'EN_COLA' || antes === 'EN_PROCESO'))) continue;

        const nombre = `${t.categoria} ${t.anio}`;
        if (t.estado === 'COMPLETADO') {
            const r = t.resumen;
            const detalle = r ? ` (${r.eventosProcesados} eventos, ${r.resultadosProcesados} resultados)` : '';
            const avisos = r && r.avisos.length ? ` con ${r.avisos.length} avisos` : '';
            this.notif.exito(`Sincronización de ${nombre} completada${avisos}${detalle}.`, '/admin');
        } else {
            this.notif.error(`La sincronización de ${nombre} falló${t.error ? ': ' + t.error : '.'}`, '/admin');
        }
        }
    }
}
