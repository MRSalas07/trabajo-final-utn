package com.tfi.motorsport.backend.clients.clientHttpExternal;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Funciones Helpers para leer JSON de forma TOLERANTE.
 *
 * Las APIs externas pueden omitir campos, enviar números como texto ("25") o valores vacíos.
 * Estos métodos devuelven null en lugar de lanzar excepciones, y la capa de normalización
 * decide qué hacer con los datos faltantes.
 */
public class FormatJson {

    public FormatJson() {   }

    /** Navega una ruta de campos: Json.at(nodo, "Driver", "givenName"). */
    public static JsonNode at(JsonNode nodo, String... campos) {
        JsonNode valor = nodo;
        for (String campo : campos) {
            if (valor == null || valor.isNull() || valor.isMissingNode()) {
                return null;
            }
            valor = valor.get(campo);
        }
        return (valor == null || valor.isNull() || valor.isMissingNode()) ? null : valor;
    }

    public static String aString(JsonNode node, String... string) {
        JsonNode campo = at(node, string);
        if (campo == null) {
            return null;
        }
        String str = campo.isString() ? campo.asString() : campo.toString();
        return str.isBlank() ? null : str.trim();
    }

    public static Integer aInt(JsonNode node, String... string) {
        JsonNode valor = at(node, string);
        if (valor == null) {
            return null;
        }
        if (valor.isNumber()) {
            return valor.asInt();
        }
        try {
            return Integer.parseInt(valor.asString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Long aLong(JsonNode node, String... string) {
        JsonNode valor = at(node, string);
        if (valor == null) {
            return null;
        }
        if (valor.isNumber()) {
            return valor.asLong();
        }
        try {
            return Long.parseLong(valor.asString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static BigDecimal aDecimal(JsonNode node, String... string) {
        JsonNode valor = at(node, string);
        if (valor == null) {
            return null;
        }
        try {
            return valor.isNumber() ? valor.decimalValue() : new BigDecimal(valor.asString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean booleano(JsonNode node, String... string) {
        JsonNode valor = at(node, string);
        return valor != null && (valor.isBoolean() ? valor.asBoolean() : "true".equalsIgnoreCase(valor.asString()));
    }

    public static LocalDate aDate(JsonNode node, String... string) {
        String str = aString(node, string);
        if (str == null) {
            return null;
        }
        try {
            return LocalDate.parse(str.length() > 10 ? str.substring(0, 10) : str);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Interpreta una fecha-hora. Si el texto no trae zona (ej: "2025-01-23T17:05:00")
     * se asume UTC, que es lo que usan los proveedores integrados.
     */
    public static Instant instantUtc(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(texto);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(texto).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    /** Recorre un arreglo de forma segura (lista vacía si el nodo no es arreglo). */
    public static List<JsonNode> lista(JsonNode nodo, String... campos) {
        JsonNode array = campos.length == 0 ? nodo : at(nodo, campos);
        List<JsonNode> lista = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(lista::add);
        }
        return lista;
    }
}
