const normalize = value => (value ?? '').trim().replace(/\s+/g, ' ').toLowerCase();
const matches = (stop, query) => [stop.name, stop.regionName]
    .some(value => normalize(value).includes(normalize(query)));
const sameStop = (left, right) => left && right &&
    (left.eventId != null && right.eventId != null
        ? left.eventId === right.eventId : left.locationId === right.locationId);

function indexOfStop(route, stop) {
    return stop ? route.findIndex(candidate => sameStop(candidate, stop)) : -1;
}

export function observedRouteIndex(bus) {
    if (bus.upcomingTrip) return -1;
    const route = bus.route ?? [];
    const currentIndex = indexOfStop(route, bus.currentStop);
    const nextIndex = indexOfStop(route, bus.nextStop);
    return Math.max(currentIndex, nextIndex < 0 ? -1 : nextIndex - 1);
}

export function getRouteStops(bus, origin, destination, ride = null) {
    const fullRoute = bus.route ?? [];
    const segment = routeSegment(fullRoute, origin, destination);
    let reachedFullIndex = observedRouteIndex(bus);
    if (ride && String(bus.vehicleId) === ride.vehicleId && bus.tripUid === ride.tripUid) {
        reachedFullIndex = Math.max(reachedFullIndex,
            indexOfStop(fullRoute, ride.route[ride.reachedIndex]));
    }
    return segment.map(stop => ({
        id: String(stop.eventId ?? `${fullRoute.indexOf(stop)}:${stop.locationId}`),
        locationId: stop.locationId,
        name: stop.name || 'Ember stop',
        location: stop.regionName || '',
        visited: fullRoute.indexOf(stop) <= reachedFullIndex,
        next: Boolean(sameStop(stop, bus.nextStop)),
    }));
}

export function routeSegment(route, origin, destination) {
    const start = normalize(origin) ? route.findIndex(stop => matches(stop, origin)) : 0;
    const end = normalize(destination)
        ? route.findIndex((stop, index) => index > start && matches(stop, destination)) : route.length - 1;
    return start < 0 || end < 0 ? [] : route.slice(start, end + 1);
}

export function startRide(bus, origin, destination, journeyKey) {
    if (bus.upcomingTrip) throw new Error('Wait until this trip becomes active before boarding.');
    if (bus.routeDetailsLoaded === false) throw new Error('Wait until the full route has loaded before boarding.');
    const route = routeSegment(bus.route ?? [], origin, destination);
    const boardingIndex = route.findIndex(stop => sameStop(stop, bus.currentStop || bus.departureStop));
    if (boardingIndex < 0 || boardingIndex >= route.length - 1) {
        throw new Error('Wait until this bus is at a stop on your selected route before boarding.');
    }
    return {
        journeyKey,
        vehicleId: String(bus.vehicleId),
        tripUid: bus.tripUid,
        routeNumber: bus.routeNumber?.trim() || null,
        route: route.slice(boardingIndex),
        reachedIndex: 0,
    };
}

export function advanceRide(ride, bus) {
    if (!bus || String(bus.vehicleId) !== ride.vehicleId || bus.tripUid !== ride.tripUid) return ride;
    const fullRoute = bus.route ?? [];
    const observedIndex = observedRouteIndex(bus);
    let reachedIndex = ride.reachedIndex;
    for (let index = 0; index < ride.route.length; index++) {
        const fullIndex = indexOfStop(fullRoute, ride.route[index]);
        if (fullIndex >= 0 && fullIndex <= observedIndex) reachedIndex = Math.max(reachedIndex, index);
    }
    return { ...ride, reachedIndex };
}

export function finishRide(ride) {
    const reached = ride.route.slice(0, ride.reachedIndex + 1);
    const stops = reached.map((stop, index) =>
        ({ ...stop, operationId: `${ride.journeyKey}:stop:${index}:${stop.locationId}` }));
    if (reached.length === 0) {
        return { stops, journey: null };
    }
    const origin = reached[0];
    const destination = reached.at(-1);
    const towns = [...new Set(reached.map(stop => stop.regionName).filter(Boolean)
        .map(normalize))];
    return {
        stops,
        journey: {
            journeyKey: ride.journeyKey,
            routeNumber: ride.routeNumber ?? null,
            originLocationId: origin.locationId,
            destinationLocationId: destination.locationId,
            originName: origin.name,
            destinationName: destination.name,
            towns,
            townsToStamp: towns,
        },
    };
}

export function disconnectRide(tracker, latestBus) {
    const observedRide = latestBus ? advanceRide(tracker.ride, latestBus) : tracker.ride;
    const { stops, journey } = finishRide(observedRide);
    return { ...tracker, ride: null, stops: [...tracker.stops, ...stops],
        journeys: journey ? [...tracker.journeys, journey] : tracker.journeys };
}
