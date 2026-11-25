package com.tfi.motorsport.backend.clients.adapter;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servidor HTTP local que responde como un proveedor externo (Jolpica o la API de WRC):
 * a cada ruta le corresponde un JSON. Lo que no está cargado responde 404.
 * Así los adaptadores se prueban de punta a punta (HTTP + lectura del JSON) sin salir a Internet.
 */
class ProveedorFalso implements AutoCloseable {

    private final HttpServer servidor;
    private final Map<String, String> respuestas = new HashMap<>();
    private final List<String> pedidos = new ArrayList<>();

    ProveedorFalso() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            String ruta = intercambio.getRequestURI().getPath().substring(1);
            String consulta = intercambio.getRequestURI().getRawQuery();
            String completa = consulta == null ? ruta : ruta + "?" + consulta;
            synchronized (pedidos) {
                pedidos.add(completa);
            }
            // Primero se busca la ruta con sus parámetros; si no, la ruta sola
            String cuerpo = respuestas.getOrDefault(completa, respuestas.get(ruta));
            byte[] bytes = (cuerpo != null ? cuerpo : "{}").getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(cuerpo != null ? 200 : 404, bytes.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(bytes);
            }
        });
        servidor.start();
    }

    /** JSON escrito con comillas simples para que se lea mejor en los tests. */
    void responder(String ruta, String json) {
        respuestas.put(ruta, json.replace('\'', '"'));
    }

    String urlBase() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/";
    }

    List<String> pedidos() {
        synchronized (pedidos) {
            return List.copyOf(pedidos);
        }
    }

    @Override
    public void close() {
        servidor.stop(0);
    }
}
