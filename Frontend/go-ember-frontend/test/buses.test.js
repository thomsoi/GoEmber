import { test } from 'node:test';
import assert from 'node:assert/strict';
import { busKey, busLabel, departureLabel } from '../src/services/buses.js';

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
