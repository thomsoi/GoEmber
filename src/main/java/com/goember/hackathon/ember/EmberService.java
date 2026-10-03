package com.goember.hackathon.ember;

import com.goember.hackathon.stop.Stop;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EmberService {

    private final EmberClient emberClient;

    public EmberService(EmberClient emberClient) {
        this.emberClient = emberClient;
    }

    public List<Map> getStopPoints() {
        return emberClient.searchLocations(
                "",
                50,
                "STOP_POINT"
        );
    }

    public List<Stop> getStops() {

        List<Map> locations = getStopPoints();

        return locations.stream()
                .map(location -> new Stop(
                        ((Number) location.get("id")).longValue(),
                        (String) location.get("name"),
                        (String) location.get("type"),
                        ((Number) location.get("area_id")).longValue(),
                        ((Number) location.get("lat")).doubleValue(),
                        ((Number) location.get("lon")).doubleValue()
                ))
                .toList();
    }
}