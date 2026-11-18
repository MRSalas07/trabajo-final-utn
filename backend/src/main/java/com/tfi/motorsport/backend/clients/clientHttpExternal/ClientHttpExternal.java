package com.tfi.motorsport.backend.clients.clientHttpExternal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
@Slf4j
public class ClientHttpExternal {

    private final RestClient restClient;

    private final JsonMapper jsonMapper;
    
    public ClientHttpExternal(String urlBase, JsonMapper jsonMapper) {
        this.restClient = RestClient.builder().baseUrl(urlBase).build();
        this.jsonMapper = jsonMapper;
    }

    public JsonNode get(String ruta) {
        String cuerpo = restClient.get().uri(ruta).retrieve().body(String.class);
        return jsonMapper.readTree(cuerpo);
    }

}
