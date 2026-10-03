package com.goember.hackathon.ember;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class EmberClient {

    private final WebClient webClient;

    public EmberClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<Map> searchLocations(
        String query,
        int limit,
        String type
        )
        {
        return webClient.get()
            .uri(uriBuilder -> {
                uriBuilder
                    .path("/v1/locations/search/")
                    .queryParam("limit", limit)
                    .queryParam("type", type);

            if (query != null && !query.isBlank()) {
                uriBuilder.queryParam("query", query);
            }

            return uriBuilder.build();
        })
        .retrieve()
        .bodyToFlux(Map.class)
        .collectList()
        .block();
    }
}
