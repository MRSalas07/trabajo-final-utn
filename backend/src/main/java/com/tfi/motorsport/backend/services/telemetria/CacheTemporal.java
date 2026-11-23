package com.tfi.motorsport.backend.services.telemetria;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Caché en memoria con vencimiento y tope de entradas, para los datos de OpenF1.
 *
 * Los datos de una sesión terminada no cambian y volver a pedirlos gastaría el cupo del proveedor
 * (30 consultas por minuto en el plan gratuito). Al superar el tope se descartan las entradas
 * usadas hace más tiempo, así la memoria no crece sin límite aunque se consulten muchas sesiones.
 */
public class CacheTemporal {

    private record Entrada(Instant vence, Object valor) {
    }

    private final Duration duracion;
    private final Map<String, Entrada> entradas;

    public CacheTemporal(Duration duracion, int maxEntradas) {
        this.duracion = duracion;
        // accessOrder = true: el orden es de uso, así removeEldestEntry descarta la menos usada
        this.entradas = new LinkedHashMap<String, Entrada>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Entrada> masVieja) {
                return size() > maxEntradas;
            }
        };
    }

    /**
     * Devuelve el valor guardado si no venció; si no, lo carga y lo guarda.
     * La carga se hace fuera del bloqueo: una consulta lenta a OpenF1 no frena al resto.
     * Si la carga lanza una excepción, no se guarda nada.
     */
    @SuppressWarnings("unchecked")
    public <T> T obtener(String clave, Supplier<T> cargar) {
        synchronized (entradas) {
            Entrada entrada = entradas.get(clave);
            if (entrada != null && entrada.vence().isAfter(Instant.now())) {
                return (T) entrada.valor();
            }
        }
        T valor = cargar.get();
        synchronized (entradas) {
            entradas.put(clave, new Entrada(Instant.now().plus(duracion), valor));
        }
        return valor;
    }
}
