# Ember Passport frontend

React and Vite frontend for the Spring Boot API. See the [repository guide](../../README.md)
for setup, configuration, architecture, and verification commands.

`src/main.jsx` mounts `App`; `/` displays the passport and `/route-tracker` displays
the live journey tracker. Navigation uses normal links, with ride progress restored
from local storage. Keep HTTP calls in `services/BackendAPI.js`, ride calculations in
`services/ride.js`, storage in `services/trackerStore.js`, and award uploads in
`services/awardSync.js`.

From this directory use `npm ci`, `npm run dev`, `npm test`, `npm run lint`, and
`npm run build`. Set `VITE_API_BASE_URL` before starting Vite or building to override
the default backend address.
