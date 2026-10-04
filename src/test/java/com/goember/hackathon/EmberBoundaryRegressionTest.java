package com.goember.hackathon;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import com.goember.hackathon.ember.EmberClient;
import com.goember.hackathon.ember.EmberApiException;

class EmberBoundaryRegressionTest {
    EmberClient client(int status, String body) {
        return new EmberClient(WebClient.builder().baseUrl("https://fixture.invalid")
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.valueOf(status))
                        .header("Content-Type", "application/json").body(body).build())).build(), Duration.ofSeconds(1));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 429, 500, 503})
    void upstreamHttpErrorsNeverExposeRawResponses(int status) {
        var exception = assertThrows(EmberApiException.class,
                () -> client(status, "private upstream diagnostics").searchLocations("Origin", 50, "STOP_POINT"));
        assertFalse(exception.getMessage().contains("private"));
        assertFalse(exception.isTimeout());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-json", "{}", "[null]", "[{\"id\":1}]", "[{\"id\":\"one\",\"name\":\"Stop\"}]"})
    void malformedAndUnexpectedLocationResponsesFailDeliberately(String body) {
        assertThrows(EmberApiException.class, () -> client(200, body).searchLocations("Origin", 50, "STOP_POINT"));
    }

    @Test
    void emptyLocationListsAreValidButMismatchedIdsAreNotReturned() {
        assertTrue(client(200, "[]").findLocationById(10).isEmpty());
        assertTrue(client(200, "[{\"id\":20,\"name\":\"Other\"}]").findLocationById(10).isEmpty());
        assertEquals("Origin", client(200, "[{\"id\":10,\"name\":\"Origin\"}]")
                .findLocationById(10).orElseThrow().get("name"));
    }

    @ParameterizedTest
    @ValueSource(longs = {66, 1456})
    void locationIdLookupIncludesStopPointsAndKeepsTheExactId(long locationId) {
        var observed = new AtomicReference<URI>();
        var client = new EmberClient(WebClient.builder().baseUrl("https://fixture.invalid")
                .exchangeFunction(request -> {
                    observed.set(request.url());
                    String body = request.url().getQuery().contains("type=all")
                            ? "[{\"id\":" + locationId + ",\"type\":\"STOP_POINT\",\"name\":\"Real Ember stop\"}]"
                            : "[]";
                    return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json")
                            .body(body).build());
                }).build(), Duration.ofSeconds(1));
        var stop = client.findLocationById(locationId).orElseThrow();
        assertEquals(locationId, ((Number) stop.get("id")).longValue());
        assertEquals("STOP_POINT", stop.get("type"));
        assertEquals("Real Ember stop", stop.get("name"));
        assertEquals("ids=" + locationId + "&type=all", observed.get().getQuery());
    }

    @Test
    void batchLocationLookupRequestsAllStopPointIdsOnce() {
        var observed = new AtomicReference<URI>();
        var requests = new java.util.concurrent.atomic.AtomicInteger();
        var client = new EmberClient(WebClient.builder().baseUrl("https://fixture.invalid")
                .exchangeFunction(request -> {
                    requests.incrementAndGet();
                    observed.set(request.url());
                    return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json")
                            .body("[{\"id\":66,\"name\":\"First stop\",\"type\":\"STOP_POINT\"},"
                                    + "{\"id\":1456,\"name\":\"Second stop\",\"type\":\"STOP_POINT\"},"
                                    + "{\"id\":999,\"name\":\"Unrequested\"}]").build());
                }).build(), Duration.ofSeconds(1));
        var locations = client.findLocationsByIds(List.of(66L, 1456L));
        assertEquals(1, requests.get());
        assertEquals("First stop", locations.get(66L).get("name"));
        assertEquals("Second stop", locations.get(1456L).get("name"));
        assertEquals(2, locations.size());
        assertTrue(observed.get().getQuery().contains("type=all"));
        assertTrue(observed.get().getQuery().contains("ids=66"));
        assertTrue(observed.get().getQuery().contains("ids=1456"));
    }

    @Test
    void stalledResponsesHaveABoundedRequestBudget() {
        var client = new EmberClient(WebClient.builder().baseUrl("https://fixture.invalid")
                .exchangeFunction(request -> Mono.never()).build(), Duration.ofMillis(30));
        var exception = assertThrows(EmberApiException.class, () -> client.getTrip(1L));
        assertTrue(exception.isTimeout());
    }

    @Test
    void networkFailuresAreTranslated() {
        var client = new EmberClient(WebClient.builder().baseUrl("https://fixture.invalid")
                .exchangeFunction(request -> Mono.error(new WebClientRequestException(new java.io.IOException("connection lost"),
                        HttpMethod.GET, URI.create("https://fixture.invalid"), new HttpHeaders()))).build(), Duration.ofSeconds(1));
        assertThrows(EmberApiException.class, client::getLiveVehicles);
    }

    @Test
    void malformedProtobufIsRejected() {
        assertThrows(EmberApiException.class, () -> client(200, "invalid-protobuf").getLiveVehicles());
    }

    @Test
    void locationQueryIsEncodedAsDataAndUsesConfiguredBaseUrl() {
        var observed = new AtomicReference<URI>();
        var client = new EmberClient(WebClient.builder().baseUrl("https://fixture.invalid")
                .exchangeFunction(request -> {
                    observed.set(request.url());
                    return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json").body("[]").build());
                }).build(), Duration.ofSeconds(1));
        client.searchLocations("A&B / town", 25, "STOP_POINT");
        assertEquals("fixture.invalid", observed.get().getHost());
        assertTrue(observed.get().getRawQuery().contains("query=A%26B"));
        assertTrue(observed.get().getRawQuery().contains("limit=25"));
    }
}
