const API_BASE_URL = (import.meta.env?.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '');
const USER_ID_KEY = 'ember-passport-user-id';
const USER_NAME_KEY = 'ember-passport-user-name';
const USER_TOKEN_KEY = 'ember-passport-user-token';
export const currentGuestToken = () => localStorage.getItem(USER_TOKEN_KEY);

let passportBootstrapRequest;

async function request(path, options = {}) {
    const headers = new Headers(options.headers);
    const token = currentGuestToken();
    if (token) headers.set('Authorization', `Bearer ${token}`);
    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...options, headers, signal: options.signal ?? AbortSignal.timeout(15000),
    });
    const responseText = await response.text();
    let body = null;

    if (responseText) {
        try {
            body = JSON.parse(responseText);
        } catch {
            body = { message: responseText };
        }
    }

    if (!response.ok) {
        const error = new Error(body?.message ?? body?.detail ?? `Request failed (${response.status})`);
        error.status = response.status;
        throw error;
    }

    return body;
}

async function loadCurrentPassport() {
    const existingUserId = localStorage.getItem(USER_ID_KEY);

    if (existingUserId && currentGuestToken()) {
        try {
            const passport = await request(`/api/passports/${encodeURIComponent(existingUserId)}`);
            return {
                userId: Number(existingUserId),
                username: localStorage.getItem(USER_NAME_KEY) || 'Guest',
                passport,
            };
        } catch (error) {
            if (![401, 403, 404].includes(error.status)) throw error;
            localStorage.removeItem(USER_ID_KEY);
            localStorage.removeItem(USER_NAME_KEY);
            localStorage.removeItem(USER_TOKEN_KEY);
        }
    }

    const user = await request(`/api/users?name=${encodeURIComponent('Guest')}`, { method: 'POST' });
    localStorage.setItem(USER_ID_KEY, String(user.id));
    localStorage.setItem(USER_NAME_KEY, user.name || 'Guest');
    localStorage.setItem(USER_TOKEN_KEY, user.accessToken);
    const passport = await request(`/api/passports/${encodeURIComponent(user.id)}`);

    return { userId: user.id, username: user.name || 'Guest', passport };
}

export function ensureCurrentPassport() {
    if (!passportBootstrapRequest) {
        passportBootstrapRequest = loadCurrentPassport().finally(() => {
            passportBootstrapRequest = undefined;
        });
    }

    return passportBootstrapRequest;
}

export async function getLiveBuses(origin, destination, trackedTripUid, detailsTripUid) {
    const params = new URLSearchParams();
    if (origin?.trim()) params.set('origin', origin.trim());
    if (destination?.trim()) params.set('destination', destination.trim());
    if (trackedTripUid) params.set('trackedTripUid', trackedTripUid);
    if (detailsTripUid) params.set('detailsTripUid', detailsTripUid);
    const query = params.toString();
    const buses = await request(`/api/vehicles/live${query ? `?${query}` : ''}`);
    if (!Array.isArray(buses) || buses.some(bus => !bus || !Array.isArray(bus.route))) {
        throw new Error('The service returned an invalid bus list.');
    }
    return buses;
}

export function recordLocationVisit(userId, locationId, locationName, operationId) {
    return request(
        `/api/passports/${encodeURIComponent(userId)}/locations/${encodeURIComponent(locationId)}/visits`,
        {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ locationName, operationId }),
        },
    );
}

export function recordTownVisit(userId, townName, operationId) {
    return request(`/api/passports/${encodeURIComponent(userId)}/towns/visits`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ townName, operationId }),
    });
}

export function recordCompletedJourney(userId, journey) {
    return request(`/api/passports/${encodeURIComponent(userId)}/journeys`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(journey),
    });
}

export function recordLocationVisits(userId, stops) {
    return request(`/api/passports/${encodeURIComponent(userId)}/locations/visits`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ visits: stops.map(stop => ({
            locationId: stop.locationId, operationId: stop.operationId,
        })) }),
    });
}
