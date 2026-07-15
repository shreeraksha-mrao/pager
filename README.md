# Personal Productivity Pager

A self-hosted productivity system that runs on your laptop, tracks time spent
on configured tasks (DSA, FDE, AI Exploration by default), and uses a
**debt-driven scheduler** to randomly "page" you via Telegram outside working
hours (11 AM–5 PM) — nudging you toward whichever task is furthest behind its
daily target. Every page, response, and completed session is persisted in
SQLite and surfaced through a React analytics dashboard.

See `docs/architecture.md` (or the session plan this was built from) for the
full design: database schema, debt model, scheduler logic, Telegram flow, and
REST API surface.

## Stack

- **Backend**: Java 21, Spring Boot 3.5.16, Spring Data JPA, Spring Scheduler,
  SQLite (via `sqlite-jdbc` + Hibernate community dialect), Maven.
- **Frontend**: React 19, Vite, Tailwind CSS 4, React Router, TanStack Query,
  Recharts, Axios.
- **Telegram**: raw Bot API calls (`RestClient`) with short-interval polling —
  no public webhook/tunnel required.

## Prerequisites

- **JDK 21** — this project was developed against a portable Microsoft
  OpenJDK 21 build (not a system-wide install). Every shell you use to build
  or run the backend must set:
  ```powershell
  $env:JAVA_HOME = "C:\path\to\jdk-21"
  $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
  ```
  Verify with `java -version` (must report 21.x) before running Maven.
- **Node.js 18+** and npm for the frontend.
- A **Telegram bot token** from [@BotFather](https://t.me/BotFather) (optional
  for local API/dashboard testing, required for real paging).

## Backend setup & run

```powershell
cd backend
$env:JAVA_HOME = "C:\path\to\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:PAGER_TELEGRAM_BOT_TOKEN = "<your-bot-token>"   # optional, enables real paging
mvn spring-boot:run
```

The backend starts on `http://localhost:8080`, creates `backend/data/pager.db`
(SQLite) on first run, applies `schema.sql`, and seeds the three default
tasks from `src/main/resources/seed/default-tasks.json` if the `tasks` table
is empty.

To register your phone for pages: message `/start` to your bot once the
backend is running with a valid token — the chat id is then stored and used
for all future pages.

### Manually triggering a page (dev profile)

Waiting for a real scheduler tick outside working hours (11 AM–5 PM) is slow
to test against. Run the backend with the `dev` Spring profile active to
unlock a debug endpoint that triggers a page immediately through the exact
same `PageEventService`/Telegram code path the scheduler uses:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

```powershell
# Auto-select a task using the normal debt-weighted selection logic:
Invoke-WebRequest -Uri "http://localhost:8080/api/debug/trigger-page" -Method Post -ContentType "application/json"

# Or force a specific task:
Invoke-WebRequest -Uri "http://localhost:8080/api/debug/trigger-page" -Method Post -ContentType "application/json" -Body '{"taskId": 2}'
```

This endpoint is only registered when the `dev` profile is active — it is
absent in a normal/default run.

### Key configuration (`backend/src/main/resources/application.yml`)

All scheduler/Telegram tunables (working hours, night-window probabilities,
cooldowns, page duration, daily minimum target, etc.) live under
`pager.scheduler.*` and `pager.telegram.*`, and are also editable at runtime
via `GET/PUT /api/settings` (in-memory only — resets to these defaults on
restart).

## Frontend setup & run

```powershell
cd frontend
npm install
npm run dev
```

Opens on `http://localhost:5173`; API calls to `/api/**` are proxied to the
backend (`vite.config.js`). Requires the backend to be running.

## Verifying the setup

- `GET http://localhost:8080/api/tasks` should return the three seeded tasks.
- `GET http://localhost:8080/api/analytics/summary` should return a full
  analytics payload (zeros initially).
- Visiting `http://localhost:5173` should show the Dashboard with live data
  from the backend.

## Manually logging completed work

If you decline a page (or miss one) but then finish the task on your own
before the next page arrives, use **Log Work** (available on both the
Dashboard and Tasks page) to record it:

- `POST /api/sessions/manual` — body: `{ "taskId": 1, "durationMinutes": 45, "notes": "optional", "sessionDate": "2026-07-13" (optional, defaults to today) }`
- Creates a `StudySession` with `source: MANUAL` (vs `PAGE_ACCEPTED` for
  page-driven sessions) and `status: COMPLETED` immediately.
- Counts toward today's completed minutes exactly like an accepted page —
  debt (`/api/analytics/debt`), today/weekly/lifetime hours, and streaks all
  update immediately, since they sum all `COMPLETED` sessions regardless of
  source.
- `GET /api/analytics/session-sources` (and the `minutesFromPages` /
  `minutesFromManual` / `totalProductiveMinutes` fields on
  `/api/analytics/summary`) break down lifetime productive minutes by how
  they were recorded.

## Known limitations

- Settings changes via `/api/settings` are **not persisted** — they mutate
  the live `SchedulerProperties` bean in memory and revert to
  `application.yml` defaults on restart. Persisting them to `app_settings`
  is a natural next step.
- Telegram polling/round-trip (accept/decline buttons, decline-reason
  capture, follow-up confirmations) requires a real bot token and a Telegram
  account to fully exercise; it has not been tested against a live bot in
  this environment.
- Flyway has no published SQLite artifact, so schema management uses Spring
  Boot's built-in `schema.sql` initializer instead (idempotent
  `CREATE TABLE IF NOT EXISTS` statements) rather than versioned migrations.
