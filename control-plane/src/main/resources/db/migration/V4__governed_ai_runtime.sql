create table model_invocation (
    id uuid primary key,
    workflow_id uuid not null references workflow_run(id) on delete cascade,
    node_key varchar(120) not null,
    agent_role varchar(60) not null,
    provider varchar(80) not null,
    model_name varchar(160) not null,
    prompt_hash varchar(64) not null,
    response_hash varchar(64),
    status varchar(30) not null,
    refusal_reason text,
    failure_reason text,
    latency_ms bigint not null,
    input_tokens integer,
    output_tokens integer,
    created_at timestamptz not null
);

create index idx_model_invocation_workflow_created
    on model_invocation(workflow_id, created_at);
create index idx_model_invocation_node
    on model_invocation(workflow_id, node_key, created_at);
