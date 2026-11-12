package com.tfi.motorsport.backend.common;

import java.time.Instant;

/**
 * Un instante expresado en UTC y en la zona horaria pedida por el usuario.
 * utc: "2025-03-16T04:00:00Z" | local: "2025-03-16T01:00:00" | zona: "America/Argentina/Buenos_Aires" | offset: "-03:00"
 */
public record Horario(Instant utc, String local, String zona, String offset) {
}
