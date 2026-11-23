package com.tfi.motorsport.backend.clients.openf1;

import java.util.ArrayDeque;

/**
 * Límite de consultas por ventana de tiempo (ej: 28 consultas cada 60 s), con ventana DESLIZANTE:
 * se guarda el instante de cada consulta y solo cuentan las de los últimos ventanaMs.
 *
 * Es más precisa que contar "por minuto calendario", que permitiría el doble de consultas en pocos
 * segundos alrededor del cambio de minuto. No duerme: devuelve cuánto hay que esperar, así se prueba
 * sin relojes reales.
 */
class VentanaDeslizante {

    private final int limite;
    private final long ventanaMs;
    private final ArrayDeque<Long> consultas = new ArrayDeque<>();

    VentanaDeslizante(int limite, long ventanaMs) {
        this.limite = Math.max(1, limite);
        this.ventanaMs = ventanaMs;
    }

    /**
     * Intenta reservar una consulta en el instante ahoraMs.
     *
     * @return 0 si se reservó; si no, los milisegundos que faltan para que se libere un lugar
     */
    synchronized long reservar(long ahoraMs) {
        while (!consultas.isEmpty() && ahoraMs - consultas.peekFirst() >= ventanaMs) {
            consultas.pollFirst();
        }
        if (consultas.size() < limite) {
            consultas.addLast(ahoraMs);
            return 0;
        }
        return Math.max(1, consultas.peekFirst() + ventanaMs - ahoraMs);
    }
}
