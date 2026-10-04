import { test, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { ensureCurrentPassport, getLiveBuses, recordCompletedJourney, recordLocationVisit,
    recordLocationVisits, recordTownVisit } from '../src/services/BackendAPI.js';

let values;
beforeEach(() => {
    values = new Map();
    globalThis.localStorage = {
        getItem: key => values.get(key) ?? null,
        setItem: (key, value) => values.set(key, String(value)),
        removeItem: key => values.delete(key),
    };
});
const response = (body, status = 200) => new Response(JSON.stringify(body), { status });

test('guest bootstrap is shared and later reads use its credential', async () => {
    const calls = [];
    globalThis.fetch = async (url, options) => {
        calls.push({ url, options });
        return url.includes('/api/users?')
            ? response({ id: 1, name: 'Guest', accessToken: 'credential-1' })
            : response({ passportId: 1, stamps: [] });
    };
    const [first, second] = await Promise.all([ensureCurrentPassport(), ensureCurrentPassport()]);
    assert.deepEqual(first, second);
    assert.equal(calls.filter(call => call.url.includes('/api/users?')).length, 1);
    assert.equal(calls[1].options.headers.get('Authorization'), 'Bearer credential-1');
    assert.ok(calls[1].options.signal instanceof AbortSignal);
});

test('expired H2 identity is replaced rather than accessing a reused numeric ID', async () => {
    values.set('ember-passport-user-id', '1');
    values.set('ember-passport-user-token', 'old-credential');
    let reads = 0;
    globalThis.fetch = async url => {
        if (url.includes('/api/users?')) return response({ id: 2, name: 'Guest', accessToken: 'new-credential' });
        if (++reads === 1) return response({ message: 'This passport belongs to another guest' }, 403);
        return response({ passportId: 2, stamps: [] });
    };
    assert.equal((await ensureCurrentPassport()).userId, 2);
    assert.equal(values.get('ember-passport-user-token'), 'new-credential');
});

test('backend outage does not discard the guest credential or create another user', async () => {
    values.set('ember-passport-user-id', '1');
    values.set('ember-passport-user-token', 'credential');
    globalThis.fetch = async () => response({ message: 'Unavailable' }, 503);
    await assert.rejects(ensureCurrentPassport(), /Unavailable/);
    assert.equal(values.get('ember-passport-user-id'), '1');
    assert.equal(values.get('ember-passport-user-token'), 'credential');
});

test('award requests carry stable operation IDs and authorization', async () => {
    values.set('ember-passport-user-token', 'credential');
    const bodies = [];
    globalThis.fetch = async (_url, options) => {
        assert.equal(options.headers.get('Authorization'), 'Bearer credential');
        bodies.push(JSON.parse(options.body));
        return response({ visitCount: 1 });
    };
    await recordLocationVisit(1, 10, 'Origin', 'ride:stop:10');
    await recordTownVisit(1, 'Origin', 'ride:town:0');
    assert.equal(bodies[0].operationId, 'ride:stop:10');
    assert.equal(bodies[1].operationId, 'ride:town:0');
});

test('a batch sends every stop visit with its own stable operation ID', async () => {
    values.set('ember-passport-user-token', 'credential');
    const calls = [];
    globalThis.fetch = async (url, options) => {
        calls.push({ url, options });
        return new Response(null, { status: 200 });
    };
    await recordLocationVisits(1, [
        { locationId: 66, name: 'Client label', operationId: 'ride:stop:0' },
        { locationId: 1456, operationId: 'ride:stop:1' },
        { locationId: 66, operationId: 'ride:stop:2' },
    ]);
    assert.equal(calls.length, 1);
    assert.equal(calls[0].url, 'http://localhost:8080/api/passports/1/locations/visits');
    assert.equal(calls[0].options.headers.get('Authorization'), 'Bearer credential');
    assert.deepEqual(JSON.parse(calls[0].options.body), { visits: [
        { locationId: 66, operationId: 'ride:stop:0' },
        { locationId: 1456, operationId: 'ride:stop:1' },
        { locationId: 66, operationId: 'ride:stop:2' },
    ] });
});

test('completed ride sends its bus number and boarding and alighting stop IDs', async () => {
    values.set('ember-passport-user-token', 'credential');
    let sent;
    globalThis.fetch = async (_url, options) => {
        sent = JSON.parse(options.body);
        return new Response(null, { status: 200 });
    };
    await recordCompletedJourney(1, { journeyKey: 'ride-1', routeNumber: 'E31',
        originLocationId: 66, destinationLocationId: 1456,
        originName: 'Boarding', destinationName: 'Alighting', towns: ['Glasgow'] });
    assert.equal(sent.routeNumber, 'E31');
    assert.equal(sent.originLocationId, 66);
    assert.equal(sent.destinationLocationId, 1456);
});

test('invalid bus response shapes are rejected before reaching components', async () => {
    for (const body of [null, {}, [null], [{ vehicleId: 1, route: {} }]]) {
        globalThis.fetch = async () => response(body);
        await assert.rejects(getLiveBuses('Origin', 'Destination'), /invalid bus list/);
    }
    globalThis.fetch = async () => response([]);
    assert.deepEqual(await getLiveBuses('Origin', 'Destination'), []);
});

test('empty and partial searches omit absent fields and preserve the tracked trip', async () => {
    const requests = [];
    globalThis.fetch = async url => { requests.push(new URL(url)); return response([]); };
    await getLiveBuses();
    await getLiveBuses('  ', ' Glasgow ');
    await getLiveBuses(' Dundee ', '', 'trip-123');
    assert.equal(requests[0].search, '');
    assert.equal(requests[0].href, 'http://localhost:8080/api/vehicles/live');
    assert.equal(requests[1].searchParams.has('origin'), false);
    assert.equal(requests[1].searchParams.get('destination'), 'Glasgow');
    assert.equal(requests[2].searchParams.get('origin'), 'Dundee');
    assert.equal(requests[2].searchParams.get('trackedTripUid'), 'trip-123');
    assert.equal(requests[2].searchParams.has('destination'), false);
});
