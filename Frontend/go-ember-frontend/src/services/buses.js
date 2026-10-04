export const busKey = bus => `${bus.vehicleId}:${bus.tripUid}`;

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
