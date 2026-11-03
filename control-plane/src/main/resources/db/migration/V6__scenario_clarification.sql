create table clarification_request (
    id uuid primary key,
    workflow_id uuid not null references workflow_run(id) on delete cascade,
    plan_revision integer not null,
    node_key varchar(120) not null,
    status varchar(30) not null,
    questions text not null,
    answer text,
    requested_at timestamptz not null,
    resolved_at timestamptz,
    resolved_by varchar(200),
    lock_version bigint not null default 0,
    constraint uk_clarification_revision_node unique (workflow_id, plan_revision, node_key)
);

create index idx_clarification_workflow_status
    on clarification_request(workflow_id, status, requested_at);
