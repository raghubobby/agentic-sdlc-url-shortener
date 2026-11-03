alter table workflow_run
    add column base_commit varchar(80);

update workflow_run
set base_commit = 'legacy-unpinned'
where base_commit is null;

alter table workflow_run
    alter column base_commit set not null;

create table tool_execution (
    id uuid primary key,
    workflow_id uuid not null references workflow_run(id) on delete cascade,
    node_key varchar(120) not null,
    agent_role varchar(60) not null,
    tool_type varchar(60) not null,
    required_capability varchar(80) not null,
    status varchar(30) not null,
    input_hash varchar(64) not null,
    output_hash varchar(64),
    workspace_path varchar(1200) not null,
    exit_code integer,
    summary text,
    latency_ms bigint,
    started_at timestamptz not null,
    completed_at timestamptz
);

create index idx_tool_execution_workflow_started
    on tool_execution(workflow_id, started_at);
create index idx_tool_execution_node_started
    on tool_execution(workflow_id, node_key, started_at);
