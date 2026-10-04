package com.goember.hackathon.ember;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import com.google.protobuf.InvalidProtocolBufferException;
import com.goember.hackathon.ember.proto.LiveVehicleList;

@Component
public class EmberClient {
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LOCATIONS =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, Object>> TRIP =
            new ParameterizedTypeReference<>() {};
    private final WebClient webClient;
    private final Duration requestTimeout;

    public EmberClient(@Qualifier("emberWebClient") WebClient webClient,
            @Value("${ember.api.request-timeout:10s}") Duration requestTimeout) {
        this.webClient = webClient;
        this.requestTimeout = requestTimeout;
    }

    public List<Map<String, Object>> searchLocations(String query, int limit, String type) {
        List<Map<String, Object>> locations = await(webClient.get().uri(builder -> {
            builder.path("/v1/locations/search/").queryParam("limit", limit).queryParam("type", type);
            if (query != null && !query.isBlank()) builder.queryParam("query", query);
            return builder.build();
        }).retrieve().bodyToMono(LOCATIONS));
        return validateLocations(locations);
    }

    public Map<String, Object> getTrip(Long tripId) {
        Map<String, Object> trip = await(webClient.get().uri(builder -> builder
                .path("/v1/trips/{tripId}/").queryParam("route", true)
                .queryParam("description", true).build(tripId)).retrieve().bodyToMono(TRIP));
        if (trip == null || trip.isEmpty()) throw invalidResponse(null);
        return trip;
    }

    public Optional<Map<String, Object>> findLocationById(long locationId) {
        List<Map<String, Object>> locations = await(webClient.get().uri(builder -> builder
                .path("/v1/locations/search/").queryParam("ids", locationId)
                .queryParam("type", "all").build())
                .retrieve().bodyToMono(LOCATIONS));
        return validateLocations(locations).stream()
                .filter(location -> location.get("id") instanceof Number id && id.longValue() == locationId)
                .findFirst();
    }

    public Map<Long, Map<String, Object>> findLocationsByIds(List<Long> locationIds) {
        if (locationIds.isEmpty()) return Map.of();
        if (locationIds.size() > 50 || locationIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Provide between 1 and 50 positive Ember location IDs");
        }
        List<Map<String, Object>> locations = await(webClient.get().uri(builder -> builder
                .path("/v1/locations/search/").queryParam("type", "all")
                .queryParam("ids", locationIds.toArray()).build()).retrieve().bodyToMono(LOCATIONS));
        var result = new java.util.HashMap<Long, Map<String, Object>>();
        for (var location : validateLocations(locations)) {
            long id = ((Number) location.get("id")).longValue();
            if (locationIds.contains(id) && result.putIfAbsent(id, location) != null) throw invalidResponse(null);
        }
        return result;
    }

    public LiveVehicleList getLiveVehicles() {
        byte[] payload = await(webClient.get().uri("/v1/vehicles/live/")
                .accept(MediaType.valueOf("application/x-protobuf"))
                .retrieve().bodyToMono(byte[].class).defaultIfEmpty(new byte[0]));
        try {
            return LiveVehicleList.parseFrom(payload);
        } catch (InvalidProtocolBufferException exception) {
            throw invalidResponse(exception);
        }
    }

    public Map<String, Map<String, Object>> getLiveTripDetails(List<String> tripUids) {
        // Bound parallel requests and the total budget for this live-feed refresh.
        return await(Flux.fromIterable(tripUids.stream().distinct().toList())
                .flatMap(uid -> webClient.get().uri(builder -> builder
                        .path("/v1/trips/{uid}/").queryParam("route", true)
                        .queryParam("description", true).build(uid))
                        .accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(TRIP)
                        .switchIfEmpty(Mono.error(invalidResponse(null)))
                        .map(body -> Map.entry(uid, body)), 4)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<Map<String, Object>> validateLocations(List<Map<String, Object>> locations) {
        if (locations == null || locations.stream().anyMatch(location -> location == null
                || !(location.get("id") instanceof Number)
                || !(location.get("name") instanceof String name) || name.isBlank())) {
            throw invalidResponse(null);
        }
        return locations;
    }

    private <T> T await(Mono<T> response) {
        return response.timeout(requestTimeout)
                .onErrorMap(TimeoutException.class, exception -> new EmberApiException(
                        "Ember did not respond in time. Please try again later.", exception, true))
                .onErrorMap(WebClientException.class, exception -> new EmberApiException(
                        "Ember is currently unavailable. Please try again later.", exception, false))
                .onErrorMap(DecodingException.class, this::invalidResponse)
                .onErrorMap(DataBufferLimitException.class, this::invalidResponse)
                .block();
    }

    private EmberApiException invalidResponse(Throwable cause) {
        return new EmberApiException("Ember returned an invalid response. Please try again later.", cause, false);
    }
}
