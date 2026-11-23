package com.tfi.motorsport.backend.services.telemetria.transmision;

import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.ClimaMuestra;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Control;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Intervalo;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.MuestraAuto;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Pit;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Piloto;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Posicion;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Stint;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Ubicacion;
import com.tfi.motorsport.backend.services.telemetria.transmision.DatosOpenF1.Vuelta;
import com.tfi.motorsport.backend.dtos.telemetria.TransmisionDtos.Clima;
import com.tfi.motorsport.backend.dtos.telemetria.TransmisionDtos.FilaTorre;
import com.tfi.motorsport.backend.dtos.telemetria.TransmisionDtos.Foco;
import com.tfi.motorsport.backend.dtos.telemetria.TransmisionDtos.MensajeControl;
import com.tfi.motorsport.backend.dtos.telemetria.TransmisionDtos.PuntoMapa;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * ESTADO DE UNA CARRERA en un instante dado. Se alimenta con eventos de OpenF1 en orden cronológico
 * (los mismos en vivo y en repetición) y puede devolver una "foto" para enviar al frontend.
 *
 * Es thread-safe (métodos synchronized): en vivo lo actualiza el hilo que consulta OpenF1
 * mientras el hilo del reloj arma las fotos para los espectadores.
 */
public class EstadoCarrera {

    private static final int MAX_MENSAJES = 12;
    private static final int LARGO_TRAZA = 110;          // ~30 s de telemetría a 3.7 Hz
    private static final int MAX_PUNTOS_TRAZADO = 1500;  // nube de puntos que dibuja el circuito
    private static final Duration DURACION_BOXES = Duration.ofSeconds(30);

    private final Map<Integer, Piloto> pilotos = new LinkedHashMap<>();
    private final Map<Integer, Integer> posicion = new HashMap<>();
    private final Map<Integer, String> gap = new HashMap<>();
    private final Map<Integer, String> intervalo = new HashMap<>();
    private final Map<Integer, BigDecimal> ultimaVuelta = new HashMap<>();
    private final Map<Integer, BigDecimal> mejorVuelta = new HashMap<>();
    private final Map<Integer, Integer> vueltas = new HashMap<>();
    private final Map<Integer, Instant> ultimoPit = new HashMap<>();
    private final Map<Integer, List<Stint>> stints = new HashMap<>();
    private final Map<Integer, Deque<MuestraAuto>> trazas = new HashMap<>();
    private final Map<Integer, Ubicacion> ubicacion = new HashMap<>();
    private final Deque<MensajeControl> mensajes = new ArrayDeque<>();
    private final List<double[]> trazado = new ArrayList<>();
    private long muestrasUbicacion;
    private int versionTrazado;
    private String estadoPista = "VERDE";
    private Clima clima;
    private Instant reloj;
    /** Vueltas de la carrera, si se conocen (en repetición sí; en vivo OpenF1 no lo informa). */
    private Integer totalVueltas;

    // ------------------------------------------------------------------ carga inicial

    public synchronized void setPilotos(List<Piloto> lista) {
        lista.forEach(p -> pilotos.put(p.numero(), p));
    }

    public synchronized void setStints(List<Stint> lista) {
        stints.clear();
        lista.forEach(s -> stints.computeIfAbsent(s.numero(), k -> new ArrayList<>()).add(s));
    }

    public synchronized void setTotalVueltas(Integer total) {
        totalVueltas = total != null && total > 0 ? total : null;
    }

    public synchronized boolean tienePilotos() {
        return !pilotos.isEmpty();
    }

    public synchronized List<Piloto> pilotos() {
        return List.copyOf(pilotos.values());
    }

    // ------------------------------------------------------------------ aplicar eventos

    public synchronized void aplicar(Object evento) {
        switch (evento) {
            case Posicion p -> posicion.put(p.numero(), p.posicion());
            case Intervalo i -> {
                gap.put(i.numero(), i.gap());
                intervalo.put(i.numero(), i.intervalo());
            }
            case Vuelta v -> aplicarVuelta(v);
            case Control c -> aplicarControl(c);
            case ClimaMuestra c -> clima = new Clima(c.aire(), c.pista(), c.humedad(), c.lluvia(), c.viento());
            case Pit p -> ultimoPit.put(p.numero(), p.fecha());
            case MuestraAuto m -> {
                Deque<MuestraAuto> t = trazas.computeIfAbsent(m.numero(), k -> new ArrayDeque<>());
                t.addLast(m);
                while (t.size() > LARGO_TRAZA) t.pollFirst();
            }
            case Ubicacion u -> aplicarUbicacion(u);
            default -> {
            }
        }
    }

    private void aplicarVuelta(Vuelta v) {
        vueltas.merge(v.numero(), v.vuelta(), Math::max);
        // Las vueltas de salida de boxes no cuentan como "última vuelta" representativa
        if (v.duracion() != null && !v.salidaBoxes()) {
            ultimaVuelta.put(v.numero(), v.duracion());
            mejorVuelta.merge(v.numero(), v.duracion(), (a, b) -> a.compareTo(b) <= 0 ? a : b);
        }
    }

    private void aplicarControl(Control c) {
        mensajes.addFirst(new MensajeControl(c.fecha(), c.vuelta(), c.bandera(), c.categoria(), c.mensaje()));
        while (mensajes.size() > MAX_MENSAJES) mensajes.pollLast();
        String msg = c.mensaje() != null ? c.mensaje().toUpperCase(Locale.ROOT) : "";
        String flag = c.bandera() != null ? c.bandera().toUpperCase(Locale.ROOT) : "";
        boolean dePista = c.alcance() == null || "TRACK".equalsIgnoreCase(c.alcance());
        if (msg.contains("VIRTUAL SAFETY CAR DEPLOYED")) estadoPista = "VSC";
        else if (msg.contains("SAFETY CAR DEPLOYED")) estadoPista = "SC";
        else if (msg.contains("SAFETY CAR IN THIS LAP") || msg.contains("VSC ENDING")) estadoPista = "VERDE";
        else if ("RED".equals(flag)) estadoPista = "ROJA";
        else if ("CHEQUERED".equals(flag)) estadoPista = "CUADROS";
        else if (dePista && ("GREEN".equals(flag) || "CLEAR".equals(flag)) && !"CUADROS".equals(estadoPista)) {
            // Bandera verde de pista: termina cualquier neutralización (salvo que la sesión ya haya terminado)
            estadoPista = "VERDE";
        } else if (dePista && flag.contains("YELLOW")) estadoPista = "AMARILLA";
    }

    private void aplicarUbicacion(Ubicacion u) {
        ubicacion.put(u.numero(), u);
        // El contorno del circuito se arma con las posiciones de TODOS los autos (una de cada tres):
        // como están repartidos por la pista, en pocos segundos queda dibujado el circuito completo.
        if (trazado.size() < MAX_PUNTOS_TRAZADO && (muestrasUbicacion++ % 3 == 0)) {
            trazado.add(new double[]{u.x(), u.y()});
            if (trazado.size() % 100 == 0) versionTrazado++;
        }
    }

    public synchronized void setReloj(Instant instante) {
        reloj = instante;
    }

    public synchronized Instant reloj() {
        return reloj;
    }

    // ------------------------------------------------------------------ fotos

    public synchronized List<FilaTorre> torre() {
        BigDecimal mejorGeneral = mejorVuelta.values().stream().min(Comparator.naturalOrder()).orElse(null);
        List<FilaTorre> filas = new ArrayList<>();
        for (Piloto p : pilotos.values()) {
            Integer pos = posicion.get(p.numero());
            Integer v = vueltas.get(p.numero());
            Stint s = stintActual(p.numero(), v == null ? 1 : v + 1);
            Instant pit = ultimoPit.get(p.numero());
            boolean enBoxes = pit != null && reloj != null && !reloj.isBefore(pit)
                    && Duration.between(pit, reloj).compareTo(DURACION_BOXES) < 0;
            BigDecimal mejor = mejorVuelta.get(p.numero());
            filas.add(new FilaTorre(pos == null ? 99 : pos, p.numero(), p.acronimo(), p.nombre(), p.equipo(), p.color(),
                    pos != null && pos == 1 ? "Líder" : gap.get(p.numero()),
                    pos != null && pos == 1 ? null : intervalo.get(p.numero()),
                    ultimaVuelta.get(p.numero()), mejor, v,
                    s != null ? s.compuesto() : null,
                    s != null && s.edadInicial() != null && v != null ? s.edadInicial() + Math.max(0, v + 1 - s.vueltaInicio()) : null,
                    enBoxes, mejor != null && mejor.equals(mejorGeneral)));
        }
        filas.sort(Comparator.comparingInt(FilaTorre::posicion));
        return filas;
    }

    private Stint stintActual(int numero, int vuelta) {
        Stint actual = null;
        for (Stint s : stints.getOrDefault(numero, List.of())) {
            if (s.vueltaInicio() <= vuelta) actual = s;
        }
        return actual;
    }

    /**
     * Vuelta EN CURSO del líder, como en la TV ("Vuelta 20/58"): vueltas completadas + 1.
     * Al largar todavía no completó ninguna (OpenF1 informa posiciones antes que vueltas): vuelta 1.
     * Nunca supera el total y, con la bandera a cuadros, muestra la última vuelta completada.
     * Devuelve null si todavía no hay líder.
     */
    public synchronized Integer vueltaLider() {
        Integer lider = posicion.entrySet().stream().filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey).findFirst().orElse(null);
        if (lider == null) return null;
        int completadas = Objects.requireNonNullElse(vueltas.get(lider), 0);
        if ("CUADROS".equals(estadoPista)) {
            return totalVueltas != null ? totalVueltas : Math.max(completadas, 1);
        }
        int enCurso = completadas + 1;
        return totalVueltas != null ? Math.min(enCurso, totalVueltas) : enCurso;
    }

    public synchronized Foco foco(Integer numero) {
        if (numero == null) return null;
        Piloto p = pilotos.get(numero);
        Deque<MuestraAuto> t = trazas.getOrDefault(numero, new ArrayDeque<>());
        MuestraAuto u = t.peekLast();
        return new Foco(numero, p != null ? p.acronimo() : String.valueOf(numero), p != null ? p.color() : null,
                u != null ? u.velocidad() : null, u != null ? u.rpm() : null, u != null ? u.marcha() : null,
                u != null ? u.acelerador() : null, u != null ? u.freno() : null,
                u != null && u.drs() != null && u.drs() >= 10,
                t.stream().map(MuestraAuto::velocidad).toList(),
                t.stream().map(MuestraAuto::acelerador).toList(),
                t.stream().map(MuestraAuto::freno).toList());
    }

    public synchronized List<PuntoMapa> mapa() {
        List<PuntoMapa> puntos = new ArrayList<>();
        for (Ubicacion u : ubicacion.values()) {
            Piloto p = pilotos.get(u.numero());
            puntos.add(new PuntoMapa(u.numero(), p != null ? p.acronimo() : String.valueOf(u.numero()),
                    p != null ? p.color() : DatosOpenF1.COLOR_POR_DEFECTO, u.x(), u.y()));
        }
        return puntos;
    }

    /** Versión del contorno: el servicio lo reenvía solo cuando cambia. */
    public synchronized int versionTrazado() {
        return versionTrazado;
    }

    public synchronized List<double[]> trazado() {
        return List.copyOf(trazado);
    }

    public synchronized List<MensajeControl> mensajes() {
        return List.copyOf(mensajes);
    }

    public synchronized Clima clima() {
        return clima;
    }

    public synchronized String estadoPista() {
        return estadoPista;
    }
}
