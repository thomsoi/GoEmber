# Ember Passport

A travel passport for Ember buses: browse departures, track a ride, and collect
stop and town stamps. Uses Java 21, Spring Boot, H2, React, and Vite.

## Run locally

Install the frontend's locked dependencies from the repository root:

```powershell
npm.cmd --prefix Frontend/go-ember-frontend ci
```

Start the backend and frontend in separate terminals:

```powershell
.\mvnw.cmd spring-boot:run
```

```powershell
npm.cmd run dev
```

The backend defaults to port 8080; Vite defaults to 5173. Use the URL printed by
Vite. On Unix, use `./mvnw` and `npm`. Use a Node.js version supported by the
installed Vite package (see its `engines`).

Restart the backend after Java changes. H2 is an in-memory database: restarting
clears guest accounts, passports, stamps, and journeys. The browser replaces an
expired guest session and asks before moving pending awards to the new passport.

## Repository map

| Path | Responsibility |
| --- | --- |
| `src/main/java/com/goember/hackathon` | Backend; entry point `PassportApplication` |
| `ember` package | External HTTP client, route mapping, departure selection |
| `user`, `passport`, `stamp`, `journey`, `stop` packages | Feature controllers, services, entities, repositories |
| `geo` package | Shared distance calculation |
| `config` package | HTTP client, CORS, central API exception handling |
| `src/main/proto` | Live vehicle wire schema; Maven generates Java into `target` |
| `src/main/resources/application.properties` | Backend configuration defaults |
| `Frontend/go-ember-frontend/src` | React entry point, pages, components, CSS, assets |
| Frontend `src/services` | Backend requests, ride rules, local persistence, award synchronization |
| `src/test`, frontend `test` | Deterministic backend and frontend tests |

## Data flow and invariants

React calls Spring Boot through `BackendAPI.js`. `EmberClient` obtains live vehicles
as protobuf and locations/trip details as JSON. `EmberRouteMapper` converts route
events, and `EmberService` selects departures. Ember owns live vehicle data and
canonical stop names; H2 owns guest passports and recorded awards.

The browser saves the active ride and pending awards in local storage. `ride.js`
advances only through observed route events, including intermediate stops between
observations. `awardSync.js` uploads stop batches, journey completions, and town
visits, checkpointing each acknowledged write. Stable operation IDs allow retries
without duplicate awards. Do not regenerate those IDs when retrying.

Passport mutations lock the owning user. Stamp catalogue creation uses a separate
transaction and a process-local lock, matching this application's single-process
H2 design. External lookups occur outside passport write transactions.

The passport response separates bus stop `stamps` from deduplicated `cities`.
Only bus stop stamps contribute to the stamp total and level progress. Cities and
towns open in their own passport pages; existing town records remain readable.
City cards include `visitCount`; bus cards use `buses` entries with `routeNumber`
and `rideCount`. The existing `busNumbers` list and distinct cover totals remain
available. Each saved ride counts once for each city it visits and once for its
bus number. City counting groups stop and town awards by their stable journey
prefix, so multiple stops, airport/city aliases, and retries do not inflate it.
Standalone visit operations count separately. Legacy visits without operation
IDs remain counted individually because their ride identity cannot be recovered.

`CityNames` normalizes upstream region labels when producing the city collection,
including existing passports. Raw stamp metadata is retained so future mappings
can correct past visits too. Stop names are never a fallback for city names. Edinburgh
Airport maps to Edinburgh; Aberdeen Airport maps to Aberdeen; Glasgow Airport
maps to Paisley. Unmapped facility labels (airports, terminals, services, park and
ride sites, etc.) are excluded from city cards while retaining their stop stamps.
Add an explicit, verified alias there when another facility needs a city mapping;
do not infer cities by stripping arbitrary words from stop names. Ordinary region
names still come from Ember, so this is not a geographic gazetteer.
The airport mappings follow [Ember's region labels](https://www.ember.to/routes/glasgow-to-edinburgh-airport/),
[Aberdeen Airport's address](https://www.aberdeenairport.com/help/terms-and-conditions/product-and-services-terms-and-conditions/),
and [Glasgow Airport's address](https://www.glasgowairport.com/terms-and-conditions/product-and-services-terms-and-conditions/).

Statistics count distinct bus numbers and visited towns. Distance is the estimated
great-circle distance between boarding and alighting locations, not road distance.
If any recorded journey has unknown distance, the total remains unavailable.

## API and configuration

`GET /api/vehicles/live` accepts optional `origin`, `destination`, `trackedTripUid`,
and `detailsTripUid`. Blank search fields return upcoming departures. Full trip
details are requested for selected/tracked trips; boarding requires a loaded route.

`POST /api/users?name=Guest` creates a guest credential. Private user, passport, and
journey requests send it as `Authorization: Bearer <token>`. The backend retains
only its hash. Existing `/api/journeys`, `/api/stops`, and `/api/ember` endpoints
remain available even where the current frontend does not use them.

Backend defaults live in `application.properties` and can be overridden using
Spring configuration, including `EMBER_API_BASE_URL` and `EMBER_API_REQUEST_TIMEOUT`.
The frontend uses `VITE_API_BASE_URL` (default `http://localhost:8080`) at build time.
CORS permits localhost and 127.0.0.1 on ports 5173 and 3000.
There is no deployment configuration in this repository. The frontend build creates
`Frontend/go-ember-frontend/dist`; Maven creates an executable `*-exec.jar`.

## Verify changes

Run from the repository root:

```powershell
.\mvnw.cmd verify
npm.cmd test
npm.cmd run lint
npm.cmd run build
```

Backend tests exercise HTTP contracts, H2 transactions, concurrency, authorization,
departure selection, and external failure mapping with mocked upstream responses.
Frontend tests use Node's test runner and React server rendering to check ride
rules, queue recovery, API requests, and rendered states. No standalone TypeScript
check or browser interaction suite is configured. Tests do not require the live
Ember API. A real live ride still needs manual verification against the service.
