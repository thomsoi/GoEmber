Run the Spring Boot backend from this directory:

```powershell
.\mvnw.cmd spring-boot:run
```

Run the frontend in another terminal:

```powershell
npm.cmd run dev
```

Restart the backend after backend code changes; the running Java process does not
automatically reload them. Stop it with Ctrl+C in its terminal and run the command
again. H2 stores data in memory, so restarting clears guest passports and journeys.

`GET /api/vehicles/live` returns upcoming departures as `application/json` without
requiring search parameters. `origin` and `destination` are optional filters.
