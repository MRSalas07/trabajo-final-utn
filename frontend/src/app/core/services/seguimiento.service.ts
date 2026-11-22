import { effect, inject, Injectable, signal } from '@angular/core';
import { AuthService } from './auth.service';
import { NotificacionService } from './notificacion.service';
import { SyncServices } from './sync.service';
import { CargaSincronizacion } from '../modelos';

const INTERVALO_MS = 3000;

const esActivo = (t: CargaSincronizacion) => t.estado === 'EN_COLA' || t.estado === 'EN_PROCESO';

/**
 * Sigue las sincronizaciones en segundo plano durante TODA la sesión del administrador,
 * no solo mientras está abierta la pantalla Admin. Las fallas se avisan en el momento y las
 * completadas se juntan en una sola notificación cuando ya no queda nada en curso,.
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
    private completadosSinAvisar: CargaSincronizacion[] = [];
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
                this.completadosSinAvisar = [];
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

            if (t.estado === 'FALLIDO') {
                this.notif.error(`La sincronización de ${t.categoria} ${t.anio} falló${t.error ? ': ' + t.error : '.'}`, '/admin');
            } else {
                this.completadosSinAvisar.push(t);
            }
        }

        if (!lista.some(esActivo) && this.completadosSinAvisar.length) {
            this.notif.exito(this.textoDeCompletados(this.completadosSinAvisar), '/admin');
            this.completadosSinAvisar = [];
        }
    }

    private textoDeCompletados(completados: CargaSincronizacion[]): string {
        if (completados.length === 1) {
            const [t] = completados;
            const r = t.resumen;
            const detalle = r ? ` (${r.eventosProcesados} eventos, ${r.resultadosProcesados} resultados)` : '';
            const avisos = r && r.avisos.length ? ` con ${r.avisos.length} avisos` : '';
            return `Sincronización de ${t.categoria} ${t.anio} completada${avisos}${detalle}.`;
        }
        const categorias = [...new Set(completados.map((t) => t.categoria))];
        const anios = completados.map((t) => t.anio);
        const totalAvisos = completados.reduce((suma, t) => suma + (t.resumen?.avisos.length ?? 0), 0);
        const de = categorias.length === 1 ? ` de ${categorias[0]} (${Math.min(...anios)}–${Math.max(...anios)})` : '';
        const avisos = totalAvisos ? `, con ${totalAvisos} avisos en total` : '';
        return `${completados.length} sincronizaciones completadas${de}${avisos}.`;
    }
}
