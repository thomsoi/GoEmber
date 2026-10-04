import { test } from 'node:test';
import assert from 'node:assert/strict';
import { startRide, advanceRide, finishRide, disconnectRide, routeSegment, getRouteStops } from '../src/services/ride.js';
import { emptyTracker, loadTracker, saveTracker, reassignQueuedAwards, TRACKER_KEY } from '../src/services/trackerStore.js';

const route = ['Before', 'Origin', 'Middle', 'Destination', 'After'].map((name, index) =>
    ({ locationId: index + 1, name, regionName: name }));
const bus = (index, overrides = {}) => ({ vehicleId: 8, tripUid: 'trip-1', routeNumber: 'E1', route,
    currentStop: route[index], nextStop: route[index + 1], ...overrides });
const begin = key => startRide(bus(1), 'Origin', 'Destination', key ?? 'ride-1');

test('blank and partial routes can be tracked without inventing boarding progress', () => {
    assert.deepEqual(routeSegment(route, '', ''), route);
    assert.deepEqual(routeSegment(route, 'Origin', '').map(stop => stop.name), ['Origin', 'Middle', 'Destination', 'After']);
    assert.deepEqual(routeSegment(route, '', 'Destination').map(stop => stop.name), ['Before', 'Origin', 'Middle', 'Destination']);
    const ride = startRide(bus(1), '', '', 'browse-ride');
    assert.equal(ride.route[0].name, 'Origin');
    assert.deepEqual(finishRide(ride).stops.map(stop => stop.name), ['Origin']);
    assert.throws(() => startRide(bus(1, { upcomingTrip: true }), '', '', 'future'), /becomes active/);
});

test('route matching normalizes names and preserves ordered endpoints', () => {
    assert.deepEqual(routeSegment(route, ' ORIGIN ', 'destination').map(stop => stop.name),
        ['Origin', 'Middle', 'Destination']);
    assert.deepEqual(routeSegment(route, 'Destination', 'Origin'), []);
});

test('requires an observed boarding stop inside the requested route', () => {
    assert.throws(() => startRide(bus(0), 'Origin', 'Destination', '1'), /Wait until/);
    assert.throws(() => startRide(bus(3), 'Origin', 'Destination', '1'), /Wait until/);
    assert.throws(() => startRide(bus(1, { currentStop: null }), 'Origin', 'Destination', '1'), /Wait until/);
});

test('getting off immediately records only the boarding stop and a zero-distance bus ride', () => {
    const result = finishRide(begin());
    assert.equal(result.journey.routeNumber, 'E1');
    assert.equal(result.journey.originLocationId, result.journey.destinationLocationId);
    assert.deepEqual(result.stops.map(stop => stop.name), ['Origin']);
});

test('boarding uses the known departure stop when the live feed has no current stop', () => {
    const ride = startRide(bus(1, { currentStop: null, departureStop: route[1] }), 'Origin', 'Destination', 'board');
    assert.deepEqual(finishRide(ride).stops.map(stop => stop.name), ['Origin']);
});

test('disconnect includes all intermediate stops reached in the final live observation', () => {
    const completed = disconnectRide({ ...emptyTracker(), ride: begin() }, bus(3));
    assert.equal(completed.ride, null);
    assert.deepEqual(completed.stops.map(stop => stop.name), ['Origin', 'Middle', 'Destination']);
    assert.equal(completed.journeys[0].destinationName, 'Destination');
    const storage = { data: null, setItem(_key, value) { this.data = value; }, getItem() { return this.data; } };
    saveTracker(storage, completed);
    assert.deepEqual(loadTracker(storage).stops, completed.stops);
});

test('instant disconnect queues exactly the boarding stop for saving', () => {
    const completed = disconnectRide({ ...emptyTracker(), ride: begin() }, bus(1));
    assert.deepEqual(completed.stops.map(stop => stop.name), ['Origin']);
    assert.equal(completed.journeys.length, 1);
    assert.equal(completed.journeys[0].routeNumber, 'E1');
});

test('live route dots use the next stop when the feed has no current stop', () => {
    const live = bus(2, { currentStop: null });
    assert.deepEqual(getRouteStops(live, 'Origin', 'Destination').map(stop => stop.visited),
        [true, true, false]);
    assert.deepEqual(getRouteStops(live, 'Origin', 'Destination').map(stop => stop.next),
        [false, false, true]);
    assert.deepEqual(finishRide(advanceRide(begin(), live)).stops.map(stop => stop.name),
        ['Origin', 'Middle']);
    assert.deepEqual(getRouteStops(bus(4), 'Origin', 'Destination').map(stop => stop.visited),
        [true, true, true]);
    assert.deepEqual(getRouteStops({ ...live, upcomingTrip: true }, 'Origin', 'Destination')
        .map(stop => stop.visited), [false, false, false]);
    assert.equal(getRouteStops(bus(0), 'Middle', 'Destination').some(stop => stop.next), false);
});

test('tracked route dots do not move backward during a sparse live update', () => {
    const ride = advanceRide(begin(), bus(3));
    const sparse = bus(1, { currentStop: null, nextStop: null });
    assert.deepEqual(getRouteStops(sparse, 'Origin', 'Destination', ride).map(stop => stop.visited),
        [true, true, true]);
    assert.equal(advanceRide(ride, sparse).reachedIndex, 2);
});

test('a route that revisits the same location records each distinct stop event', () => {
    const loop = [
        { locationId: 66, eventId: 100, name: 'Start', regionName: 'Town' },
        { locationId: 1456, eventId: 101, name: 'Middle', regionName: 'Village' },
        { locationId: 66, eventId: 102, name: 'Start', regionName: 'Town' },
        { locationId: 20, eventId: 103, name: 'End', regionName: 'City' },
    ];
    const first = { vehicleId: 8, tripUid: 'loop', routeNumber: 'E31', route: loop,
        currentStop: loop[0], nextStop: loop[1] };
    const last = { ...first, currentStop: loop[2], nextStop: loop[3] };
    const result = finishRide(advanceRide(startRide(first, '', '', 'loop-ride'), last));
    assert.deepEqual(result.stops.map(stop => stop.locationId), [66, 1456, 66]);
    assert.equal(new Set(result.stops.map(stop => stop.operationId)).size, 3);
    assert.equal(result.journey.routeNumber, 'E31');
    assert.equal(result.journey.originLocationId, result.journey.destinationLocationId);
    const dots = getRouteStops(last, '', '');
    assert.equal(new Set(dots.map(stop => stop.id)).size, loop.length);
    assert.deepEqual(dots.map(stop => stop.visited), [true, true, true, false]);
});

test('early alighting awards only observed stops, towns and distance endpoints', () => {
    const result = finishRide(advanceRide(begin(), bus(2)));
    assert.deepEqual(result.stops.map(stop => stop.name), ['Origin', 'Middle']);
    assert.equal(result.journey.routeNumber, 'E1');
    assert.equal(result.journey.destinationLocationId, 3);
    assert.deepEqual(result.journey.towns, ['origin', 'middle']);
});

test('skipped polls collect intermediate stops once', () => {
    const ride = advanceRide(begin(), bus(3));
    const repeated = advanceRide(ride, bus(3));
    assert.deepEqual(finishRide(repeated).stops.map(stop => stop.locationId), [2, 3, 4]);
});

test('missing buses and changed trips preserve an endable ride', () => {
    const ride = advanceRide(begin(), bus(2));
    assert.equal(advanceRide(ride, null), ride);
    assert.equal(advanceRide(ride, bus(3, { tripUid: 'trip-2' })), ride);
    assert.equal(finishRide(ride).journey.destinationName, 'Middle');
});

test('backward observations cannot invent progress and later stops clamp to the selected destination', () => {
    const ride = advanceRide(begin(), bus(2));
    assert.equal(advanceRide(ride, bus(1)).reachedIndex, 1);
    const result = finishRide(advanceRide(ride, bus(4)));
    assert.deepEqual(result.stops.map(stop => stop.name), ['Origin', 'Middle', 'Destination']);
    assert.equal(advanceRide(ride, bus(0)).reachedIndex, 1);
});

test('ordered next-stop transition is usable when current-stop data is absent', () => {
    const ride = advanceRide(begin(), bus(1, { currentStop: null, nextStop: route[3] }));
    assert.equal(finishRide(ride).journey.destinationName, 'Middle');
});

test('separate rides retain separate award operation IDs', () => {
    const first = finishRide(advanceRide(begin('first'), bus(2)));
    const second = finishRide(advanceRide(begin('second'), bus(2)));
    assert.notEqual(first.stops[0].operationId, second.stops[0].operationId);
    assert.equal(new Set([...first.stops, ...second.stops].map(stop => stop.operationId)).size, 4);
});

test('storage round trip preserves ride progress and pending operations', () => {
    const data = new Map();
    const storage = { getItem: key => data.get(key) ?? null, setItem: (key, value) => data.set(key, value) };
    assert.deepEqual(loadTracker(storage), emptyTracker());
    const finished = finishRide(advanceRide(begin(), bus(2)));
    const tracker = { ...emptyTracker(), ownerToken: 'guest', routeRequest: { startLocation: 'Origin', endLocation: 'Destination' },
        ride: advanceRide(begin('second'), bus(2)), stops: finished.stops, journeys: [finished.journey] };
    saveTracker(storage, tracker);
    assert.deepEqual(loadTracker(storage), tracker);
    data.set(TRACKER_KEY, '{broken');
    assert.throws(() => loadTracker(storage));
});

test('storage failures are surfaced rather than pretending the queue was saved', () => {
    assert.throws(() => saveTracker({ setItem() { throw new Error('quota'); } }, emptyTracker()), /quota/);
});

test('confirmed pending awards can move to a replacement H2 guest without dropping operation IDs', () => {
    const stop = { operationId: 'ride:stop:66', locationId: 66, name: 'Buchanan Bus Stn' };
    const pending = { ...emptyTracker(), ownerToken: 'previous-guest', stops: [stop] };
    const moved = reassignQueuedAwards(pending, 'replacement-guest');
    assert.equal(moved.ownerToken, 'replacement-guest');
    assert.deepEqual(moved.stops, [stop]);
    assert.equal(pending.ownerToken, 'previous-guest');
    assert.throws(() => reassignQueuedAwards({ ...pending, ride: { route: [] } }, 'replacement-guest'), /Only pending/);
    assert.throws(() => reassignQueuedAwards(pending, ''), /Only pending/);
});
