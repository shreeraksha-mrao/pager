# Personal Productivity Pager — Architecture & Implementation Plan

## 1. Problem Statement
Build a self-hosted productivity system that runs on the user's laptop, tracks time
spent on configured tasks (DSA, FDE, AI Exploration, more later), and uses a
**debt-driven scheduler** to randomly "page" the user via Telegram outside core
working hours (11 AM–5 PM) to nudge them to work on whichever task is furthest
behind its daily target. Every page, response, and completed session is persisted
in SQLite and surfaced through a React analytics dashboard.

Stack: Java 21, Spring Boot 3.x, Spring Scheduler, Spring Data JPA, SQLite, Maven,
React + Vite + Tailwind CSS, Telegram Bot API (long polling).

## 2. High-Level Architecture

```
┌─────────────────────────┐        REST/JSON        ┌───────────────────────┐
│  React SPA (Vite/Tailwind)│ <────────────────────> │  Spring Boot Backend   │
│  Dashboard / Tasks / History│                       │  (REST + Scheduler)    │
└─────────────────────────┘                          │                       │
                                                       │  ┌─────────────────┐ │
                                                       │  │ Debt Scheduler   │ │
                                                       │  │ (tick job)       │ │
                                                       │  └────────┬─────────┘ │
                                                       │           │ triggers  │
                                                       │  ┌────────▼─────────┐ │
                                                       │  │ Telegram Service │ │
                                                       │  │ (long polling)   │ │<──> Telegram Bot API <──> User's phone
                                                       │  └────────┬─────────┘ │
                                                       │           │           │
                                                       │  ┌────────▼─────────┐ │
                                                       │  │ SQLite (JPA)     │ │
                                                       │  └──────────────────┘ │
                                                       └───────────────────────┘
```

Both apps run locally on the laptop: backend on e.g. `localhost:8080`, frontend
dev server on `localhost:5173` (proxied to backend), or built and served as
static files by the backend in "production" mode.

## 3. Backend Package Structure

```
com.pager
├─ config/            SchedulerConfig, TelegramConfig, CorsConfig, JacksonConfig
├─ entity/            Task, PageEvent, StudySession, AppSetting, BotState
├─ repository/         Spring Data JPA repositories
├─ dto/                Request/response records (TaskDto, PageEventDto, AnalyticsSummaryDto, ...)
├─ service/
│   ├─ TaskService              CRUD + reordering
│   ├─ DebtCalculatorService     computes daily/rolling debt per task
│   ├─ PageDeciderService        tick logic: probability + task selection
│   ├─ PageEventService          lifecycle of a page (create/send/respond/expire)
│   ├─ StudySessionService       session lifecycle (in-progress/completed/abandoned)
│   ├─ AnalyticsService          all dashboard aggregates
│   ├─ TelegramMessagingService  formats & sends messages/edits via Bot API
│   ├─ TelegramPollingService    long-poll loop (getUpdates), dispatches callbacks
│   └─ SeedDataService           ApplicationRunner: loads default-tasks.json if tasks table empty
├─ scheduler/          @Scheduled jobs: PagingTickJob, FollowUpCheckJob, MissedPageExpiryJob
├─ controller/         TaskController, PageController, SessionController, AnalyticsController, SettingsController
└─ exception/          GlobalExceptionHandler, ApiError
```

Schema managed via Spring Boot's built-in SQL initializer
(`spring.sql.init.mode=always` + `classpath:schema.sql`), since no official
Flyway module for SQLite is published on Maven Central; this gives the same
explicit, versionable DDL approach Flyway would have provided.

## 4. Database Schema (SQLite via Flyway)

### `tasks`
| column | type | notes |
|---|---|---|
| id | INTEGER PK | |
| name | TEXT UNIQUE | |
| description | TEXT | |
| daily_target_minutes | INTEGER | e.g. 60 |
| priority_weight | REAL DEFAULT 1.0 | manual boost/demote in scheduler weighting |
| color | TEXT | hex, for charts |
| icon | TEXT | icon key/emoji |
| active | BOOLEAN DEFAULT true | soft-disable instead of delete |
| sort_order | INTEGER | dashboard/reorder display |
| created_at / updated_at | TIMESTAMP | |

### `task_scheduler_state` (1:1 with tasks — scheduler bookkeeping)
| column | type | notes |
|---|---|---|
| task_id | INTEGER PK/FK | |
| next_eligible_at | TIMESTAMP | cooldown gate after decline/miss/send |
| last_paged_at | TIMESTAMP | |
| consecutive_declines | INTEGER | escalates urgency/cooldown shortening |

### `task_debt_ledger` (persistent, one row per task per calendar day)
| column | type | notes |
|---|---|---|
| id | INTEGER PK | |
| task_id | FK → tasks | |
| ledger_date | DATE | finalized calendar day |
| target_minutes | INTEGER | that day's target (captured, in case target changes later) |
| completed_minutes | INTEGER | confirmed-completed minutes that day |
| shortfall_minutes | INTEGER | max(0, target − completed) that day |
| cumulative_debt_minutes | INTEGER | running balance: previous cumulative + shortfall − any excess-over-target credit |
| created_at | TIMESTAMP | |

This is the durable debt ledger: **debt persists across days**. A nightly
rollover job finalizes the previous day per task, appends a ledger row, and
carries the resulting `cumulative_debt_minutes` forward. Working extra beyond
a day's target reduces the carried balance (auto "repayment"); missing a
target increases it. `days_behind_target` and debt-trend charts are derived
directly from this table.

### `page_events`
| column | type | notes |
|---|---|---|
| id | INTEGER PK | |
| task_id | FK → tasks | |
| status | TEXT | PENDING, ACCEPTED, DECLINED, MISSED, EXPIRED, CANCELLED |
| duration_minutes | INTEGER | fixed 60 (configurable per settings) |
| debt_snapshot_minutes | INTEGER | task debt at moment of paging (for analytics) |
| sent_at | TIMESTAMP | |
| responded_at | TIMESTAMP | nullable |
| decline_reason | TEXT | nullable |
| decline_reason_type | TEXT | PRESET / FREE_TEXT, nullable |
| telegram_message_id | TEXT | to edit message after response |
| telegram_chat_id | TEXT | |
| replacement_of_event_id | INTEGER FK self | links a re-page to the declined/missed one it replaces |
| created_at | TIMESTAMP | |

### `study_sessions`
| column | type | notes |
|---|---|---|
| id | INTEGER PK | |
| task_id | FK → tasks | |
| page_event_id | FK → page_events, nullable | nullable to allow future manual logging |
| status | TEXT | IN_PROGRESS, AWAITING_CONFIRMATION, COMPLETED, ABANDONED |
| start_time | TIMESTAMP | |
| end_time | TIMESTAMP | planned/actual end |
| duration_minutes | INTEGER | credited minutes — only set/counted once COMPLETED |
| confirmation_requested_at | TIMESTAMP | when the first check-in ping was sent |
| confirmed_at | TIMESTAMP | when the user explicitly confirmed completion/abandonment |
| session_date | DATE | for fast daily/weekly aggregation (indexed) |
| created_at | TIMESTAMP | |

**No auto-completion**: a session is only ever credited toward debt/analytics
when the user explicitly taps "Completed". If planned `end_time` passes with
no response, the session moves to `AWAITING_CONFIRMATION` (not COMPLETED) and
the bot keeps sending reminder check-ins (capped backoff, e.g. every 20–30 min
up to a max number of reminders) until the user confirms Completed/Extend/
Abandoned.

### `app_settings` (key/value)
Holds: registered `telegram_chat_id`, working-hours window, quiet-window
probability %, default page duration, missed-page timeout, replacement cooldown
ranges, decline-reason presets list (JSON), follow-up check-in delay.

### `bot_state` (key/value)
Holds Telegram `getUpdates` offset and any transient "awaiting free-text decline
reason for chat X / event Y" conversation state.

### Seed file: `src/main/resources/seed/default-tasks.json`
```json
[
  {"name": "DSA", "description": "Data Structures & Algorithms practice", "dailyTargetMinutes": 60, "priorityWeight": 1.0, "color": "#3B82F6", "icon": "🧮"},
  {"name": "FDE", "description": "Forward Deployed Engineering skill-building", "dailyTargetMinutes": 60, "priorityWeight": 1.0, "color": "#10B981", "icon": "🛠️"},
  {"name": "AI Exploration", "description": "Exploring AI tools, papers, projects", "dailyTargetMinutes": 60, "priorityWeight": 1.0, "color": "#8B5CF6", "icon": "🤖"}
]
```
Loaded by `SeedDataService` (an `ApplicationRunner`) only when the `tasks` table
is empty. All later mutation happens via REST/DB — the file is never re-read
after first boot.

## 5. Debt Model & Scheduler Logic

**Primary objective**: the scheduler's core goal is to ensure at least **3–4
hours of confirmed, completed** productive work across active tasks each day
— not to hit a fixed page count. Page frequency and urgency rise as the day
progresses with that minimum still unmet. Independently of the daily minimum,
**debt persists across days**: any shortfall against a task's daily target
carries forward as cumulative debt in `task_debt_ledger`, and the scheduler
becomes progressively more likely to assign tasks with large outstanding debt
until it is repaid (via extra completed work above target on later days).

### Debt calculation (`DebtCalculatorService`)
- `todayCompletedMinutes(task)` = SUM(study_sessions.duration_minutes) WHERE
  task_id=? AND session_date = today AND status = COMPLETED (only confirmed
  sessions count — never AWAITING_CONFIRMATION or IN_PROGRESS).
- `todayShortfall(task)` = max(0, daily_target_minutes − todayCompletedMinutes(task)).
- `cumulativeDebt(task)` = latest `task_debt_ledger.cumulative_debt_minutes`
  for that task (frozen as of the last nightly rollover — i.e., prior days'
  unresolved debt).
- `effectiveDebt(task)` = `cumulativeDebt(task) + todayShortfall(task)` — this
  is the persistent, day-crossing debt figure used both for scheduling weight
  and for the "Productivity Debt" / debt-dashboard analytics.
- `taskWeight(task)` = `(effectiveDebt(task)^1.3 + floorWeight) * priority_weight`.
  The small `floorWeight` ensures zero-debt tasks are never fully excluded —
  "may continue assigning additional sessions beyond the daily minimum ...
  based on remaining debt, streak goals, or random motivation boosts."

### Nightly debt rollover job (`DebtRolloverJob`, runs once at day boundary)
For each active task, finalize the day just ended:
1. `completed = todayCompletedMinutes(task)` for that finished day.
2. `shortfall = max(0, target − completed)`.
3. `excessCredit = max(0, completed − target)` (working beyond target repays debt).
4. `newCumulative = max(0, previousCumulative + shortfall − excessCredit)`.
5. Insert a `task_debt_ledger` row `{ledger_date, target_minutes, completed_minutes, shortfall_minutes, cumulative_debt_minutes: newCumulative}`.
This is what makes debt durable: a missed day's shortfall keeps elevating
`taskWeight` on every subsequent day until enough extra completed work repays it.

### Paging tick job (`PagingTickJob`, `@Scheduled(fixedRate)`, every ~5 min)
1. Skip entirely if current time is inside working hours (11:00–17:00).
2. Determine time-bucket multiplier (configurable in `app_settings`, defaults below):
   - 01:00–07:00: **30%** relative probability — night pages remain possible,
     just less frequent.
   - 07:00–11:00: **75%** relative probability.
   - 17:00–01:00: **100%** (normal/full probability).
3. Compute `urgencyMultiplier` combining (a) how much of the 3–4h daily minimum
   remains unmet as the day window narrows, and (b) total `effectiveDebt`
   across active tasks — both rising urgency as the day progresses with debt
   unresolved. Capped so a single tick never exceeds ~60% trigger probability.
4. `tickProbability = baseProbability * timeBucketMultiplier * urgencyMultiplier`.
5. Roll the dice. If triggered:
   - Filter tasks: active, `next_eligible_at <= now`, no existing PENDING page.
   - Weighted-random pick using `taskWeight` (§ above, driven by `effectiveDebt`).
   - Create `page_events` row (status PENDING, duration = configured default,
     debt_snapshot_minutes = current `effectiveDebt`), send Telegram message.
6. Enforce a global minimum gap between any two sent pages (e.g., 15 min) to
   avoid notification bursts even if multiple tasks are eligible.

### On Decline / Miss (cooldown + replacement)
- Compute cooldown = base (e.g., 45–90 min), shortened as
  `consecutive_declines` or debt rises (never below a floor, e.g., 15 min).
- Set `task_scheduler_state.next_eligible_at = now + cooldown`.
- Debt is **not** reduced — "declining does not remove the obligation." A
  replacement page for the same task will naturally be considered on a future
  tick once eligible again (higher debt → higher weight → likely re-selected).

### On Accept → Explicit-confirmation completion check (`FollowUpCheckJob`)
- Accept creates a `study_sessions` row (IN_PROGRESS, start=now,
  end=now+duration) and marks the page ACCEPTED — this is what "Acceptance
  rate" measures.
- A job fires at `end_time` and sends a check-in message:
  ✅ Completed / 🔁 Extend 30m / ✋ Abandoned, and flips the session to
  `AWAITING_CONFIRMATION`.
  - **No automatic completion, ever.** Time is credited toward debt/analytics
    only when the user explicitly taps Completed.
  - If there's no response, the session simply stays `AWAITING_CONFIRMATION`
    (not COMPLETED, not counted toward debt) and the job re-sends a reminder
    check-in on a backoff schedule (e.g., every 20–30 min, up to a configurable
    max number of reminders) until the user responds.
  - Completed → session COMPLETED, `duration_minutes` credited, debt reduced —
    this is what "Completion rate" measures (Completed / Accepted).
  - Extend → push `end_time` +30 min, status stays IN_PROGRESS, re-check later.
  - Abandoned → session ABANDONED, 0 minutes credited, debt remains.

### Missed-page expiry (`MissedPageExpiryJob`, every ~1 min)
- Any PENDING page with `sent_at` older than the configured timeout (e.g., 20
  min) → status MISSED, Telegram message edited to reflect it, debt unchanged,
  cooldown/replacement logic same as Decline (shorter cooldown, since a miss
  implies availability is unclear rather than an explicit refusal).

## 6. Telegram Integration
- **Long polling**: `TelegramPollingService` runs a scheduled short-interval
  poll (`getUpdates` with stored `offset` from `bot_state`), no public
  endpoint/webhook needed — works from behind the laptop's NAT.
- **Registration**: user sends `/start` to the bot once; handler stores the
  `chat_id` in `app_settings`. All later pages are sent to that chat id.
- **Page message**: task name, duration, current debt context, inline keyboard
  `[✅ Accept] [❌ Decline]`.
- **Decline flow**: tapping Decline replaces the keyboard with preset reasons
  (`Too busy`, `Not feeling it`, `In a meeting`, `Low energy`, `📝 Other…`).
  Preset tap stores reason immediately (`PRESET`). "Other" sets a pending
  free-text state in `bot_state` keyed by chat id; the next plain-text message
  from that chat is captured as `decline_reason` (`FREE_TEXT`).
- **Message editing**: after any response, the original Telegram message is
  edited (via `editMessageText`/`editMessageReplyMarkup`) to show the final
  state (✅/❌/⌛) instead of leaving stale buttons.

## 7. REST API Surface

**Tasks**
- `GET /api/tasks` / `GET /api/tasks/{id}`
- `POST /api/tasks`, `PUT /api/tasks/{id}`, `DELETE /api/tasks/{id}`
- `PATCH /api/tasks/{id}/status` (activate/deactivate)
- `PUT /api/tasks/reorder` (sort_order / priority_weight batch update)

**Pages**
- `GET /api/pages` (filter by status/task/date range, paginated)
- `GET /api/pages/{id}`
- `POST /api/pages/{id}/respond` (dashboard-side accept/decline parity with Telegram, optional convenience)

**Sessions**
- `GET /api/sessions` (filter by task/date range)
- `POST /api/sessions` (manual logging, future use)

**Analytics**
- `GET /api/analytics/summary` — one-shot payload for the whole dashboard
  (today/week/lifetime hours by task, streaks, page stats, rates, debt, top
  decline reasons, trend series)
- `GET /api/analytics/hours?range=today|week|lifetime`
- `GET /api/analytics/streaks`
- `GET /api/analytics/pages-stats`
- `GET /api/analytics/decline-reasons`
- `GET /api/analytics/trends?period=daily|weekly&points=8`
- `GET /api/analytics/debt` — per-task debt dashboard: current
  `effectiveDebt` minutes per task, total debt across tasks, days-behind-target
  count per task, and cumulative-debt trend series (from `task_debt_ledger`)

**Settings**
- `GET/PUT /api/settings` (working-hours window, quiet-window %, cooldowns,
  page duration, missed timeout, Telegram registration status)

## 8. Analytics Definitions
- **Streaks**: consecutive calendar days where combined **completed
  (confirmed)** minutes ≥ combined daily target across active tasks (current +
  longest, computed from `study_sessions` where status=COMPLETED).
- **Acceptance rate** = ACCEPTED+DECLINED-resolved pages accepted / all
  non-pending pages sent.
- **Completion rate** = COMPLETED sessions / ACCEPTED sessions (sessions stuck
  in AWAITING_CONFIRMATION are excluded from the numerator until confirmed).
- **Productivity debt** = current total `effectiveDebt` across tasks (§5),
  the same persistent, day-crossing figure that drives scheduler urgency.
- **Missed pages** = pages with status MISSED.
- **Top decline reasons** = grouped count of `decline_reason`
  (preset code or free-text bucket) over selectable window.
- **Task completion trends** = daily/weekly completed-minutes series per task
  for line/bar charts.
- **Debt dashboard** (new, backed by `GET /api/analytics/debt` and
  `task_debt_ledger`):
  - Debt minutes per task (`effectiveDebt`, i.e. cumulative carried-over debt
    + today's shortfall so far).
  - Total debt across all active tasks.
  - Days behind target per task (count of ledger rows with `shortfall_minutes > 0`).
  - Debt trend over time per task (line chart of `cumulative_debt_minutes`
    from `task_debt_ledger`, newest N days/weeks).

## 9. Frontend Structure (React + Vite + Tailwind)

```
pager-frontend/
├─ src/
│  ├─ api/            axios client + per-resource modules (tasks, pages, sessions, analytics, settings)
│  ├─ hooks/           React Query hooks (useTasks, useAnalyticsSummary, usePages...), polling refresh (~30s)
│  ├─ components/      StatCard, TaskHoursBarChart, StreakBadge, PageHistoryTable,
│  │                    DeclineReasonsChart, CompletionTrendChart, TaskForm, TaskList, RateGauge
│  ├─ pages/           Dashboard.jsx, Tasks.jsx, PageHistory.jsx, Settings.jsx
│  ├─ App.jsx / router (react-router)
│  └─ styles/          tailwind.css, tailwind.config.js
```
- **Dashboard.jsx** renders all requested metrics from `/api/analytics/summary`:
  today/weekly/lifetime hours by task, current & longest streak, pages
  received/accepted/declined/missed, acceptance & completion rate, productivity
  debt, top decline reasons, completion trends.
- **DebtDashboard** section/component (backed by `/api/analytics/debt`): per-task
  debt minutes, total debt, days-behind-target per task, and cumulative debt
  trend chart over time.
- Charting via `recharts`; data fetching/caching via `@tanstack/react-query`.
- Dev server proxies `/api` to the Spring Boot backend (`vite.config.js`
  `server.proxy`).

## 10. Implementation Phases (tracked as todos)
1. Scaffold backend (Maven, Spring Boot 3.x, Java 21, SQLite + Flyway deps)
2. Scaffold frontend (Vite + React + Tailwind, project skeleton)
3. Define JPA entities + Flyway migrations for full schema
4. Seed-data loader for default tasks from JSON
5. Task management REST API (CRUD, reorder, activate/deactivate)
6. Page event & study session core services (state machines, repositories)
7. Debt ledger (persistent, cross-day) + debt calculator + paging tick
   scheduler job (probability/weight logic) + nightly rollover job
8. Telegram integration: polling, sending, inline keyboards, decline reason
   capture (preset + free-text), message editing
9. Follow-up explicit-confirmation check-in job (no auto-complete, reminder
   backoff) + missed-page expiry job
10. Analytics service + REST endpoints (including debt dashboard endpoint)
11. Settings REST API (+ persisted config for scheduler tunables)
12. Frontend API client + hooks
13. Dashboard UI (all requested metrics/charts) + debt dashboard view
14. Task management UI (CRUD, reorder, enable/disable)
15. Page history & settings UI
16. End-to-end validation (simulate ticks, Telegram round-trip, debt rollover) + README setup docs

## 11. Open Configuration Defaults (tunable via `app_settings`, not hardcoded)
- Working hours (no paging): 11:00–17:00
- Night-hours relative probability: 30% from 01:00–07:00, 75% from 07:00–11:00
  (night pages remain possible, just less frequent — never fully suppressed)
- Default page duration: 60 min
- Missed-page timeout: 20 min
- Confirmation reminder backoff: every 20–30 min, capped max reminder count, while a session is AWAITING_CONFIRMATION
- Decline/miss cooldown: 45–90 min (shrinks with consecutive declines / rising debt)
- Global minimum gap between sent pages: 15 min
- Daily minimum objective: 3–4 hours of confirmed completed work per day across active tasks (drives urgency, not a fixed page count)
- Debt rollover: nightly job at day boundary finalizes `task_debt_ledger`; debt persists indefinitely until repaid via excess completed work
