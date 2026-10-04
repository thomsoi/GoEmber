import { test } from 'node:test';
import assert from 'node:assert/strict';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { createServer } from 'vite';
import { emptyTracker } from '../src/services/trackerStore.js';

test('the initial route page shows departures and allows blank search fields', async () => {
    const server = await createServer({ server: { middlewareMode: true } });
    globalThis.localStorage = { getItem: () => null };
    try {
        const { default: RouteTracker } = await server.ssrLoadModule('/src/pages/RouteTracker.jsx');
        const markup = renderToStaticMarkup(React.createElement(RouteTracker));
        assert.match(markup, /Soonest departures/);
        assert.match(markup, /leave the fields blank/);
        assert.doesNotMatch(markup, /required=""/);
        assert.match(markup, /Loading buses/);
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
            { busNumbersRidden: 2, townsVisited: 3, distanceTravelledKm: 62.5 }));
        assert.match(markup, /Bus numbers ridden<\/dt><dd>2<\/dd>/);
        assert.match(markup, /Cities and towns visited<\/dt><dd>3<\/dd>/);
        assert.match(markup, /Distance travelled \(estimated\)/);
    } finally { await server.close(); }
});
