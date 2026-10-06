import { test } from 'node:test';
import assert from 'node:assert/strict';
import { busKey, busLabel, busStopsLabel, departureLabel } from '../src/services/buses.js';

const route = ['Origin', 'Most recently visited', 'Stirling Castleview Park and Ride'].map((name, index) =>
    ({ eventId: 100 + index, locationId: 10 + index, name }));
const betweenStops = { route, routeDetailsLoaded: true, currentStop: null, nextStop: route[2] };

test('stop summary shows the most recently visited stop between stops and advances with the next stop', () => {
    assert.equal(busStopsLabel(betweenStops), 'Most recently visited → Stirling Castleview Park and Ride');
    assert.equal(busStopsLabel({ ...betweenStops, nextStop: route[1] }), 'Origin → Most recently visited');
    assert.equal(betweenStops.currentStop, null);
});

test('an explicitly reported current stop takes precedence and the terminal stop needs no arrow', () => {
    assert.equal(busStopsLabel({ ...betweenStops, currentStop: { name: 'Reported stop' } }),
        'Reported stop → Stirling Castleview Park and Ride');
    assert.equal(busStopsLabel({ ...betweenStops, currentStop: route[2], nextStop: null }),
        'Stirling Castleview Park and Ride');
});

test('stop summary does not invent a visit before departure or from incomplete route data', () => {
    for (const overrides of [
        { routeDetailsLoaded: false }, { routeDetailsLoaded: undefined }, { upcomingTrip: true },
        { route: [] }, { route: undefined }, { nextStop: { eventId: 999, name: route[2].name } },
    ]) {
        assert.equal(busStopsLabel({ ...betweenStops, ...overrides }),
            'Current stop unavailable → Stirling Castleview Park and Ride');
    }
    assert.equal(busStopsLabel({ ...betweenStops, nextStop: route[0] }), 'Current stop unavailable → Origin');
    assert.equal(busStopsLabel({ ...betweenStops, nextStop: null }), 'Current stop unavailable');
});

test('stop summary distinguishes repeated visits to the same location by route event', () => {
    const loop = [route[0], route[1], { ...route[0], eventId: 200 }, route[2]];
    assert.equal(busStopsLabel({ ...betweenStops, route: loop, nextStop: loop[2] }),
        'Most recently visited → Origin');
    assert.equal(busStopsLabel({ ...betweenStops, route: loop }), 'Origin → Stirling Castleview Park and Ride');
});

test('departure labels use London time, include the date and distinguish leaving now', () => {
    const now = Date.parse('2026-10-04T12:00:00Z');
    assert.equal(departureLabel({ departureTime: '2026-10-04T12:00:00Z' }, now), 'Leaving now');
    assert.equal(departureLabel({ departureTime: '2026-10-04T11:59:00Z' }, now), 'Leaving now');
    assert.equal(departureLabel({ departureTime: '2026-10-04T12:05:00Z' }, now), 'Departs 13:05 4 Oct');
    assert.equal(departureLabel({ departureTime: '2026-12-04T12:05:00Z' }, now), 'Departs 12:05 4 Dec');
    assert.equal(departureLabel({ departureTime: null }, now), 'Trip in progress');
    assert.equal(departureLabel({ departureTime: 'invalid' }, now), 'Departure time unavailable');
});

test('bus labels identify actual stops and separate two trips using the same vehicle', () => {
    const bus = { vehicleId: 1, tripUid: 'active', routeNumber: 'E1', departureStop: { name: 'Middle' },
        route: [{ name: 'Origin' }, { name: 'Destination' }], departureTime: '2026-10-04T12:05:00Z' };
    assert.match(busLabel(bus, Date.parse('2026-10-04T12:00:00Z')), /Middle → Destination · Departs 13:05/);
    assert.notEqual(busKey(bus), busKey({ ...bus, tripUid: 'next' }));
});
