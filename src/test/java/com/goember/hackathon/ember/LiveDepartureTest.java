package com.goember.hackathon.ember;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import com.goember.hackathon.ember.proto.*;

class LiveDepartureTest {
    static final Instant NOW = Instant.parse("2026-10-04T12:00:00Z");
    final EmberClient client = mock(EmberClient.class);
    final EmberService service = new EmberService(client);

    static Map<String, Object> stop(long eventId, long locationId, String name, int minutes, boolean actual) {
        return Map.of("id", eventId, "location", Map.of("id", locationId, "name", name, "region_name", name),
                "allow_boarding", true, "allow_drop_off", true, "skipped", false,
                "departure", actual ? Map.of("scheduled", NOW.plusSeconds(minutes * 60).toString(), "actual", NOW.minusSeconds(60).toString())
                        : Map.of("scheduled", NOW.plusSeconds(minutes * 60).toString()));
    }

    static MinimalVehicleTrip trip(String uid, long current) {
        var builder = MinimalVehicleTrip.newBuilder().setUid(uid).setRouteNumber("E1");
        if (current != 0) builder.setCurrentStop(MinimalLocationTime.newBuilder().setId((int) current));
        builder.setNextStop(MinimalLocationTime.newBuilder().setId(102));
        return builder.build();
    }

    static LiveVehicleData vehicle(int id, String uid, long current) {
        return LiveVehicleData.newBuilder().setId(id)
                .setTrips(MinimalVehicleTrips.newBuilder().setActive(trip(uid, current))).build();
    }

    void feed(List<LiveVehicleData> vehicles, Map<String, Map<String, Object>> details) {
        var enriched = vehicles.stream().map(vehicle -> {
            var trips = vehicle.getTrips().toBuilder();
            if (trips.hasActive()) trips.setActive(withRoute(trips.getActive(), details.get(trips.getActive().getUid())));
            if (trips.hasNext()) trips.setNext(withRoute(trips.getNext(), details.get(trips.getNext().getUid())));
            return vehicle.toBuilder().setTrips(trips).build();
        }).toList();
        when(client.getLiveVehicles()).thenReturn(LiveVehicleList.newBuilder().addAllVehicles(enriched).build());
        when(client.getLiveTripDetails(anyList())).thenReturn(details);
    }

    static MinimalVehicleTrip withRoute(MinimalVehicleTrip trip, Map<String, Object> detail) {
        var builder = trip.toBuilder();
        for (var value : (List<?>) detail.get("route")) {
            var row = (Map<?, ?>) value;
            var location = (Map<?, ?>) row.get("location");
            var departure = (Map<?, ?>) row.get("departure");
            var times = MinimalEventTimes.newBuilder();
            if (departure.get("scheduled") instanceof String text && !text.equals("bad-time")) times.setScheduled(time(text));
            if (departure.get("estimated") instanceof String text) times.setEstimated(time(text));
            if (departure.get("actual") instanceof String text) times.setActual(time(text));
            var stop = MinimalLocationTime.newBuilder().setId(((Number) row.get("id")).intValue())
                    .setLocation(MinimalLocation.newBuilder().setName((String) location.get("name")))
                    .setAllowBoarding(Boolean.TRUE.equals(row.get("allow_boarding")) && !Boolean.TRUE.equals(row.get("skipped")))
                    .setAllowDropOff(Boolean.TRUE.equals(row.get("allow_drop_off"))).setDeparture(times).build();
            builder.addRoute(stop);
            if (trip.hasCurrentStop() && trip.getCurrentStop().getId() == stop.getId()) builder.setCurrentStop(stop);
            if (trip.hasNextStop() && trip.getNextStop().getId() == stop.getId()) builder.setNextStop(stop);
        }
        return builder.build();
    }

    static ProtoTimestamp time(String text) {
        var instant = Instant.parse(text);
        return ProtoTimestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
    }

    @Test
    void blankSearchReturnsSoonestDeparturesAndCorrectLocationIds() {
        feed(List.of(vehicle(1, "later", 0), vehicle(2, "soon", 0)), Map.of(
                "later", Map.of("route", List.of(stop(101, 10, "Origin", 20, false), stop(102, 20, "Destination", 40, false))),
                "soon", Map.of("route", List.of(stop(101, 10, "Origin", 2, false), stop(102, 20, "Destination", 20, false)))));
        var buses = service.getLiveBuses(null, "  ", NOW);
        assertEquals(List.of(2L, 1L), buses.stream().map(EmberService.LiveBus::vehicleId).toList());
        assertNull(buses.getFirst().departureStop().locationId());
        assertFalse(buses.getFirst().routeDetailsLoaded());
        assertEquals(NOW.plusSeconds(120), buses.getFirst().departureTime());
        verify(client, never()).getLiveTripDetails(anyList());
        var loaded = service.getLiveBuses(null, "", "", "soon", NOW);
        assertEquals(10L, loaded.getFirst().departureStop().locationId());
    }

    @Test
    void estimatedDepartureOverridesScheduleAndOverdueCurrentStopLeavesNow() {
        var estimated = new java.util.HashMap<>(stop(101, 10, "Origin", -10, false));
        estimated.put("departure", Map.of("scheduled", NOW.minusSeconds(600).toString(), "estimated", NOW.plusSeconds(300).toString()));
        feed(List.of(vehicle(1, "estimated", 0), vehicle(2, "now", 101)), Map.of(
                "estimated", Map.of("route", List.of(estimated, stop(102, 20, "Destination", 20, false))),
                "now", Map.of("route", List.of(stop(101, 10, "Origin", -2, false), stop(102, 20, "Destination", 20, false)))));
        var buses = service.getLiveBuses("", "", NOW);
        assertEquals(NOW, buses.getFirst().departureTime());
        assertEquals(NOW.plusSeconds(300), buses.getLast().departureTime());
        var loaded = service.getLiveBuses("", "", "", "now", NOW).getFirst();
        assertEquals(10L, loaded.currentStop().locationId());
        assertEquals(20L, loaded.nextStop().locationId());
        assertEquals(101L, loaded.currentStop().eventId());
    }

    @Test
    void departedAndPastDeparturesAreExcludedButNextTripsAreIncluded() {
        var vehicle = LiveVehicleData.newBuilder().setId(1).setTrips(MinimalVehicleTrips.newBuilder()
                .setActive(trip("departed", 101)).setNext(trip("next", 0))).build();
        feed(List.of(vehicle, vehicle(2, "past", 0)), Map.of(
                "departed", Map.of("route", List.of(stop(101, 10, "Origin", -2, true), stop(102, 20, "Destination", 20, false))),
                "past", Map.of("route", List.of(stop(101, 10, "Origin", -2, false), stop(102, 20, "Destination", 20, false))),
                "next", Map.of("route", List.of(stop(101, 10, "Origin", 5, false), stop(102, 20, "Destination", 20, false)))));
        var buses = service.getLiveBuses("", "", NOW);
        assertEquals(1, buses.size());
        assertEquals("next", buses.getFirst().tripUid());
        assertTrue(buses.getFirst().upcomingTrip());
    }

    @Test
    void partialSearchFiltersAnIntermediateOriginAndOrderedDestination() {
        feed(List.of(vehicle(1, "trip", 0)), Map.of("trip", Map.of("route", List.of(
                stop(101, 10, "Origin", 1, false), stop(102, 20, "Middle", 5, false), stop(103, 30, "Destination", 20, false)))));
        assertEquals("Middle", service.getLiveBuses(" MIDDLE ", "", NOW).getFirst().departureStop().name());
        assertEquals("Origin", service.getLiveBuses("", "destination", NOW).getFirst().departureStop().name());
        assertTrue(service.getLiveBuses("Destination", "Origin", NOW).isEmpty());
        assertTrue(service.getLiveBuses("", "Unknown", NOW).isEmpty());
    }

    @Test
    void skippedAndNonBoardingStopsAreNotDepartures() {
        var skipped = new java.util.HashMap<>(stop(101, 10, "Origin", 1, false));
        skipped.put("skipped", true);
        var terminal = new java.util.HashMap<>(stop(103, 30, "Destination", 20, false));
        terminal.put("allow_boarding", false);
        feed(List.of(vehicle(1, "trip", 0)), Map.of("trip", Map.of("route", List.of(
                skipped, stop(102, 20, "Middle", 5, false), terminal))));
        var buses = service.getLiveBuses("", "", "", "trip", NOW);
        assertEquals(20L, buses.getFirst().departureStop().locationId());
        assertEquals(List.of(20L, 30L), buses.getFirst().route().stream().map(EmberService.BusStop::locationId).toList());
        assertTrue(service.getLiveBuses("Origin", "", NOW).isEmpty());
    }

    @Test
    void trackedRideRemainsVisibleAfterItsLastBoardingDeparture() {
        feed(List.of(vehicle(1, "trip", 101)), Map.of("trip", Map.of("route", List.of(
                stop(101, 10, "Origin", -2, true), stop(102, 20, "Destination", 20, false)))));
        assertTrue(service.getLiveBuses("", "", NOW).isEmpty());
        assertEquals(1, service.getLiveBuses("", "", "trip", NOW).size());
        assertEquals(1, service.getLiveBuses("Origin", "Destination", NOW).size());
    }

    @Test
    void missingOrMalformedTimesNeverInventADeparture() {
        var row = new java.util.HashMap<>(stop(101, 10, "Origin", 1, false));
        row.put("departure", Map.of());
        feed(List.of(vehicle(1, "trip", 0)), Map.of("trip", Map.of("route", List.of(row, stop(102, 20, "Destination", 20, false)))));
        assertTrue(service.getLiveBuses("", "", NOW).isEmpty());
        row.put("departure", Map.of("scheduled", "bad-time"));
        assertThrows(EmberApiException.class, () -> service.getLiveBuses("", "", "", "trip", NOW));
    }

    @Test
    void emptyLiveFeedDoesNotMakeTripRequests() {
        when(client.getLiveVehicles()).thenReturn(LiveVehicleList.getDefaultInstance());
        assertTrue(service.getLiveBuses("", "", NOW).isEmpty());
        verify(client, never()).getLiveTripDetails(anyList());
    }

    @Test
    void binaryLiveFeedAndJsonTripDetailsWorkTogetherWithoutLiveNetwork() {
        var data = Map.<String, Object>of("route", List.of(stop(101, 10, "Origin", 1, false), stop(102, 20, "Destination", 20, false)));
        var vehicle = vehicle(1, "trip", 101).toBuilder().setTrips(MinimalVehicleTrips.newBuilder().setActive(withRoute(trip("trip", 101), data))).build();
        var feed = LiveVehicleList.newBuilder().addVehicles(vehicle).build().toByteArray();
        String detail = new ObjectMapper().writeValueAsString(data);
        var web = WebClient.builder().baseUrl("https://fixture.invalid").exchangeFunction(request -> {
            if (request.url().getPath().equals("/v1/vehicles/live/")) {
                return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/x-protobuf")
                        .body(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(feed))).build());
            }
            assertEquals("/v1/trips/trip/", request.url().getPath());
            assertTrue(request.headers().getAccept().stream().anyMatch(type -> type.toString().equals("application/json")));
            return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json").body(detail).build());
        }).build();
        var result = new EmberService(new EmberClient(web, Duration.ofSeconds(1))).getLiveBuses("", "", "", "trip", NOW);
        assertEquals(10L, result.getFirst().currentStop().locationId());
        assertEquals(2, result.getFirst().route().size());
        assertEquals(NOW.plusSeconds(60), result.getFirst().departureTime());
    }
}
