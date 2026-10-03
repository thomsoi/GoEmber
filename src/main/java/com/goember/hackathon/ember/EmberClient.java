package com.goember.hackathon.ember;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class EmberClient {

    private final WebClient webClient;

    public EmberClient(@Qualifier("emberWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public List<Map<String, Object>> searchLocations(
            String query,
            int limit,
            String type
    ) {
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
            .map(payload -> (Map<String, Object>) payload)
            .collectList()
            .block();
    }

    public Map<String, Object> getTrip(Long tripId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/trips/{tripId}/")
                        .queryParam("route", true)
                        .queryParam("description", true)
                        .build(tripId))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    public Optional<Map<String, Object>> findLocationById(long locationId) {
        List<Map<String, Object>> locations = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/locations/search/")
                        .queryParam("ids", locationId)
                        .build())
                .retrieve()
                .bodyToFlux(Map.class)
                .map(payload -> (Map<String, Object>) payload)
                .collectList()
                .block();

        if (locations == null || locations.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> firstLocation = locations.get(0);
        if (firstLocation == null || firstLocation.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(firstLocation);
    }
}
