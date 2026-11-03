CREATE TABLE failure_injection (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL REFERENCES workflow_run(id) ON DELETE CASCADE,
    node_key VARCHAR(120) NOT NULL,
    failure_category VARCHAR(40) NOT NULL,
    remaining_failures INTEGER NOT NULL CHECK (remaining_failures >= 0),
    side_effects_possible BOOLEAN NOT NULL DEFAULT FALSE,
    message TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(160) NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_failure_injection_active
    ON failure_injection(workflow_id, node_key, active, created_at);
