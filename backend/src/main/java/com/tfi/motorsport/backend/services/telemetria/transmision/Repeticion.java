package com.tfi.motorsport.backend.services.telemetria.transmision;

import com.tfi.motorsport.backend.clients.openf1.OpenF1Cliente;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.SesionTelemetria;
import com.tfi.motorsport.backend.dtos.telemetria.TransmisionDtos;
import com.tfi.motorsport.backend.dtos.telemetria.TransmisionDtos.FilaTorre;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.ConFecha;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * MODO REPETICIÓN: reproduce una sesión ya terminada como si fuera en vivo.
 *
 * Usa datos HISTÓRICOS de OpenF1 (gratis, mismo formato que en vivo) y un RELOJ VIRTUAL:
 * cada segundo real el reloj avanza "velocidad" segundos (x1 = tiempo real) y se aplican
 * todos los eventos cuya fecha ya fue alcanzada.
 *
 *  - Torre, vueltas, banderas, clima y boxes: se descargan completos al inicio (≈4 MB por carrera).
 *  - Telemetría del auto y posiciones en pista: son muy pesadas para bajarlas enteras, así que se
 *    piden en VENTANAS de 60 s a medida que el reloj avanza (y se precarga la siguiente).
 *    Si una ventana todavía no llegó, el reloj espera (estado ESPERANDO_DATOS) para no desincronizar.
 */
@Slf4j
public class Repeticion extends Transmision {

    private static final long VENTANA_SEG = 60;
    /** Al empezar por vuelta se arranca unos segundos antes, para ver el paso por la línea. */
    private static final Duration ANTICIPO_VUELTA = Duration.ofSeconds(5);

    private final OpenF1Cliente cliente;
    private final ExecutorService descargas;
    private final Function<Integer, DatosSesion> cargarDatos;
    /** Caché COMPARTIDO entre repeticiones: dos espectadores de la misma sesión no descargan dos veces. */
    private final Map<String, List<? extends ConFecha>> cacheCompartido;
    private final int sessionKey;
    private final int inicioMin;
    /** Vuelta desde la que se empieza (null = se usa inicioMin). */
    private final Integer inicioVuelta;

    private List<ConFecha> eventos = List.of();
    private int indiceEvento;
    private Instant reloj;
    private Instant fin;

    /** Ventanas de telemetría (MuestraAuto o Ubicacion): clave "car-{piloto}-{inicio}" o "loc-{inicio}". */
    private final Map<String, List<? extends ConFecha>> ventanas = new ConcurrentHashMap<>();
    private final Map<String, Boolean> pedidas = new ConcurrentHashMap<>();
    /** true cuando el espectador se fue: las descargas pendientes se cancelan para no gastar consultas. */
    private volatile boolean detenida;

    public Repeticion(OpenF1Cliente cliente, ExecutorService descargas, Function<Integer, DatosSesion> cargarDatos,
                      Map<String, List<? extends ConFecha>> cacheCompartido, int sessionKey, double velocidad, int inicioMin,
                      Integer inicioVuelta) {
        super(TransmisionDtos.MODO_REPETICION);
        this.cliente = cliente;
        this.descargas = descargas;
        this.cargarDatos = cargarDatos;
        this.cacheCompartido = cacheCompartido;
        this.sessionKey = sessionKey;
        this.velocidad = velocidad;
        this.inicioMin = inicioMin;
        this.inicioVuelta = inicioVuelta;
    }

    @Override
    public void iniciar() {
        aviso = "Descargando los datos de la sesión…";
        try {
            DatosSesion datos = cargarDatos.apply(sessionKey);
            sesion = datos.sesion();
            if (sesion.inicio() == null) {
                throw new IllegalStateException("OpenF1 no informa el horario de la sesión");
            }
            estado.setPilotos(datos.pilotos());
            estado.setStints(datos.stints());
            eventos = datos.eventos();
            estado.setTotalVueltas(totalVueltas(eventos));
            fin = sesion.fin() != null ? sesion.fin() : sesion.inicio().plus(Duration.ofHours(2));
            reloj = sesion.inicio().plus(Duration.ofMinutes(inicioMin));
            if (inicioVuelta != null) {
                int total = totalVueltas(eventos);
                if (inicioVuelta > total) {
                    aviso = total == 0 ? "La sesión no tiene vueltas registradas."
                            : "La sesión tiene " + total + " vueltas: elegí una entre 1 y " + total + ".";
                    fase = ERROR;
                    return;
                }
                // Si OpenF1 no informa el inicio de esa vuelta (suele pasar con la 1), se arranca desde el principio
                Instant partida = inicioDeVuelta(eventos, inicioVuelta).map(i -> i.minus(ANTICIPO_VUELTA))
                        .orElse(sesion.inicio());
                reloj = partida.isBefore(sesion.inicio()) ? sesion.inicio() : partida;
            }
            // "Adelantar" hasta el minuto pedido: se aplican al instante todos los eventos anteriores
            aplicarEventosHasta(reloj);
            estado.setReloj(reloj);
            aviso = null;
            fase = EN_CURSO;
        } catch (RuntimeException ex) {
            log.warn("No se pudo iniciar la repetición de {}: {}", sessionKey, ex.getMessage());
            aviso = "No se pudo cargar la sesión: " + ex.getMessage();
            fase = ERROR;
        }
    }

    @Override
    public synchronized void avanzar() {
        if (!EN_CURSO.equals(fase) && !ESPERANDO_DATOS.equals(fase)) {
            return;
        }
        if (pausada) {
            return;
        }
        Instant objetivo = reloj.plusMillis((long) (velocidad * 1000));
        Integer foco = focoActual();

        // Ventanas necesarias para cubrir [reloj, objetivo] + precarga de la siguiente
        boolean listas = asegurarVentana("loc", null, reloj) & asegurarVentana("loc", null, objetivo);
        if (foco != null) {
            listas &= asegurarVentana("car", foco, reloj) & asegurarVentana("car", foco, objetivo);
            asegurarVentana("car", foco, objetivo.plusSeconds(VENTANA_SEG / 2));
        }
        asegurarVentana("loc", null, objetivo.plusSeconds(VENTANA_SEG / 2));
        if (!listas) {
            fase = ESPERANDO_DATOS;
            aviso = "Descargando telemetría…";
            return;
        }
        fase = EN_CURSO;
        aviso = null;

        aplicarEventosHasta(objetivo);
        aplicarVentanas("loc", null, reloj, objetivo);
        if (foco != null) {
            aplicarVentanas("car", foco, reloj, objetivo);
        }
        reloj = objetivo;
        estado.setReloj(reloj);
        liberarVentanasViejas();

        if (reloj.isAfter(fin.plus(Duration.ofMinutes(2)))) {
            fase = FINALIZADA;
            aviso = "Fin de la sesión";
        }
    }

    private void aplicarEventosHasta(Instant hasta) {
        while (indiceEvento < eventos.size() && !eventos.get(indiceEvento).fecha().isAfter(hasta)) {
            estado.aplicar(eventos.get(indiceEvento++));
        }
    }

    private void aplicarVentanas(String tipo, Integer piloto, Instant desde, Instant hasta) {
        for (long inicio = inicioVentana(desde); inicio <= inicioVentana(hasta); inicio += VENTANA_SEG) {
            List<? extends ConFecha> datos = ventanas.get(clave(tipo, piloto, inicio));
            if (datos == null) continue;
            for (ConFecha d : datos) {
                if (d.fecha().isAfter(desde) && !d.fecha().isAfter(hasta)) {
                    estado.aplicar(d);
                }
            }
        }
    }

    /** Pide la ventana si hace falta. Devuelve true si ya está disponible. */
    private boolean asegurarVentana(String tipo, Integer piloto, Instant instante) {
        long inicio = inicioVentana(instante);
        String clave = clave(tipo, piloto, inicio);
        if (ventanas.containsKey(clave)) {
            return true;
        }
        List<? extends ConFecha> compartida = cacheCompartido.get(sessionKey + ":" + clave);
        if (compartida != null) {
            ventanas.put(clave, compartida);
            return true;
        }
        if (pedidas.putIfAbsent(clave, Boolean.TRUE) == null) {
            descargas.submit(() -> {
                if (detenida) {
                    return;
                }
                Instant a = Instant.ofEpochSecond(inicio);
                Instant b = a.plusSeconds(VENTANA_SEG);
                String filtro = "&date>=" + OpenF1Cliente.fecha(a) + "&date<" + OpenF1Cliente.fecha(b);
                try {
                    List<? extends ConFecha> datos = "car".equals(tipo)
                            ? DatosOpenF1.muestrasAuto(cliente.get(
                                    "car_data?session_key=" + sessionKey + "&driver_number=" + piloto + filtro))
                            : DatosOpenF1.ubicaciones(cliente.get("location?session_key=" + sessionKey + filtro));
                    ventanas.put(clave, datos);
                    cacheCompartido.put(sessionKey + ":" + clave, datos);
                } catch (RuntimeException ex) {
                    // Sin telemetría para ese tramo: se continúa sin ella en lugar de trabar la repetición
                    log.debug("Ventana {} no disponible: {}", clave, ex.getMessage());
                    ventanas.put(clave, List.of());
                }
            });
        }
        return false;
    }

    private void liberarVentanasViejas() {
        long limite = inicioVentana(reloj) - VENTANA_SEG;
        ventanas.keySet().removeIf(k -> Long.parseLong(k.substring(k.lastIndexOf('-') + 1)) < limite);
        pedidas.keySet().removeIf(k -> Long.parseLong(k.substring(k.lastIndexOf('-') + 1)) < limite);
    }

    /** Piloto que sigue el (único) espectador de la repetición. */
    private Integer focoActual() {
        Integer f = suscripciones.stream().findFirst().map(Suscripcion::foco).orElse(null);
        if (f != null) return f;
        return estado.torre().stream().filter(x -> x.posicion() < 99).findFirst()
                .map(FilaTorre::numero).orElse(null);
    }

    /** Inicio de la vuelta N: el primero en empezarla es el líder. Vacío si OpenF1 no la informa. */
    static Optional<Instant> inicioDeVuelta(List<? extends ConFecha> eventos, int vuelta) {
        return eventos.stream()
                .filter(e -> e instanceof DatosOpenF1.Vuelta v && v.vuelta() == vuelta)
                .map(e -> ((DatosOpenF1.Vuelta) e).inicio())
                .min(Instant::compareTo);
    }

    /** Cantidad de vueltas de la sesión (la mayor vuelta registrada). */
    static int totalVueltas(List<? extends ConFecha> eventos) {
        return eventos.stream().filter(e -> e instanceof DatosOpenF1.Vuelta)
                .mapToInt(e -> ((DatosOpenF1.Vuelta) e).vuelta()).max().orElse(0);
    }

    private static long inicioVentana(Instant i) {
        return Math.floorDiv(i.getEpochSecond(), VENTANA_SEG) * VENTANA_SEG;
    }

    private static String clave(String tipo, Integer piloto, long inicio) {
        return tipo + (piloto != null ? "-" + piloto : "") + "-" + inicio;
    }

    @Override
    public void detener() {
        detenida = true;
        ventanas.clear();
    }

    public void setVelocidad(double v) {
        this.velocidad = v;
    }

    public void setPausada(boolean p) {
        this.pausada = p;
    }

    /** Agrupa los datos completos de una sesión (cacheados por el servicio y compartidos entre repeticiones). */
    public record DatosSesion(SesionTelemetria sesion,
                              List<DatosOpenF1.Piloto> pilotos, List<DatosOpenF1.Stint> stints,
                              List<ConFecha> eventos) {
    }
}
