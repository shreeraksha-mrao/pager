-- Personal Productivity Pager schema (SQLite)
-- Applied via Spring Boot's SQL initializer (schema-locations: classpath:schema.sql)

CREATE TABLE IF NOT EXISTS tasks (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    name                  TEXT NOT NULL UNIQUE,
    description           TEXT,
    daily_target_minutes  INTEGER NOT NULL DEFAULT 60,
    priority_weight       REAL NOT NULL DEFAULT 1.0,
    color                 TEXT,
    icon                  TEXT,
    active                INTEGER NOT NULL DEFAULT 1,
    sort_order            INTEGER NOT NULL DEFAULT 0,
    created_at            TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at            TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS task_scheduler_state (
    task_id                INTEGER PRIMARY KEY REFERENCES tasks(id) ON DELETE CASCADE,
    next_eligible_at        TEXT,
    last_paged_at           TEXT,
    consecutive_declines    INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS task_debt_ledger (
    id                       INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id                  INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    ledger_date              TEXT NOT NULL,
    target_minutes           INTEGER NOT NULL,
    completed_minutes        INTEGER NOT NULL,
    shortfall_minutes        INTEGER NOT NULL,
    cumulative_debt_minutes  INTEGER NOT NULL,
    created_at               TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE (task_id, ledger_date)
);
CREATE INDEX IF NOT EXISTS idx_debt_ledger_task_date ON task_debt_ledger(task_id, ledger_date);

-- Manually-triggered pauses ("disable pages"). ended_at IS NULL means the pause is
-- currently active; while active, the scheduler never pages and debt does not accrue
-- for any minute overlapping [started_at, ended_at or now).
CREATE TABLE IF NOT EXISTS pager_pause_period (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    started_at    TEXT NOT NULL,
    ended_at      TEXT,
    reason        TEXT,
    created_at    TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Snapshot of each task's effectiveDebt at the instant a pause begins. While that pause
-- stays open, debt is read from here verbatim (frozen) instead of being recomputed live.
CREATE TABLE IF NOT EXISTS pager_pause_debt_snapshot (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    pause_period_id       INTEGER NOT NULL REFERENCES pager_pause_period(id) ON DELETE CASCADE,
    task_id               INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    frozen_debt_minutes   INTEGER NOT NULL,
    UNIQUE (pause_period_id, task_id)
);

CREATE TABLE IF NOT EXISTS page_events (
    id                       INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id                  INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    status                   TEXT NOT NULL DEFAULT 'PENDING',
    duration_minutes         INTEGER NOT NULL,
    debt_snapshot_minutes    INTEGER NOT NULL DEFAULT 0,
    sent_at                  TEXT NOT NULL DEFAULT (datetime('now')),
    responded_at             TEXT,
    decline_reason           TEXT,
    decline_reason_type      TEXT,
    telegram_message_id      TEXT,
    telegram_chat_id         TEXT,
    replacement_of_event_id  INTEGER REFERENCES page_events(id),
    created_at               TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_page_events_task ON page_events(task_id);
CREATE INDEX IF NOT EXISTS idx_page_events_status ON page_events(status);
CREATE INDEX IF NOT EXISTS idx_page_events_sent_at ON page_events(sent_at);

CREATE TABLE IF NOT EXISTS study_sessions (
    id                          INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id                     INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    page_event_id               INTEGER REFERENCES page_events(id),
    status                      TEXT NOT NULL DEFAULT 'IN_PROGRESS',
    start_time                  TEXT NOT NULL,
    end_time                    TEXT NOT NULL,
    duration_minutes            INTEGER NOT NULL DEFAULT 0,
    confirmation_requested_at   TEXT,
    confirmed_at                TEXT,
    reminder_count               INTEGER NOT NULL DEFAULT 0,
    session_date                TEXT NOT NULL,
    source                      TEXT NOT NULL DEFAULT 'PAGE_ACCEPTED',
    notes                       TEXT,
    checkin_message_id          TEXT,
    checkin_chat_id             TEXT,
    created_at                  TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_sessions_task_date ON study_sessions(task_id, session_date);
CREATE INDEX IF NOT EXISTS idx_sessions_status ON study_sessions(status);

CREATE TABLE IF NOT EXISTS app_settings (
    key         TEXT PRIMARY KEY,
    value       TEXT,
    updated_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS bot_state (
    key         TEXT PRIMARY KEY,
    value       TEXT,
    updated_at  TEXT NOT NULL DEFAULT (datetime('now'))
);
