package com.tfi.motorsport.backend.services.telemetria;

import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.ClimaFoto;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.FilaFoto;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.MensajeCarrera;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.PilotoVersus;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.Versus;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.VueltaComparada;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.VueltaDetalle;
import com.tfi.motorsport.backend.dtos.telemetria.TelemetriaDtos.VueltaEnCurso;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.ClimaMuestra;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.ConFecha;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Control;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Intervalo;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Pit;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Piloto;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Posicion;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Stint;
import com.tfi.motorsport.backend.services.telemetria.DatosOpenF1.Vuelta;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Arma la "foto" de una sesión en un instante T: para cada dato se toma el último registro de OpenF1
 * con fecha menor o igual a T. Es la misma cuenta para el vivo (T = ahora), para la repetición
 * (T avanza segundo a segundo) y para el resultado final (T = el último dato).
 *
 * Son funciones puras (sin Spring, sin red, sin estado): se prueban con datos armados a mano.
 * Todas las listas de DatosSesion vienen ordenadas por fecha (así las deja DatosOpenF1).
 */
public final class CalculadoraFoto {

    /** Mensajes de la dirección de carrera que se muestran (los más recientes). */
    public static final int MAX_MENSAJES = 8;
    /** Vueltas que se comparan en el versus (las últimas). */
    public static final int MAX_VUELTAS_VERSUS = 8;
    /**
     * Una vuelta sin tiempo que empezó hace más de esto no está "en curso": el piloto abandonó o entró a boxes
     * (la vuelta más lenta de una carrera normal ronda 1:50; con auto de seguridad, algo más).
     */
    public static final Duration MAX_DURACION_VUELTA = Duration.ofMinutes(4);

    private CalculadoraFoto() {
    }

    /** Todo lo que se descarga de una sesión para armar sus fotos. */
    public record DatosSesion(List<Piloto> pilotos, List<Posicion> posiciones, List<Intervalo> intervalos,
                              List<Vuelta> vueltas, List<Stint> stints, List<Pit> pits,
                              List<Control> controles, List<ClimaMuestra> clima) {
    }

    /** Tanda de la clasificación en un instante: su número (1, 2 o 3) y cuándo empezó. */
    public record Tanda(int numero, Instant inicio) {
    }

    // ------------------------------------------------------------------ el instante de la foto

    /** La vuelta más alta que completó alguien (en una carrera, las vueltas que dio el ganador). */
    public static int totalVueltas(List<Vuelta> vueltas) {
        return vueltas.stream().filter(Vuelta::terminada).mapToInt(Vuelta::vuelta).max().orElse(0);
    }

    /**
     * Cuándo terminó la vuelta N el primero que la completó (el líder), o null si nadie la completó.
     * Si nadie tiene tiempo en esa vuelta (en carrera OpenF1 no informa el de la vuelta 1, que se larga desde
     * la grilla), terminó cuando el primero empezó la siguiente.
     */
    public static Instant finDeVuelta(List<Vuelta> vueltas, int numeroVuelta) {
        return vueltas.stream()
                .filter(v -> v.vuelta() == numeroVuelta && v.terminada())
                .map(Vuelta::fecha)
                .min(Comparator.naturalOrder())
                .or(() -> vueltas.stream()
                        .filter(v -> v.vuelta() == numeroVuelta + 1)
                        .map(Vuelta::inicio)
                        .min(Comparator.naturalOrder()))
                .orElse(null);
    }

    /** El primer momento con datos (posiciones, vueltas o intervalos), o null si OpenF1 no tiene nada. */
    public static Instant primerInstante(DatosSesion datos) {
        return extremo(datos, lista -> lista.get(0).fecha(), Comparator.reverseOrder());
    }

    /** El último momento con datos de la sesión: la foto final. null si OpenF1 no tiene nada. */
    public static Instant ultimoInstante(DatosSesion datos) {
        return extremo(datos, lista -> lista.get(lista.size() - 1).fecha(), Comparator.naturalOrder());
    }

    private static Instant extremo(DatosSesion datos, Function<List<? extends ConFecha>, Instant> fechaDe,
                                   Comparator<Instant> orden) {
        return List.of(datos.posiciones(), datos.vueltas(), datos.intervalos()).stream()
                .filter(lista -> !lista.isEmpty())
                .map(fechaDe)
                .max(orden)
                .orElse(null);
    }

    /** Vueltas que completó el líder hasta el instante (la vuelta que se informa en la foto). */
    public static int vueltasHasta(List<Vuelta> vueltas, Instant instante) {
        return completadasPorPiloto(vueltas, instante).values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    /**
     * Vueltas que completó cada piloto hasta el instante: las que tienen tiempo y también las que dejó atrás al
     * empezar la siguiente, aunque no tengan tiempo (en carrera OpenF1 informa la vuelta 1 sin tiempo).
     */
    private static Map<Integer, Integer> completadasPorPiloto(List<Vuelta> vueltas, Instant instante) {
        Map<Integer, Integer> completadas = new HashMap<>();
        for (Vuelta vuelta : vueltas) {
            if (vuelta.terminada() && !vuelta.fecha().isAfter(instante)) {
                completadas.merge(vuelta.numero(), vuelta.vuelta(), Math::max);
            } else if (!vuelta.inicio().isAfter(instante)) {
                completadas.merge(vuelta.numero(), vuelta.vuelta() - 1, Math::max);
            }
        }
        return completadas;
    }

    /**
     * La tanda de la clasificación en el instante: la dirección de carrera informa en cada mensaje a qué
     * tanda pertenece. null si la sesión no es una clasificación (o todavía no empezó).
     */
    public static Tanda tanda(List<Control> controles, Instant instante) {
        Tanda actual = null;
        for (Control control : controles) {
            if (control.fecha().isAfter(instante)) {
                break;
            }
            if (control.tanda() != null && (actual == null || actual.numero() != control.tanda())) {
                actual = new Tanda(control.tanda(), control.fecha());
            }
        }
        return actual;
    }

    /** El último registro con fecha <= instante, o null si todos son posteriores. */
    public static <T extends ConFecha> T ultimoHasta(List<T> registros, Instant instante) {
        T ultimo = null;
        for (T registro : registros) {
            if (registro.fecha().isAfter(instante)) {
                break;
            }
            ultimo = registro;
        }
        return ultimo;
    }

    // ------------------------------------------------------------------ la torre de tiempos

    /**
     * La torre en el instante: una fila por piloto, ordenadas por posición.
     * En la clasificación la mejor vuelta es la de la tanda en curso (cada tanda empieza de cero, como en la TV).
     */
    public static List<FilaFoto> filas(DatosSesion datos, Instant instante) {
        Map<Integer, Integer> posicion = new HashMap<>();
        for (Posicion registro : datos.posiciones()) {
            if (registro.fecha().isAfter(instante)) {
                break;
            }
            posicion.put(registro.numero(), registro.posicion());
        }
        Map<Integer, Intervalo> intervalo = new HashMap<>();
        for (Intervalo registro : datos.intervalos()) {
            if (registro.fecha().isAfter(instante)) {
                break;
            }
            intervalo.put(registro.numero(), registro);
        }
        Tanda tanda = tanda(datos.controles(), instante);
        Map<Integer, Vuelta> ultimaVuelta = new HashMap<>();
        Map<Integer, BigDecimal> mejorVuelta = new HashMap<>();
        Map<Integer, BigDecimal> mejorDeLaTanda = new HashMap<>();
        for (Vuelta vuelta : datos.vueltas()) {
            if (vuelta.fecha().isAfter(instante)) {
                break;
            }
            if (vuelta.terminada()) {
                ultimaVuelta.put(vuelta.numero(), vuelta);
                mejorVuelta.merge(vuelta.numero(), vuelta.duracion(), BigDecimal::min);
                if (tanda != null && !vuelta.fecha().isBefore(tanda.inicio())) {
                    mejorDeLaTanda.merge(vuelta.numero(), vuelta.duracion(), BigDecimal::min);
                }
            }
        }
        Map<Integer, Integer> vueltasCompletadas = completadasPorPiloto(datos.vueltas(), instante);
        Map<Integer, Integer> paradas = paradasHasta(datos.pits(), instante);
        Map<Integer, BigDecimal> mejorAMostrar = tanda != null ? mejorDeLaTanda : mejorVuelta;
        BigDecimal vueltaRapida = mejorAMostrar.values().stream().min(Comparator.naturalOrder()).orElse(null);

        List<FilaFoto> filas = new ArrayList<>();
        for (Piloto piloto : datos.pilotos()) {
            int numero = piloto.numero();
            Vuelta ultima = ultimaVuelta.get(numero);
            int completadas = vueltasCompletadas.getOrDefault(numero, 0);
            Stint stint = stintEnVuelta(datos.stints(), numero, completadas + 1);
            // El líder no tiene diferencia (OpenF1 a veces le manda +0.000)
            Intervalo diferencias = Integer.valueOf(1).equals(posicion.get(numero)) ? null : intervalo.get(numero);
            // Quien no marcó tiempo en esta tanda (eliminado o sin vuelta todavía) muestra su mejor vuelta anterior
            BigDecimal mejor = mejorAMostrar.containsKey(numero) ? mejorAMostrar.get(numero) : mejorVuelta.get(numero);
            filas.add(new FilaFoto(posicion.get(numero), numero, piloto.acronimo(), piloto.nombre(), piloto.equipo(),
                    piloto.color(),
                    diferencias != null ? diferencias.gap() : null,
                    diferencias != null ? diferencias.intervalo() : null,
                    completadas > 0 ? completadas : null,
                    ultima != null ? ultima.duracion() : null,
                    mejor,
                    mejorAMostrar.containsKey(numero) && mejorAMostrar.get(numero).compareTo(vueltaRapida) == 0,
                    stint != null ? stint.compuesto() : null,
                    edadNeumatico(stint, completadas + 1),
                    paradas.getOrDefault(numero, 0)));
        }
        filas.sort(Comparator.comparing(FilaFoto::posicion, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(FilaFoto::numero));
        // Prácticas y clasificaciones no tienen intervalos: la diferencia es con la mejor vuelta del primero
        return datos.intervalos().isEmpty() ? diferenciasPorMejorVuelta(filas, mejorAMostrar) : filas;
    }

    // ------------------------------------------------------------------ versus

    /**
     * Dos pilotos frente a frente hasta el instante: última y mejor vuelta con sus sectores, mejores sectores,
     * velocidad máxima, paradas y neumático, y sus últimas vueltas comparadas una por una.
     * Las vueltas se comparan solo en carrera y sprint: en práctica y clasificación la vuelta N de cada uno
     * puede ser lanzada o de salida, así que se comparan las mejores vueltas y los sectores.
     */
    public static Versus versus(DatosSesion datos, Instant instante, List<FilaFoto> filas, int numeroA, int numeroB,
                                boolean compararVueltas) {
        List<Vuelta> hastaElInstante = new ArrayList<>();
        for (Vuelta vuelta : datos.vueltas()) {
            if (vuelta.fecha().isAfter(instante)) {
                break;
            }
            if (vuelta.terminada()) {
                hastaElInstante.add(vuelta);
            }
        }
        List<BigDecimal> mejoresSectoresSesion = mejoresSectores(hastaElInstante);
        PilotoVersus pilotoA = pilotoVersus(numeroA, datos, hastaElInstante, instante, filas);
        PilotoVersus pilotoB = pilotoVersus(numeroB, datos, hastaElInstante, instante, filas);
        return new Versus(pilotoA, pilotoB, mejoresSectoresSesion,
                compararVueltas ? vueltasComparadas(hastaElInstante, numeroA, numeroB) : List.of());
    }

    private static PilotoVersus pilotoVersus(int numero, DatosSesion datos, List<Vuelta> vueltas, Instant instante,
                                             List<FilaFoto> filas) {
        List<Pit> pits = datos.pits();
        String foto = datos.pilotos().stream().filter(p -> p.numero() == numero)
                .map(Piloto::foto).filter(Objects::nonNull).findFirst().orElse(null);
        List<Vuelta> delPiloto = vueltas.stream().filter(v -> v.numero() == numero).toList();
        Vuelta ultima = delPiloto.isEmpty() ? null : delPiloto.get(delPiloto.size() - 1);
        Vuelta mejor = delPiloto.stream().filter(v -> v.duracion() != null)
                .min(Comparator.comparing(Vuelta::duracion)).orElse(null);
        Integer velocidadMaxima = delPiloto.stream()
                .flatMap(v -> Arrays.asList(v.velocidadI1(), v.velocidadI2(), v.velocidadTrampa()).stream())
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
        Pit ultimaParada = ultimoHasta(pits.stream().filter(p -> p.numero() == numero).toList(), instante);
        FilaFoto fila = filas.stream().filter(f -> f.numero() == numero).findFirst().orElse(null);
        return new PilotoVersus(numero,
                fila != null ? fila.acronimo() : String.valueOf(numero),
                fila != null ? fila.nombre() : null,
                fila != null ? fila.equipo() : null,
                fila != null ? fila.color() : DatosOpenF1.COLOR_POR_DEFECTO,
                foto,
                fila != null ? fila.posicion() : null,
                ultima != null ? ultima.vuelta() : null,
                vueltaEnCurso(datos.vueltas(), numero, instante),
                detalle(ultima), detalle(mejor), mejoresSectores(delPiloto), velocidadMaxima,
                fila != null ? fila.paradas() : 0,
                ultimaParada != null ? ultimaParada.duracion() : null,
                fila != null ? fila.neumatico() : null,
                fila != null ? fila.edadNeumatico() : null);
    }

    /**
     * La vuelta que el piloto está dando en el instante: empezó antes y todavía no terminó (o no tiene tiempo).
     * Cada sector se muestra recién cuando lo completó: el sector 1 termina en inicio + S1 y el 2 en inicio + S1 + S2.
     * null si no está en pista (todavía no largó, abandonó o está en boxes).
     */
    public static VueltaEnCurso vueltaEnCurso(List<Vuelta> vueltas, int numero, Instant instante) {
        Vuelta enCurso = null;
        for (Vuelta vuelta : vueltas) {
            boolean empezo = vuelta.numero() == numero && !vuelta.inicio().isAfter(instante);
            boolean sinTerminar = vuelta.terminada() ? vuelta.fecha().isAfter(instante)
                    : Duration.between(vuelta.inicio(), instante).compareTo(MAX_DURACION_VUELTA) < 0;
            if (empezo && sinTerminar && (enCurso == null || vuelta.vuelta() > enCurso.vuelta())) {
                enCurso = vuelta;
            }
        }
        if (enCurso == null) {
            return null;
        }
        Instant finSector1 = sumar(enCurso.inicio(), enCurso.sector1());
        boolean hizoSector1 = finSector1 != null && !finSector1.isAfter(instante);
        Instant finSector2 = hizoSector1 ? sumar(finSector1, enCurso.sector2()) : null;
        boolean hizoSector2 = finSector2 != null && !finSector2.isAfter(instante);
        return new VueltaEnCurso(enCurso.vuelta(), Duration.between(enCurso.inicio(), instante).toSeconds(),
                hizoSector1 ? enCurso.sector1() : null, hizoSector1 ? enCurso.velocidadI1() : null,
                hizoSector2 ? enCurso.sector2() : null, hizoSector2 ? enCurso.velocidadI2() : null);
    }

    /** El instante más los segundos (con milésimas), o null si no se conocen los segundos. */
    private static Instant sumar(Instant instante, BigDecimal segundos) {
        return segundos == null ? null : instante.plusMillis(segundos.movePointRight(3).longValue());
    }

    /** Las últimas vueltas que dieron los dos, la más reciente primero, con quién fue más rápido y por cuánto. */
    private static List<VueltaComparada> vueltasComparadas(List<Vuelta> vueltas, int numeroA, int numeroB) {
        Map<Integer, BigDecimal> tiemposA = new TreeMap<>(Comparator.reverseOrder());
        Map<Integer, BigDecimal> tiemposB = new HashMap<>();
        for (Vuelta vuelta : vueltas) {
            if (vuelta.duracion() == null) {
                continue;
            }
            if (vuelta.numero() == numeroA) {
                tiemposA.put(vuelta.vuelta(), vuelta.duracion());
            } else if (vuelta.numero() == numeroB) {
                tiemposB.put(vuelta.vuelta(), vuelta.duracion());
            }
        }
        List<VueltaComparada> comparadas = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> entrada : tiemposA.entrySet()) {
            BigDecimal tiempoB = tiemposB.get(entrada.getKey());
            if (tiempoB == null) {
                continue;
            }
            BigDecimal tiempoA = entrada.getValue();
            int comparacion = tiempoA.compareTo(tiempoB);
            comparadas.add(new VueltaComparada(entrada.getKey(), tiempoA, tiempoB,
                    comparacion == 0 ? null : (comparacion < 0 ? numeroA : numeroB),
                    String.format(Locale.ROOT, "%.3f", tiempoA.subtract(tiempoB).abs())));
            if (comparadas.size() == MAX_VUELTAS_VERSUS) {
                break;
            }
        }
        return List.copyOf(comparadas);
    }

    /** El mejor tiempo de cada sector entre las vueltas dadas (pueden ser de vueltas distintas). */
    private static List<BigDecimal> mejoresSectores(List<Vuelta> vueltas) {
        List<Function<Vuelta, BigDecimal>> sectores = List.of(Vuelta::sector1, Vuelta::sector2, Vuelta::sector3);
        List<BigDecimal> mejores = new ArrayList<>();
        for (Function<Vuelta, BigDecimal> sector : sectores) {
            mejores.add(vueltas.stream().map(sector).filter(Objects::nonNull)
                    .min(Comparator.naturalOrder()).orElse(null));
        }
        return Collections.unmodifiableList(mejores);
    }

    private static VueltaDetalle detalle(Vuelta vuelta) {
        return vuelta == null ? null : new VueltaDetalle(vuelta.vuelta(), vuelta.duracion(), vuelta.sector1(),
                vuelta.sector2(), vuelta.sector3(), vuelta.velocidadI1(), vuelta.velocidadI2(), vuelta.velocidadTrampa());
    }

    // ------------------------------------------------------------------ mensajes y clima

    /** Los mensajes de la dirección de carrera hasta el instante, el más reciente primero. */
    public static List<MensajeCarrera> mensajes(List<Control> controles, Instant instante) {
        List<MensajeCarrera> mensajes = new ArrayList<>();
        for (Control control : controles) {
            if (control.fecha().isAfter(instante)) {
                break;
            }
            mensajes.add(0, new MensajeCarrera(control.fecha(), control.vuelta(), control.bandera(), control.mensaje()));
        }
        return List.copyOf(mensajes.subList(0, Math.min(MAX_MENSAJES, mensajes.size())));
    }

    /** El último registro del clima hasta el instante (OpenF1 lo mide una vez por minuto). */
    public static ClimaFoto clima(List<ClimaMuestra> muestras, Instant instante) {
        ClimaMuestra muestra = ultimoHasta(muestras, instante);
        return muestra == null ? null
                : new ClimaFoto(muestra.aire(), muestra.pista(), muestra.humedad(), muestra.lluvia(), muestra.viento());
    }

    // ------------------------------------------------------------------ auxiliares

    private static Map<Integer, Integer> paradasHasta(List<Pit> pits, Instant instante) {
        Map<Integer, Integer> paradas = new HashMap<>();
        for (Pit pit : pits) {
            if (pit.fecha().isAfter(instante)) {
                break;
            }
            paradas.merge(pit.numero(), 1, Integer::sum);
        }
        return paradas;
    }

    /** El stint (juego de neumáticos) que el piloto usa en esa vuelta: el último que empezó antes o en ella. */
    private static Stint stintEnVuelta(List<Stint> stints, int numero, int vuelta) {
        Stint actual = null;
        for (Stint stint : stints) {
            if (stint.numero() == numero && stint.vueltaInicio() <= vuelta) {
                actual = stint;
            }
        }
        return actual;
    }

    /** Vueltas de uso: las que ya traía el neumático más las que dio en este stint. */
    private static Integer edadNeumatico(Stint stint, int vuelta) {
        if (stint == null || stint.edadInicial() == null) {
            return null;
        }
        return stint.edadInicial() + (vuelta - stint.vueltaInicio());
    }

    /**
     * Diferencia con la mejor vuelta del primero que marcó tiempo y con la del de adelante ("+0.123").
     * Solo tienen diferencia los que marcaron tiempo (en la clasificación, en la tanda en curso).
     */
    private static List<FilaFoto> diferenciasPorMejorVuelta(List<FilaFoto> filas, Map<Integer, BigDecimal> mejores) {
        List<FilaFoto> resultado = new ArrayList<>();
        BigDecimal delPrimero = null;
        BigDecimal delAnterior = null;
        for (FilaFoto fila : filas) {
            BigDecimal mejor = mejores.get(fila.numero());
            String diferencia = null;
            String intervalo = null;
            if (mejor != null) {
                if (delPrimero == null) {
                    delPrimero = mejor;
                } else {
                    diferencia = diferencia(mejor, delPrimero);
                    intervalo = delAnterior != null ? diferencia(mejor, delAnterior) : null;
                }
                delAnterior = mejor;
            }
            resultado.add(new FilaFoto(fila.posicion(), fila.numero(), fila.acronimo(), fila.nombre(), fila.equipo(),
                    fila.color(), diferencia, intervalo, fila.vueltas(), fila.ultimaVuelta(), fila.mejorVuelta(),
                    fila.vueltaRapida(), fila.neumatico(), fila.edadNeumatico(), fila.paradas()));
        }
        return resultado;
    }

    private static String diferencia(BigDecimal tiempo, BigDecimal referencia) {
        return String.format(Locale.ROOT, "%+.3f", tiempo.subtract(referencia));
    }
}
