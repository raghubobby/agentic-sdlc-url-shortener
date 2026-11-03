alter table workflow_node_execution
    add column risk_level varchar(30),
    add column failure_category varchar(40),
    add column last_error text,
    add column compensation_status varchar(30) not null default 'NONE';

alter table approval_request
    drop column decided_at,
    drop column decided_by,
    drop column comment,
    add column risk_level varchar(30),
    add column required_approvals integer,
    add column reason text,
    add column resolved_at timestamptz;

update approval_request
set risk_level = 'HIGH',
    required_approvals = 1,
    reason = 'Migrated approval request'
where risk_level is null;

alter table approval_request
    alter column risk_level set not null,
    alter column required_approvals set not null,
    alter column reason set not null;

create table approval_decision (
    id uuid primary key,
    approval_request_id uuid not null references approval_request(id) on delete cascade,
    actor varchar(200) not null,
    decision varchar(20) not null,
    comment text,
    decided_at timestamptz not null,
    constraint uk_approval_actor unique (approval_request_id, actor)
);

create table policy_evaluation (
    id uuid primary key,
    workflow_id uuid not null references workflow_run(id) on delete cascade,
    node_key varchar(120) not null,
    risk_level varchar(30) not null,
    risk_score integer not null,
    decision varchar(40) not null,
    required_approvals integer not null,
    reason text not null,
    risk_reasons text not null,
    created_at timestamptz not null
);

create table artifact (
    id uuid primary key,
    workflow_id uuid not null references workflow_run(id) on delete cascade,
    node_key varchar(120) not null,
    type varchar(50) not null,
    version integer not null,
    status varchar(20) not null,
    content_hash varchar(64) not null,
    content text not null,
    created_by varchar(120) not null,
    created_at timestamptz not null,
    constraint uk_artifact_node_version unique (workflow_id, node_key, version)
);

create table artifact_dependency (
    id uuid primary key,
    artifact_id uuid not null references artifact(id) on delete cascade,
    depends_on_artifact_id uuid not null references artifact(id) on delete restrict,
    input_hash varchar(64) not null,
    constraint uk_artifact_dependency unique (artifact_id, depends_on_artifact_id),
    constraint chk_artifact_not_self_dependency check (artifact_id <> depends_on_artifact_id)
);

create index idx_approval_decision_request on approval_decision(approval_request_id);
create index idx_policy_workflow_node on policy_evaluation(workflow_id, node_key, created_at);
create index idx_artifact_workflow_node on artifact(workflow_id, node_key, version desc);
create index idx_artifact_dependency_artifact on artifact_dependency(artifact_id);
