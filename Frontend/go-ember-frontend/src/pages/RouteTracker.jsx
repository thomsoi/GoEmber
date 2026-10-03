import { useState } from 'react';
import DisplayRoute from '../components/DisplayRoute';
import JourneyInfoRq from '../components/JourneyInfoRq';
import LiveBusTracker from '../components/LiveBusTracker';
import '../css/RouteTracker.css';

function getRouteStops(bus, origin, destination) {
    const route = bus.route ?? [];
    const normalize = value => value?.trim().replace(/\s+/g, ' ').toLowerCase() ?? '';
    const matches = (stop, query) => [stop.name, stop.regionName]
        .some(value => normalize(value).includes(normalize(query)));
    const originIndex = route.findIndex(stop => matches(stop, origin));
    const destinationIndex = route.findIndex(
        (stop, index) => index > originIndex && matches(stop, destination),
    );
    const startIndex = originIndex >= 0 ? originIndex : 0;
    const endIndex = destinationIndex > startIndex ? destinationIndex : route.length - 1;
    const journeyStops = route.slice(startIndex, endIndex + 1);
    const currentStopIndex = journeyStops.findIndex(
        stop => stop.locationId === bus.currentStop?.locationId,
    );

    return journeyStops.map((stop, index) => ({
        id: String(stop.locationId),
        locationId: stop.locationId,
        name: stop.name || 'Ember stop',
        location: stop.regionName || '',
        visited: currentStopIndex >= 0 && index <= currentStopIndex,
    }));
}

function RouteTracker() {
    const [routeRequest, setRouteRequest] = useState(null);
    const [selectedBus, setSelectedBus] = useState(null);

    return (
        <section className="route-tracker" aria-label="Route tracker">
            {routeRequest ? (
                <>
                    <header className="route-tracker-route-summary">
                        <p className="journey-eyebrow">LIVE BUS SEARCH</p>
                        <h1>{routeRequest.startLocation} to {routeRequest.endLocation}</h1>
                        <button className="route-change-button" type="button" onClick={() => setRouteRequest(null)}>
                            Change route
                        </button>
                    </header>
                    <LiveBusTracker
                        key={`${routeRequest.startLocation}-${routeRequest.endLocation}`}
                        origin={routeRequest.startLocation}
                        destination={routeRequest.endLocation}
                        onSelectedBusChange={setSelectedBus}
                    />
                    {selectedBus?.route?.length > 0 && (
                        <DisplayRoute
                            startLocation={routeRequest.startLocation}
                            endLocation={routeRequest.endLocation}
                            routeNumber={selectedBus.routeNumber}
                            stops={getRouteStops(selectedBus, routeRequest.startLocation, routeRequest.endLocation)}
                        />
                    )}
                </>
            ) : (
                <JourneyInfoRq onStartJourney={setRouteRequest} />
            )}
        </section>
    );
}

export default RouteTracker;
