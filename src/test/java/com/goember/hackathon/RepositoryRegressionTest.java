package com.goember.hackathon;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.goember.hackathon.ember.EmberClient;
import com.goember.hackathon.ember.EmberApiException;
import com.goember.hackathon.journey.JourneyService;
import com.goember.hackathon.passport.PassportService;
import com.goember.hackathon.user.UserService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:regression;DB_CLOSE_DELAY=-1",
        "spring.jpa.open-in-view=false", "spring.jpa.show-sql=false" })
class RepositoryRegressionTest {
    @LocalServerPort int port;
    @MockitoBean EmberClient ember;
    @Autowired UserService users;
    @Autowired PassportService passports;
    @Autowired JourneyService journeys;
    final ObjectMapper json = new ObjectMapper();
    final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void fixtures() {
        when(ember.findLocationById(anyLong())).thenAnswer(invocation -> {
            long id = invocation.getArgument(0);
            if (id != 10 && id != 20) return Optional.empty();
            return Optional.of(Map.of("id", id, "name", id == 10 ? "Origin" : "Destination",
                    "region_name", id == 10 ? "Origin" : "Destination", "lat", 56.0, "lon", id == 10 ? -3.0 : -2.0));
        });
        when(ember.searchLocations(anyString(), anyInt(), anyString())).thenReturn(List.of());
    }

    @Test
    void soonestDeparturesAcceptOmittedAndBlankSearchParameters() throws Exception {
        var now = java.time.Instant.now();
        var departure = com.goember.hackathon.ember.proto.MinimalLocationTime.newBuilder().setId(1001)
                .setLocation(com.goember.hackathon.ember.proto.MinimalLocation.newBuilder().setName("Origin"))
                .setAllowBoarding(true)
                .setDeparture(com.goember.hackathon.ember.proto.MinimalEventTimes.newBuilder()
                        .setScheduled(com.goember.hackathon.ember.proto.ProtoTimestamp.newBuilder().setSeconds(now.plusSeconds(300).getEpochSecond())));
        var arrival = com.goember.hackathon.ember.proto.MinimalLocationTime.newBuilder().setId(1002)
                .setLocation(com.goember.hackathon.ember.proto.MinimalLocation.newBuilder().setName("Destination"))
                .setAllowDropOff(true)
                .setDeparture(com.goember.hackathon.ember.proto.MinimalEventTimes.newBuilder()
                        .setScheduled(com.goember.hackathon.ember.proto.ProtoTimestamp.newBuilder().setSeconds(now.plusSeconds(900).getEpochSecond())));
        var trip = com.goember.hackathon.ember.proto.MinimalVehicleTrip.newBuilder().setUid("soonest").addRoute(departure).addRoute(arrival);
        when(ember.getLiveVehicles()).thenReturn(com.goember.hackathon.ember.proto.LiveVehicleList.newBuilder()
                .addVehicles(com.goember.hackathon.ember.proto.LiveVehicleData.newBuilder().setId(8)
                        .setTrips(com.goember.hackathon.ember.proto.MinimalVehicleTrips.newBuilder().setNext(trip))).build());
        for (String path : List.of("/api/vehicles/live", "/api/vehicles/live?", "/api/vehicles/live?origin=&destination=",
                "/api/vehicles/live?origin=Origin", "/api/vehicles/live?destination=Destination")) {
            var result = call("GET", path, null, null);
            assertEquals(200, result.statusCode(), path + ": " + result.body());
            assertTrue(result.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
            var buses = json.readTree(result.body());
            assertEquals(1, buses.size(), path);
            assertEquals("soonest", buses.get(0).get("tripUid").asString());
            assertEquals("Origin", buses.get(0).get("departureStop").get("name").asString());
        }
        verify(ember, never()).getLiveTripDetails(anyList());
    }

    @Test
    void blankLiveSearchReturnsJsonAndLocationIdsWithoutRequiredParameters() throws Exception {
        var trip = com.goember.hackathon.ember.proto.MinimalVehicleTrip.newBuilder().setUid("upcoming-test").setRouteNumber("E1");
        var vehicle = com.goember.hackathon.ember.proto.LiveVehicleData.newBuilder().setId(8)
                .setTrips(com.goember.hackathon.ember.proto.MinimalVehicleTrips.newBuilder().setNext(trip));
        when(ember.getLiveVehicles()).thenReturn(com.goember.hackathon.ember.proto.LiveVehicleList.newBuilder().addVehicles(vehicle).build());
        var now = java.time.Instant.now();
        when(ember.getLiveTripDetails(anyList())).thenReturn(Map.of("upcoming-test", Map.of("route", List.of(
                Map.of("id", 1001, "location", Map.of("id", 10, "name", "Origin"), "allow_boarding", true,
                        "allow_drop_off", false, "departure", Map.of("scheduled", now.plusSeconds(300).toString())),
                Map.of("id", 1002, "location", Map.of("id", 20, "name", "Destination"), "allow_boarding", false,
                        "allow_drop_off", true, "departure", Map.of("scheduled", now.plusSeconds(900).toString()))))));
        var result = call("GET", "/api/vehicles/live?detailsTripUid=upcoming-test", null, null);
        assertEquals(200, result.statusCode(), result.body());
        assertTrue(result.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        var bus = json.readTree(result.body()).get(0);
        assertEquals(10, bus.get("departureStop").get("locationId").asInt());
        assertEquals("upcoming-test", bus.get("tripUid").asString());
        assertTrue(bus.get("upcomingTrip").asBoolean());
        assertEquals(now.plusSeconds(300), java.time.Instant.parse(bus.get("departureTime").asString()));
        assertEquals(200, call("GET", "/api/vehicles/live?origin=Origin", null, null).statusCode());
    }

    @Test
    void privateGuestPassportRequiresItsOwnCredential() throws Exception {
        var guest = guest();
        String path = "/api/passports/" + guest.get("id").asLong();
        assertEquals(401, call("GET", path, null, null).statusCode());
        assertEquals(403, call("GET", path, "wrong-credential", null).statusCode());
        assertEquals(200, call("GET", path, guest.get("accessToken").asString(), null).statusCode());
        assertEquals(403, call("GET", "/api/users", guest.get("accessToken").asString(), null).statusCode());
        var user = call("GET", "/api/users/" + guest.get("id").asLong(), guest.get("accessToken").asString(), null);
        assertEquals(200, user.statusCode());
        assertFalse(user.body().contains("accessToken"));
        assertFalse(user.body().contains("credentialHash"));
        var other = guest();
        assertEquals(403, call("GET", "/api/passports/" + other.get("id").asLong(),
                guest.get("accessToken").asString(), null).statusCode());
    }

    @Test
    void stampNamesAreCanonicalAndRetriesDoNotIncreaseVisits() throws Exception {
        var guest = guest();
        String path = "/api/passports/" + guest.get("id").asLong() + "/locations/10/visits";
        String token = guest.get("accessToken").asString();
        String body = "{\"locationName\":\"Invented name\",\"operationId\":\"visit-1\"}";
        var first = call("POST", path, token, body);
        assertEquals(200, first.statusCode(), first.body());
        assertEquals("Origin", json.readTree(first.body()).get("stampName").asString());
        assertEquals(1, json.readTree(call("POST", path, token, body).body()).get("visitCount").asInt());
        assertEquals(1, json.readTree(call("GET", "/api/passports/" + guest.get("id").asLong(), token, null).body())
                .get("townsVisited").asInt());
        assertEquals(2, json.readTree(call("POST", path, token,
                "{\"operationId\":\"visit-2\"}").body()).get("visitCount").asInt());
        assertEquals(409, call("POST", path.replace("/10/", "/20/"), token, body).statusCode());
    }

    @Test
    void inventedLocationsAndMissingOperationIdsAreRejected() throws Exception {
        var guest = guest();
        String path = "/api/passports/" + guest.get("id").asLong() + "/locations/99999/visits";
        String token = guest.get("accessToken").asString();
        assertEquals(404, call("POST", path, token,
                "{\"locationName\":\"Fake\",\"operationId\":\"unknown\"}").statusCode());
        assertEquals(400, call("POST", path, token, "{\"locationName\":\"Fake\"}").statusCode());
    }

    @Test
    void townAwardRetriesAndUnknownTownsAreHandled() throws Exception {
        when(ember.searchLocations(eq("Origin"), anyInt(), anyString()))
                .thenReturn(List.of(Map.of("id", 10L, "name", "Origin", "region_name", "Origin")));
        var guest = guest();
        String path = "/api/passports/" + guest.get("id").asLong() + "/towns/visits";
        String token = guest.get("accessToken").asString();
        String body = "{\"townName\":\"Origin\",\"operationId\":\"town-1\"}";
        assertEquals(200, call("POST", path, token, body).statusCode());
        assertEquals(1, json.readTree(call("POST", path, token, body).body()).get("visitCount").asInt());
        assertEquals(1, json.readTree(call("GET", "/api/passports/" + guest.get("id").asLong(), token, null).body())
                .get("townsVisited").asInt());
        assertEquals(404, call("POST", path, token,
                "{\"townName\":\"Invented town\",\"operationId\":\"town-2\"}").statusCode());
    }

    @Test
    void unknownDistancePersistsInH2AndRemainsUnavailableInResponse() throws Exception {
        var guest = guest();
        String path = "/api/passports/" + guest.get("id").asLong();
        String body = completion("missing-distance", 100, 200);
        var saved = call("POST", path + "/journeys", guest.get("accessToken").asString(), body);
        assertEquals(200, saved.statusCode(), saved.body());
        assertEquals(200, call("POST", path + "/journeys", guest.get("accessToken").asString(), body).statusCode());
        var response = call("GET", path, guest.get("accessToken").asString(), null);
        assertEquals(200, response.statusCode(), response.body());
        var passport = json.readTree(response.body());
        assertTrue(passport.get("totalDistanceTravelled").isNull());
        assertEquals(1, passport.get("busNumbersRidden").asInt());
        assertEquals(0, passport.get("townsVisited").asInt());
    }

    @Test
    void knownDistanceAndDuplicateCompletionArePersistedOnce() throws Exception {
        var guest = guest();
        String path = "/api/passports/" + guest.get("id").asLong();
        String body = completion("known-distance", 10, 20);
        assertEquals(200, call("POST", path + "/journeys", guest.get("accessToken").asString(), body).statusCode());
        assertEquals(200, call("POST", path + "/journeys", guest.get("accessToken").asString(), body).statusCode());
        var passport = json.readTree(call("GET", path, guest.get("accessToken").asString(), null).body());
        assertTrue(passport.get("totalDistanceTravelled").asDouble() > 60);
        assertEquals(1, passport.get("busNumbersRidden").asInt());
    }

    @Test
    void coordinateLookupFailureLeavesDistanceUnknownButKeepsTheBusNumber() throws Exception {
        when(ember.findLocationById(10201L)).thenThrow(
                new EmberApiException("Ember is currently unavailable", null, false));
        var guest = guest();
        String path = "/api/passports/" + guest.get("id").asLong();
        String token = guest.get("accessToken").asString();
        assertEquals(200, call("POST", path + "/journeys", token,
                completion("route-without-distance", "E31", 10201, 10202)).statusCode());
        var passport = json.readTree(call("GET", path, token, null).body());
        assertEquals(1, passport.get("busNumbersRidden").asInt());
        assertTrue(passport.get("totalDistanceTravelled").isNull());
    }

    @Test
    void oneStopRideCountsItsTownAndBusNumberWithZeroDistance() throws Exception {
        when(ember.findLocationById(10301L)).thenReturn(Optional.of(Map.of(
                "id", 10301L, "name", "Boarding stop", "region_name", "Glasgow",
                "lat", 55.865, "lon", -4.252)));
        var guest = guest();
        String path = "/api/passports/" + guest.get("id").asLong();
        String token = guest.get("accessToken").asString();
        assertEquals(200, call("POST", path + "/locations/10301/visits", token,
                "{\"operationId\":\"single-stop\"}").statusCode());
        assertEquals(200, call("POST", path + "/journeys", token,
                completion("single-stop-ride", "E31", 10301, 10301)).statusCode());
        var passport = json.readTree(call("GET", path, token, null).body());
        assertEquals(1, passport.get("townsVisited").asInt());
        assertEquals(1, passport.get("busNumbersRidden").asInt());
        assertEquals(0.0, passport.get("totalDistanceTravelled").asDouble(), 0.0001);
        verify(ember, times(1)).findLocationById(10301L);
    }

    @Test
    void stopStampsCountDistinctTownsAndCompletedRidesCountDistinctBusNumbers() throws Exception {
        when(ember.findLocationsByIds(anyList())).thenReturn(Map.of(
                10101L, Map.of("id", 10101L, "name", "First stop", "region_name", "Edinburgh",
                        "lat", 56.0, "lon", -3.0),
                10102L, Map.of("id", 10102L, "name", "Second stop", "region_name", "Edinburgh",
                        "lat", 56.0, "lon", -2.5),
                10103L, Map.of("id", 10103L, "name", "Third stop", "region_name", "Dundee",
                        "lat", 56.0, "lon", -2.0)));
        var guest = guest();
        String path = "/api/passports/" + guest.get("id").asLong();
        String token = guest.get("accessToken").asString();
        String visits = "{\"visits\":[{\"locationId\":10101,\"operationId\":\"stat:0\"},"
                + "{\"locationId\":10102,\"operationId\":\"stat:1\"},"
                + "{\"locationId\":10103,\"operationId\":\"stat:2\"}]}";
        assertEquals(200, call("POST", path + "/locations/visits", token, visits).statusCode());
        var stamped = json.readTree(call("GET", path, token, null).body());
        assertEquals(2, stamped.get("townsVisited").asInt());
        assertEquals(0, stamped.get("busNumbersRidden").asInt());
        assertEquals(0, stamped.get("totalDistanceTravelled").asDouble());

        for (String key : List.of("ride-1", "ride-2")) {
            assertEquals(200, call("POST", path + "/journeys", token,
                    completion(key, key.equals("ride-1") ? "E31" : "e31", 10101, 10103)).statusCode());
        }
        assertEquals(200, call("POST", path + "/journeys", token,
                completion("ride-3", "E10", 10103, 10103)).statusCode());
        assertEquals(200, call("POST", path + "/journeys", token,
                completion("ride-1", "E31", 10101, 10103)).statusCode());
        var passport = json.readTree(call("GET", path, token, null).body());
        assertEquals(2, passport.get("townsVisited").asInt());
        assertEquals(2, passport.get("busNumbersRidden").asInt());
        assertTrue(passport.get("totalDistanceTravelled").asDouble() > 120);
        assertTrue(passport.get("totalDistanceTravelled").asDouble() < 130);
        verify(ember, never()).findLocationById(10101L);
        verify(ember, never()).findLocationById(10103L);
    }

    @Test
    void concurrentAwardRequestsKeepEveryVisitAndDeduplicateOneOperation() throws Exception {
        var user = users.createUser("Concurrent awards");
        try (var executor = Executors.newFixedThreadPool(4)) {
            var jobs = java.util.stream.IntStream.range(0, 8).<Callable<Void>>mapToObj(index -> () -> {
                passports.recordLocationVisit(user.getId(), 10, null, "concurrent-" + index);
                return null;
            }).toList();
            for (var result : executor.invokeAll(jobs)) result.get();
            var retries = java.util.stream.IntStream.range(0, 4).<Callable<Void>>mapToObj(index -> () -> {
                passports.recordLocationVisit(user.getId(), 10, null, "shared-retry");
                return null;
            }).toList();
            for (var result : executor.invokeAll(retries)) result.get();
        }
        var passport = passports.getOrCreatePassport(user.getId());
        assertEquals(1, passport.getStamps().size());
        assertEquals(9, passport.getStamps().get(0).getVisitCount());
    }

    @Test
    void batchAwardsEveryStopEventAndRetryDoesNotAwardAgain() throws Exception {
        when(ember.findLocationsByIds(anyList())).thenReturn(Map.of(
                10010L, Map.of("id", 10010L, "name", "Boarding stop"),
                10020L, Map.of("id", 10020L, "name", "Next stop")));
        var guest = guest();
        String path = "/api/passports/" + guest.get("id").asLong();
        String token = guest.get("accessToken").asString();
        String body = "{\"visits\":[{\"locationId\":10010,\"operationId\":\"ride-a:stop:0\"},"
                + "{\"locationId\":10020,\"operationId\":\"ride-a:stop:1\"},"
                + "{\"locationId\":10010,\"operationId\":\"ride-a:stop:2\"}]}";
        assertEquals(200, call("POST", path + "/locations/visits", token, body).statusCode());
        assertEquals(200, call("POST", path + "/locations/visits", token, body).statusCode());
        var stamps = json.readTree(call("GET", path, token, null).body()).get("stamps");
        assertEquals(2, stamps.size());
        for (var stamp : stamps) {
            long id = stamp.get("locationId").asLong();
            assertEquals(id == 10010 ? 2 : 1, stamp.get("visitCount").asInt());
            assertEquals(id == 10010 ? "Boarding stop" : "Next stop", stamp.get("stampName").asString());
        }
        verify(ember, times(1)).findLocationsByIds(List.of(10010L, 10020L));
        assertEquals(400, call("POST", path + "/locations/visits", token,
                "{\"visits\":[{\"locationId\":10010}]}" ).statusCode());
    }

    @Test
    void missingLocationInBatchDoesNotAwardAnyStops() throws Exception {
        when(ember.findLocationsByIds(anyList())).thenReturn(Map.of(
                10030L, Map.of("id", 10030L, "name", "Known stop")));
        var guest = guest();
        String path = "/api/passports/" + guest.get("id").asLong();
        String token = guest.get("accessToken").asString();
        String body = "{\"visits\":[{\"locationId\":10030,\"operationId\":\"ride-b:stop:0\"},"
                + "{\"locationId\":10040,\"operationId\":\"ride-b:stop:1\"}]}";
        assertEquals(404, call("POST", path + "/locations/visits", token, body).statusCode());
        assertEquals(0, json.readTree(call("GET", path, token, null).body()).get("stamps").size());
    }

    @Test
    void concurrentPassportsShareOneCanonicalCatalogueStamp() throws Exception {
        when(ember.findLocationById(30L)).thenReturn(Optional.of(Map.of("id", 30L, "name", "Shared stop")));
        var first = users.createUser("First");
        var second = users.createUser("Second");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.<Callable<Long>>of(
                    () -> passports.recordLocationVisit(first.getId(), 30, null, "first").getStamp().getStampId(),
                    () -> passports.recordLocationVisit(second.getId(), 30, null, "second").getStamp().getStampId()));
            assertEquals(results.get(0).get(), results.get(1).get());
        }
    }

    @Test
    void journeyLifecycleRejectsInvalidTransitionsAndMultipleActiveRides() throws Exception {
        var guest = guest();
        String token = guest.get("accessToken").asString();
        String body = journeyBody(guest.get("id").asLong());
        var first = json.readTree(call("POST", "/api/journeys", token, body).body());
        var second = json.readTree(call("POST", "/api/journeys", token, body).body());
        String firstPath = "/api/journeys/" + first.get("id").asLong();
        String secondPath = "/api/journeys/" + second.get("id").asLong();
        assertEquals(409, call("POST", firstPath + "/complete", token, null).statusCode());
        var started = call("POST", firstPath + "/start", token, null);
        assertEquals(200, started.statusCode(), started.body());
        assertEquals(json.readTree(started.body()).get("startedAt"),
                json.readTree(call("POST", firstPath + "/start", token, null).body()).get("startedAt"));
        assertEquals(409, call("POST", secondPath + "/start", token, null).statusCode());
        assertEquals(200, call("GET", "/api/journeys/active?userId=" + guest.get("id").asLong(), token, null).statusCode());
        var completed = call("POST", firstPath + "/complete", token, null);
        assertEquals(200, completed.statusCode());
        assertEquals(json.readTree(completed.body()).get("completedAt"),
                json.readTree(call("POST", firstPath + "/complete", token, null).body()).get("completedAt"));
        assertEquals(409, call("POST", firstPath + "/start", token, null).statusCode());
        assertEquals(200, call("POST", secondPath + "/start", token, null).statusCode());
        assertEquals(400, call("POST", "/api/journeys", token,
                body.replace("2026-10-04T10:00:00", "2026-10-04T08:00:00")).statusCode());
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> journeys.createJourney(
                999999L, 1L, 10L, 20L, "Origin", "Destination", LocalDateTime.now(), LocalDateTime.now().plusHours(1)));
    }

    @Test
    void concurrentJourneyStartsAllowExactlyOneActiveJourney() throws Exception {
        var user = users.createUser("Concurrent journeys");
        var first = journeys.createJourney(user.getId(), 1L, 10L, 20L, "Origin", "Destination",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        var second = journeys.createJourney(user.getId(), 1L, 10L, 20L, "Origin", "Destination",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.<Callable<Boolean>>of(
                    () -> start(first.getId()), () -> start(second.getId())));
            assertNotEquals(results.get(0).get(), results.get(1).get());
        }
        assertTrue(journeys.getActiveJourney(user.getId()).isPresent());
    }

    @Test
    void upstreamFailuresBecomeStableErrorsAndDoNotWriteAwards() throws Exception {
        when(ember.findLocationById(40L)).thenThrow(new EmberApiException("Ember is currently unavailable", null, false));
        var guest = guest();
        String path = "/api/passports/" + guest.get("id").asLong();
        var response = call("POST", path + "/locations/40/visits", guest.get("accessToken").asString(),
                "{\"operationId\":\"failure\"}");
        assertEquals(502, response.statusCode());
        assertEquals(0, json.readTree(call("GET", path, guest.get("accessToken").asString(), null).body()).get("stamps").size());
        when(ember.getLiveVehicles()).thenThrow(new EmberApiException("Ember did not respond in time", null, true));
        assertEquals(504, call("GET", "/api/vehicles/live?origin=Origin&destination=Destination", null, null).statusCode());
    }

    boolean start(Long id) {
        try { journeys.startJourney(id); return true; }
        catch (org.springframework.web.server.ResponseStatusException exception) {
            assertEquals(409, exception.getStatusCode().value()); return false;
        }
    }

    JsonNode guest() throws Exception {
        var response = call("POST", "/api/users?name=Regression", null, null);
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    HttpResponse<String> call(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(java.time.Duration.ofSeconds(15)).header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    String completion(String key, long origin, long destination) {
        return completion(key, "E1", origin, destination);
    }

    String completion(String key, String routeNumber, long origin, long destination) {
        return "{\"journeyKey\":\"" + key + "\",\"routeNumber\":\"" + routeNumber
                + "\",\"originLocationId\":" + origin + ",\"destinationLocationId\":" + destination
                + ",\"originName\":\"Origin\",\"destinationName\":\"Destination\",\"towns\":[\"Origin\",\"Destination\"]}";
    }

    String journeyBody(long userId) {
        return "{\"userId\":" + userId + ",\"emberTripId\":1,\"originLocationId\":10,\"destinationLocationId\":20,"
                + "\"originName\":\"Origin\",\"destinationName\":\"Destination\",\"scheduledDeparture\":\"2026-10-04T09:00:00\","
                + "\"scheduledArrival\":\"2026-10-04T10:00:00\"}";
    }
}
