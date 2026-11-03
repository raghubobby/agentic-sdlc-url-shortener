create table workflow_run (
    id uuid primary key,
    scenario varchar(100) not null,
    requirement text not null,
    status varchar(40) not null,
    lock_version bigint not null default 0,
    started_at timestamptz,
    completed_at timestamptz
);

create table workflow_node_execution (
    id uuid primary key,
    workflow_id uuid not null references workflow_run(id) on delete cascade,
    node_key varchar(120) not null,
    status varchar(40) not null,
    requires_approval boolean not null,
    attempt integer not null default 0,
    lock_version bigint not null default 0,
    started_at timestamptz,
    completed_at timestamptz,
    constraint uk_workflow_node unique (workflow_id, node_key)
);

create table approval_request (
    id uuid primary key,
    workflow_id uuid not null references workflow_run(id) on delete cascade,
    node_key varchar(120) not null,
    status varchar(30) not null,
    requested_at timestamptz not null,
    decided_at timestamptz,
    decided_by varchar(200),
    comment text,
    lock_version bigint not null default 0,
    constraint uk_workflow_approval_node unique (workflow_id, node_key)
);

create table audit_event (
    event_seq bigint generated always as identity primary key,
    workflow_id uuid not null references workflow_run(id) on delete cascade,
    node_key varchar(120),
    event_type varchar(60) not null,
    payload text not null,
    created_at timestamptz not null
);

create index idx_node_workflow_status on workflow_node_execution(workflow_id, status);
create index idx_audit_workflow_sequence on audit_event(workflow_id, event_seq);
