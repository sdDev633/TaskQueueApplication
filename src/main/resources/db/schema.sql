-- Task Table
CREATE TABLE IF NOT EXISTS tasks (
    id SERIAL PRIMARY KEY,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Outbox Events
CREATE TABLE IF NOT EXISTS outbox_event (
    id SERIAL PRIMARY KEY,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',  -- NEW, SENT
    created_at TIMESTAMP DEFAULT NOW()
);

-- Index for Faster Outbox Polling
CREATE INDEX IF NOT EXISTS idx_outbox_status
    ON outbox_event(status);

-- Index for Task status queries
CREATE INDEX IF NOT EXISTS idx_task_status
    ON tasks(status);
