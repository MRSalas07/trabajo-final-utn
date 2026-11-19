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
 * metodos utils para leer JSON de forma TOLERANTE.
 *
 * Las APIs externas pueden omitir campos, enviar números como texto ("25") o valores vacíos.
 * Estos métodos devuelven null en lugar de lanzar excepciones, y la capa de normalización
 * decide qué hacer con los datos faltantes.
 */
public class FormatJson {

    public FormatJson() {   }

    /** Navega una ruta de campos: Json.at(nodo, "Driver", "givenName"). */
    public static JsonNode at(JsonNode node, String... fields) {
        JsonNode current = node;
        for (String field : fields) {
            if (current == null || current.isNull() || current.isMissingNode()) {
                return null;
            }
            current = current.get(field);
        }
        return (current == null || current.isNull() || current.isMissingNode()) ? null : current;
    }

    public static String getString(JsonNode node, String... fields) {
        JsonNode value = at(node, fields);
        if (value == null) {
            return null;
        }
        String text = value.isString() ? value.asString() : value.toString();
        return text.isBlank() ? null : text.trim();
    }

    public static Integer getInt(JsonNode node, String... fields) {
        JsonNode value = at(node, fields);
        if (value == null) {
            return null;
        }
        if (value.isNumber()) {
            return value.asInt();
        }
        try {
            return Integer.parseInt(value.asString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Long getLong(JsonNode node, String... fields) {
        JsonNode value = at(node, fields);
        if (value == null) {
            return null;
        }
        if (value.isNumber()) {
            return value.asLong();
        }
        try {
            return Long.parseLong(value.asString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static BigDecimal getDecimal(JsonNode node, String... fields) {
        JsonNode value = at(node, fields);
        if (value == null) {
            return null;
        }
        try {
            return value.isNumber() ? value.decimalValue() : new BigDecimal(value.asString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean getBoolean(JsonNode node, String... fields) {
        JsonNode value = at(node, fields);
        return value != null && (value.isBoolean() ? value.asBoolean() : "true".equalsIgnoreCase(value.asString()));
    }

    public static LocalDate getDate(JsonNode node, String... fields) {
        String text = getString(node, fields);
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text.length() > 10 ? text.substring(0, 10) : text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Interpreta una fecha-hora. Si el texto no trae zona (ej: "2025-01-23T17:05:00")
     * se asume UTC, que es lo que usan los proveedores integrados.
     */
    public static Instant parseInstantUtc(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    /** Recorre un arreglo de forma segura (lista vacía si el nodo no es arreglo). */
    public static List<JsonNode> list(JsonNode node, String... fields) {
        JsonNode array = fields.length == 0 ? node : at(node, fields);
        List<JsonNode> result = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(result::add);
        }
        return result;
    }
}
