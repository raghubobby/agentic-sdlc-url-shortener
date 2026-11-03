alter table workflow_run
    add column plan_revision integer not null default 1;

alter table workflow_node_execution
    add column generation integer not null default 1,
    add column validated_revision integer not null default 0;

update workflow_node_execution n
set validated_revision = 1
where n.status = 'SUCCEEDED';

alter table approval_request
    add column node_generation integer not null default 1;

alter table approval_request
    drop constraint uk_workflow_approval_node;

alter table approval_request
    add constraint uk_workflow_approval_generation unique (workflow_id, node_key, node_generation);

create table replan_record (
    id uuid primary key,
    workflow_id uuid not null references workflow_run(id) on delete cascade,
    change_id varchar(160) not null,
    from_revision integer not null,
    to_revision integer not null,
    source_node_key varchar(120) not null,
    previous_hash varchar(64) not null,
    new_hash varchar(64) not null,
    applied boolean not null,
    lineage_impacted_nodes text not null,
    invalidated_nodes text not null,
    preserved_nodes text not null,
    next_ready_nodes text not null,
    reason text not null,
    changed_by varchar(160) not null,
    created_at timestamptz not null,
    constraint uk_replan_change_id unique (workflow_id, change_id),
    constraint chk_replan_revision_order check (to_revision >= from_revision)
);

create index idx_replan_workflow_created on replan_record(workflow_id, created_at);
create index idx_approval_workflow_generation on approval_request(workflow_id, node_key, node_generation);
