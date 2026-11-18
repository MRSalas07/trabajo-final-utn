package com.tfi.motorsport.backend.handlers.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Clase para manejar las excepciones de la comunicación con API externa
 */
public class IntegrationExternalException extends RuntimeException {

    public enum Tipo {
        TIMEOUT(HttpStatus.GATEWAY_TIMEOUT),
        LIMITE_USO(HttpStatus.TOO_MANY_REQUESTS),
        RESPUESTA_VACIA(HttpStatus.BAD_GATEWAY),
        FORMATO_INVALIDO(HttpStatus.BAD_GATEWAY),
        ERROR_PROVEEDOR(HttpStatus.BAD_GATEWAY),
        NO_DISPONIBLE(HttpStatus.SERVICE_UNAVAILABLE):

        private final HttpStatus estadoHttp;

        Tipo(HttpStatus estadoHttp) {
            this.estadoHttp = estadoHttp;
        }

        public HttpStatus getEstadoHttp() {
            return estadoHttp;
        }
    }

    private final String proveedor;
    private final Tipo tipo;


    public IntegrationExternalException(String proveedor, Tipo tipo, String mensaje) {
        this(proveedor, tipo, mensaje, null);
    }

    public IntegrationExternalException(String proveedor, Tipo tipo, String mensaje, Throwable causa) {
        super("[" + proveedor + "] " + mensaje, causa);
        this.proveedor = proveedor;
        this.tipo = tipo;
    }

    public String getProveedor() {
        return proveedor;
    }

    public Tipo getTipo() {
        return tipo;
    }

}
