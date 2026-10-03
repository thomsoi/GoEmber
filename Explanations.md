# GoEmber architecture and API overview

## Overall architecture

GoEmber is split into a React/Vite frontend and a Spring Boot backend.

- **Frontend (`Frontend/go-ember-frontend`)** contains the route tracker and passport UI. React components render the pages, while `services/BackendAPI.js` centralizes calls to the backend. The frontend uses `VITE_API_BASE_URL` when set and otherwise calls `http://localhost:8080`.
- **Backend (`src/main/java/com/goember/hackathon`)** exposes REST endpoints. Controllers handle HTTP input and output, services coordinate business rules and external Ember data, repositories persist entities through Spring Data JPA, and entities represent the database records.
- **Persistence** stores users, passports, stamps, visits, and journeys in the configured relational database. PostgreSQL is used for the normal database setup.
- **Ember integration** is handled by the backend. It retrieves stop and live-vehicle information and maps the live feed into application response records for the frontend.

The main data path for passport stamps is:

```text
LiveBusTracker
  -> BackendAPI.js
  -> PassportController
  -> PassportService
  -> StampService / repositories
  -> database
```

## API calls used by the frontend

The current route-tracking and passport experience uses these backend calls:

| Call | Purpose |
| --- | --- |
| `POST /api/users?name=Guest` | Creates a guest user when the browser has no usable stored user ID. |
| `GET /api/passports/{userId}` | Loads or creates that user's passport and returns its stamps and travel totals. |
| `GET /api/vehicles/live?origin={origin}&destination={destination}` | Fetches live buses whose route serves the entered origin and destination. The tracker polls this endpoint while open. |
| `POST /api/passports/{userId}/locations/{emberLocationId}/visits` | Records a stop visit and returns the stamp's name, tier, visit count, and visit timestamps. The request body may include `locationName`. |

The browser keeps the guest user ID and name in `localStorage`. When a ride ends, the tracker queues the origin, destination, and detected passed stops. The user chooses **Add stamps to passport** to submit those queued locations through the visit endpoint.

## Other backend endpoints

These endpoints are available in the backend but are not part of the current frontend stamp submission flow:

- `GET /api/users` and `GET /api/users/{userId}` list or retrieve users.
- `GET /api/stops`, `GET /api/stops/nearest`, and `GET /api/stops/nearby` expose stop discovery by location.
- `GET /api/ember/stops`, `GET /api/ember/stops/nearby`, and `GET /api/ember/trip/{tripId}` expose Ember stop and trip data.
- `POST /api/journeys`, `GET /api/journeys/{journeyId}`, `GET /api/journeys/active?userId={userId}`, `POST /api/journeys/{journeyId}/start`, and `POST /api/journeys/{journeyId}/complete` manage persisted journey records.

## Main architectural decisions

- **Layered backend responsibilities:** controllers map HTTP requests and responses, services own business logic, and repositories handle persistence. This keeps API concerns separate from stamp and passport rules.
- **One passport stamp per location:** a passport stamp is uniquely associated with a passport and a stamp/location. Recording another visit updates its visit count, tier, and most recent visit rather than creating a duplicate passport entry.
- **Defer stamp submission until the ride ends:** the live tracker identifies passed stops from the selected bus's successive live route positions and holds them in a client-side queue. The user explicitly confirms with **Add stamps to passport** before visits are persisted. The client deduplicates queued locations, while the backend's passport/location uniqueness also protects the persisted collection.
- **Use Ember's route stop IDs as stamp identities:** each stamp is associated with the Ember location ID. The entered origin and destination are matched to stops on the bus route, and their entered names are sent as display names.
- **Keep tier visuals in the frontend:** the passport API returns the stamp tier as data; the React `Stamp` component selects the matching default, bronze, silver, or gold image asset. This keeps presentation independent of backend persistence.
- **Keep journey polling in the route-tracker component:** live bus positions are polled by `LiveBusTracker`; its cursor compares current and next stops between polls, allowing it to detect stops passed even when the feed advances by more than one stop.

