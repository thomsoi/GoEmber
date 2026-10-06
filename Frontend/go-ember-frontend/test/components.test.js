import { test } from 'node:test';
import assert from 'node:assert/strict';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { createServer } from 'vite';
import { emptyTracker } from '../src/services/trackerStore.js';

test('the initial route page shows only the form and allows blank search fields', async () => {
    const server = await createServer({ server: { middlewareMode: true } });
    globalThis.localStorage = { getItem: () => null };
    try {
        const { default: RouteTracker } = await server.ssrLoadModule('/src/pages/RouteTracker.jsx');
        const markup = renderToStaticMarkup(React.createElement(RouteTracker));
        assert.match(markup, /Plan a journey/);
        assert.match(markup, /leave the fields blank/);
        assert.doesNotMatch(markup, /required=""/);
        assert.match(markup, /Find live buses/);
        assert.doesNotMatch(markup, /live-bus-tracker|Loading buses/);
    } finally { await server.close(); delete globalThis.localStorage; }
});

test('submitted blank and filtered searches open live buses, while recovered awards remain accessible', async () => {
    const server = await createServer({ server: { middlewareMode: true } });
    try {
        const { default: RouteTracker } = await server.ssrLoadModule('/src/pages/RouteTracker.jsx');
        for (const routeRequest of [
            { startLocation: '', endLocation: '' },
            { startLocation: 'Edinburgh', endLocation: 'Dundee' },
            { startLocation: 'Edinburgh', endLocation: '' },
        ]) {
            globalThis.localStorage = { getItem: () => JSON.stringify({ ...emptyTracker(), routeRequest }) };
            const markup = renderToStaticMarkup(React.createElement(RouteTracker));
            assert.match(markup, /live-bus-tracker/);
            assert.match(markup, /Loading buses/);
            assert.doesNotMatch(markup, /Plan a journey/);
            if (!routeRequest.startLocation && !routeRequest.endLocation) assert.match(markup, /Soonest departures/);
        }
        globalThis.localStorage = { getItem: () => JSON.stringify({ ...emptyTracker(),
            stops: [{ operationId: 'ride:stop:10', locationId: 10 }] }) };
        const recovered = renderToStaticMarkup(React.createElement(RouteTracker));
        assert.match(recovered, /Add stamps to passport \(1\)/);
    } finally { await server.close(); delete globalThis.localStorage; }
});

test('an unavailable bus still exposes the end-ride action after session recovery', async () => {
    const server = await createServer({ server: { middlewareMode: true } });
    try {
        const { default: LiveBusTracker } = await server.ssrLoadModule('/src/components/LiveBusTracker.jsx');
        const tracker = { ...emptyTracker(), ride: { vehicleId: '8', route: [], journeyKey: 'ride' } };
        const markup = renderToStaticMarkup(React.createElement(LiveBusTracker,
            { origin: 'Origin', destination: 'Destination', tracker, updateTracker() {} }));
        assert.match(markup, /I got off this bus/);
        assert.match(markup, /Tracked bus unavailable/);
        assert.doesNotMatch(markup, /Add stamps to passport/);
    } finally { await server.close(); }
});

test('recovered pending awards remain saveable without an active bus', async () => {
    const server = await createServer({ server: { middlewareMode: true } });
    try {
        const { default: LiveBusTracker } = await server.ssrLoadModule('/src/components/LiveBusTracker.jsx');
        const tracker = { ...emptyTracker(), stops: [{ operationId: 'ride:stop:10', locationId: 10 }] };
        const markup = renderToStaticMarkup(React.createElement(LiveBusTracker,
            { origin: 'Origin', destination: 'Destination', tracker, updateTracker() {} }));
        assert.match(markup, /Add stamps to passport \(1\)/);
    } finally { await server.close(); }
});

test('a recovered ride keeps its visited and future route dots visible without a live poll', async () => {
    const server = await createServer({ server: { middlewareMode: true } });
    const route = ['Origin', 'Middle', 'Destination'].map((name, index) =>
        ({ eventId: index + 100, locationId: index + 10, name }));
    const tracker = { ...emptyTracker(), ride: { vehicleId: '8', tripUid: 'trip',
        route, reachedIndex: 1, journeyKey: 'ride' } };
    globalThis.localStorage = { getItem: () => JSON.stringify(tracker) };
    try {
        const { default: RouteTracker } = await server.ssrLoadModule('/src/pages/RouteTracker.jsx');
        const markup = renderToStaticMarkup(React.createElement(RouteTracker));
        assert.match(markup, /LIVE BUS ROUTE/);
        assert.equal((markup.match(/display-route-stop is-visited/g) ?? []).length, 2);
        assert.doesNotMatch(markup, /display-route-stop is-next/);
    } finally { await server.close(); delete globalThis.localStorage; }
});

test('passport cover labels the distinct bus and town counters clearly', async () => {
    const server = await createServer({ server: { middlewareMode: true } });
    try {
        const { default: Passport } = await server.ssrLoadModule('/src/components/Passport.jsx');
        const markup = renderToStaticMarkup(React.createElement(Passport,
            { busNumbersRidden: 2, cities: [{ key: 'a', name: 'A' }, { key: 'b', name: 'B' }, { key: 'c', name: 'C' }], distanceTravelledKm: 62.5 }));
        assert.match(markup, /aria-label="View all 2 bus numbers ridden"/);
        assert.match(markup, /View buses/);
        assert.match(markup, /aria-label="View all 3 visited cities and towns"/);
        assert.match(markup, /View cities/);
        assert.match(markup, /Distance travelled \(estimated\)/);
        assert.match(markup, /Estimated from stop locations; not road mileage/);
    } finally { await server.close(); }
});

test('passport collections paginate independently and never mix cities with stop stamps', async () => {
    const server = await createServer({ server: { middlewareMode: true } });
    try {
        const { default: Collection } = await server.ssrLoadModule('/src/components/PassportCollection.jsx');
        const cities = ['Aberdeen', 'Dundee', 'Edinburgh', 'Glasgow', 'Perth', 'Stirling', 'Inverness'].map((name, index) => ({ key: name, name, visitCount: index + 1 }));
        const stamps = [{ id: 'location:1', stopName: 'Buchanan Bus Station', level: 'bronze', timesCollected: 10 }];
        const buses = ['E1', 'E2', 'E7', 'E10', 'E20', 'E21', 'E22'].map((routeNumber, index) => ({ routeNumber, rideCount: index + 1 }));
        const render = (section, page, overrides = {}) => renderToStaticMarkup(React.createElement(Collection,
            { section, page, cities, stamps, buses, ...overrides }));
        const firstBuses = render('buses', 1);
        assert.match(firstBuses, /Bus numbers ridden/);
        assert.match(firstBuses, /Page 1 of 2/);
        assert.match(firstBuses, /<h3>E1<\/h3>/);
        assert.match(firstBuses, /<h3>E7<\/h3>/);
        assert.match(firstBuses, /Ridden 1 time<\/p>/);
        assert.match(firstBuses, /Ridden 3 times<\/p>/);
        assert.equal((firstBuses.match(/class="passport-bus"/g) ?? []).length, 6);
        assert.match(firstBuses, /<h3>E21<\/h3>/);
        assert.doesNotMatch(firstBuses, /E22|Glasgow|Buchanan Bus Station/);
        const secondBuses = render('buses', 2);
        assert.match(secondBuses, /<h3>E22<\/h3>/);
        assert.equal((secondBuses.match(/class="passport-bus"/g) ?? []).length, 1);
        assert.match(secondBuses, /Page 2 of 2/);
        assert.doesNotMatch(secondBuses, /<h3>E1<\/h3>|Glasgow|Buchanan Bus Station/);
        assert.match(render('buses', 1, { buses: [] }), /No bus numbers recorded yet/);
        const firstCities = render('cities', 1);
        assert.match(firstCities, /Cities and towns visited/);
        assert.match(firstCities, /Page 1 of 2/);
        assert.match(firstCities, /Glasgow/);
        assert.match(firstCities, /Visited 1 time<\/p>/);
        assert.match(firstCities, /Visited 4 times<\/p>/);
        assert.equal((firstCities.match(/class="passport-city"/g) ?? []).length, 6);
        assert.match(firstCities, /Stirling/);
        assert.doesNotMatch(firstCities, /Inverness|Buchanan Bus Station|stamp-seal|Collected 10/);
        const secondCities = render('cities', 2);
        assert.match(secondCities, /Inverness/);
        assert.equal((secondCities.match(/class="passport-city"/g) ?? []).length, 1);
        assert.match(secondCities, /Page 2 of 2/);
        assert.doesNotMatch(secondCities, /Glasgow|Buchanan Bus Station/);
        const stops = render('stamps', 1);
        assert.match(stops, /Bus stop stamps/);
        assert.match(stops, /Buchanan Bus Station/);
        assert.match(stops, /Collected 10 times/);
        assert.match(stops, /Page 1 of 1/);
        assert.doesNotMatch(stops, /Glasgow|Perth|passport-city/);
        assert.match(render('cities', 1, { cities: [] }), /No cities or towns visited yet/);
        assert.match(render('stamps', 1, { stamps: [] }), /No bus stop stamps collected yet/);
        assert.match(render('buses', 1, { buses: buses.slice(0, 6) }), /Page 1 of 1/);
        assert.match(render('cities', 1, { cities: cities.slice(0, 6) }), /Page 1 of 1/);
    } finally { await server.close(); }
});
