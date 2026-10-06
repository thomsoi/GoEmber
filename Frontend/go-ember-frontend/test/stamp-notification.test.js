import { test } from 'node:test';
import assert from 'node:assert/strict';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { createServer } from 'vite';

test('stamp notification announces saved stamps with singular, batch and empty states', async () => {
    const server = await createServer({ server: { middlewareMode: true, ws: false } });
    try {
        const { default: StampNotification } = await server.ssrLoadModule('/src/components/StampNotification.jsx');
        const render = notification => renderToStaticMarkup(React.createElement(StampNotification,
            { notification, onDismiss() {} }));
        const single = render({ count: 1, name: 'Stirling Castleview Park and Ride' });
        assert.match(single, /Stamp collected!/);
        assert.match(single, /Stirling Castleview Park and Ride/);
        assert.match(single, /Saved to your passport/);
        assert.match(single, /role="status"/);
        assert.match(single, /aria-label="Dismiss stamp notification"/);
        const batch = render({ count: 3, name: 'Origin' });
        assert.match(batch, /3 stamps collected!/);
        assert.doesNotMatch(batch, /Origin/);
        const empty = render(null);
        assert.match(empty, /aria-live="polite"/);
        assert.doesNotMatch(empty, /collected!|<button|<img/);
    } finally { await server.close(); }
});
