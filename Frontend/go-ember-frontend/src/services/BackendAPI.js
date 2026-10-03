const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '');
const USER_ID_KEY = 'ember-passport-user-id';
const USER_NAME_KEY = 'ember-passport-user-name';

let passportBootstrapRequest;

async function request(path, options = {}) {
    const response = await fetch(`${API_BASE_URL}${path}`, options);
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
        const error = new Error(body?.message ?? `Request failed (${response.status})`);
        error.status = response.status;
        throw error;
    }

    return body;
}

async function loadCurrentPassport() {
    const existingUserId = localStorage.getItem(USER_ID_KEY);

    if (existingUserId) {
        try {
            const passport = await request(`/api/passports/${encodeURIComponent(existingUserId)}`);
            return {
                userId: Number(existingUserId),
                username: localStorage.getItem(USER_NAME_KEY) || 'Guest',
                passport,
            };
        } catch (error) {
            if (error.status !== 404) throw error;
            localStorage.removeItem(USER_ID_KEY);
            localStorage.removeItem(USER_NAME_KEY);
        }
    }

    const user = await request(`/api/users?name=${encodeURIComponent('Guest')}`, { method: 'POST' });
    localStorage.setItem(USER_ID_KEY, String(user.id));
    localStorage.setItem(USER_NAME_KEY, user.name || 'Guest');
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

export function getLiveBuses(origin, destination) {
    const params = new URLSearchParams({ origin, destination });
    return request(`/api/vehicles/live?${params}`);
}

export function recordLocationVisit(userId, locationId, locationName) {
    return request(
        `/api/passports/${encodeURIComponent(userId)}/locations/${encodeURIComponent(locationId)}/visits`,
        {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ locationName }),
        },
    );
}

export function recordTownVisit(userId, townName) {
    return request(`/api/passports/${encodeURIComponent(userId)}/towns/visits`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ townName }),
    });
}

export function recordCompletedJourney(userId, journey) {
    return request(`/api/passports/${encodeURIComponent(userId)}/journeys`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(journey),
    });
}