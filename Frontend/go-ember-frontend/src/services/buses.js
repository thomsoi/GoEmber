import { observedRouteIndex } from './ride.js';

export const busKey = bus => `${bus.vehicleId}:${bus.tripUid}`;

export function busStopsLabel(bus) {
    // The live feed can omit current_stop between stops. Only the full route
    // can identify the preceding stop; the sparse feed may contain endpoints only.
    const lastStop = bus.routeDetailsLoaded && !bus.upcomingTrip
        ? bus.route?.[observedRouteIndex(bus)] : null;
    const from = bus.currentStop?.name || lastStop?.name || 'Current stop unavailable';
    return `${from}${bus.nextStop?.name ? ` → ${bus.nextStop.name}` : ''}`;
}

export function departureLabel(bus, now = Date.now()) {
    if (!bus.departureTime) return 'Trip in progress';
    const time = Date.parse(bus.departureTime);
    if (!Number.isFinite(time)) return 'Departure time unavailable';
    if (time <= now) return 'Leaving now';
    return `Departs ${new Date(time).toLocaleTimeString('en-GB', {
        timeZone: 'Europe/London', hour: '2-digit', minute: '2-digit',
    })} ${new Date(time).toLocaleDateString('en-GB', {
        timeZone: 'Europe/London', day: 'numeric', month: 'short',
    })}`;
}

export function busLabel(bus, now) {
    const from = bus.departureStop?.name || bus.route?.[0]?.name || 'Unknown stop';
    const to = bus.route?.at(-1)?.name || 'Unknown destination';
    return `${bus.routeNumber || 'Bus'} · ${from} → ${to} · ${departureLabel(bus, now)}`;
}
