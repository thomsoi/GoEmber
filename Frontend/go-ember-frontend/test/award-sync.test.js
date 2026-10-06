import { test, beforeEach, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { syncPendingAwards } from '../src/services/awardSync.js';
import { emptyTracker } from '../src/services/trackerStore.js';

const originalFetch = globalThis.fetch;
beforeEach(() => {
    globalThis.localStorage = { getItem: () => 'guest-token' };
});
afterEach(() => {
    globalThis.fetch = originalFetch;
    delete globalThis.localStorage;
});

function queue(stopCount = 1) {
    let tracker = { ...emptyTracker(),
        stops: Array.from({ length: stopCount }, (_, index) => ({
            locationId: index + 1, operationId: `ride:stop:${index}`,
        })),
        journeys: [{ journeyKey: 'ride', routeNumber: 'E1',
            originLocationId: 1, destinationLocationId: 2,
            originName: 'Start', destinationName: 'End',
            towns: ['first', 'second'], townsToStamp: ['first', 'second'] }],
    };
    return { get current() { return tracker; }, update: change => { tracker = change(tracker); } };
}

test('partial batch failure retains unacknowledged stops and retries their original operation IDs', async () => {
    const state = queue(51);
    const batches = [];
    const collected = [];
    const onCollected = stamps => collected.push(...stamps.map(stamp => stamp.operationId));
    globalThis.fetch = async (_url, options) => {
        batches.push(JSON.parse(options.body));
        if (batches.length === 2) throw new Error('offline');
        return new Response(null, { status: 200 });
    };
    await assert.rejects(syncPendingAwards(1, state.current, state.update, onCollected), /offline/);
    assert.equal(collected.length, 50);
    assert.equal(collected.includes('ride:stop:50'), false);
    assert.equal(batches[0].visits.length, 50);
    assert.deepEqual(state.current.stops.map(stop => stop.operationId), ['ride:stop:50']);
    assert.equal(state.current.journeys.length, 1);

    const retries = [];
    globalThis.fetch = async (_url, options) => {
        retries.push(JSON.parse(options.body));
        return new Response(null, { status: 200 });
    };
    await syncPendingAwards(1, state.current, state.update, onCollected);
    assert.equal(collected.length, 51);
    assert.equal(new Set(collected).size, 51);
    assert.deepEqual(retries[0], batches[1]);
    assert.deepEqual(state.current.stops, []);
    assert.deepEqual(state.current.journeys, []);
});

test('town failure resumes after the saved completion and keeps the original town operation ID', async () => {
    const state = queue(0);
    const calls = [];
    globalThis.fetch = async (url, options) => {
        const body = JSON.parse(options.body);
        calls.push({ url, body });
        if (body.townName === 'second') throw new Error('offline');
        return new Response(null, { status: 200 });
    };
    await assert.rejects(syncPendingAwards(1, state.current, state.update), /offline/);
    assert.equal(state.current.journeys[0].completionRecorded, true);
    assert.deepEqual(state.current.journeys[0].townsToStamp, ['second']);
    assert.equal('townsToStamp' in calls[0].body, false);
    assert.equal('completionRecorded' in calls[0].body, false);

    const retries = [];
    globalThis.fetch = async (_url, options) => {
        retries.push(JSON.parse(options.body));
        return new Response(null, { status: 200 });
    };
    await syncPendingAwards(1, state.current, state.update);
    assert.deepEqual(retries, [{ townName: 'second', operationId: 'ride:town:1' }]);
    assert.deepEqual(state.current.journeys, []);
});

test('local checkpoint failure keeps the queue and stops further server writes', async () => {
    const state = queue();
    let writes = 0;
    const notifications = [];
    globalThis.fetch = async () => {
        writes++;
        return new Response(null, { status: 200 });
    };
    await assert.rejects(syncPendingAwards(1, state.current, () => {
        throw new Error('storage full');
    }, stamps => notifications.push(stamps)), /storage full/);
    assert.deepEqual(notifications, []);
    assert.equal(writes, 1);
    assert.equal(state.current.stops.length, 1);
    assert.equal(state.current.journeys.length, 1);
});
