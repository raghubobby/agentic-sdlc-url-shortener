CREATE SCHEMA IF NOT EXISTS url_shortener;

CREATE TABLE url_shortener.short_url (
    id uuid primary key,
    short_code varchar(32) not null unique,
    destination_url varchar(4096) not null,
    created_at timestamp with time zone not null,
    expires_at timestamp with time zone null,
    deactivated_at timestamp with time zone null,
    idempotency_key varchar(200) null unique,
    request_hash char(64) null,
    version bigint not null default 0,
    constraint ck_short_url_code check (short_code ~ '^[A-Za-z0-9_-]{4,32}$')
);

CREATE INDEX idx_short_url_expires_at ON url_shortener.short_url(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX idx_short_url_active ON url_shortener.short_url(short_code) WHERE deactivated_at IS NULL;

CREATE TABLE url_shortener.click_event (
    id uuid primary key,
    short_url_id uuid not null references url_shortener.short_url(id),
    occurred_at timestamp with time zone not null,
    referrer_host varchar(255) null,
    user_agent_family varchar(100) null
);

CREATE INDEX idx_click_event_url_time ON url_shortener.click_event(short_url_id, occurred_at desc);
