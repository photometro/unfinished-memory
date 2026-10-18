create table if not exists sync_vault_locks (
    vault_id uuid primary key references vaults(vault_id) on delete cascade
);

insert into sync_vault_locks(vault_id)
select vault_id from vaults
on conflict (vault_id) do nothing;

create table if not exists sync_operation_receipts (
    vault_id uuid not null references vaults(vault_id) on delete cascade,
    operation_id uuid not null,
    request_hash text not null,
    outcome text not null,
    change_sequence bigint,
    created_at timestamptz not null default now(),
    primary key (vault_id, operation_id)
);

create table if not exists sync_changes (
    change_sequence bigserial primary key,
    vault_id uuid not null references vaults(vault_id) on delete cascade,
    operation_id uuid not null,
    record_id uuid not null,
    entity_type text not null,
    entity_id text not null,
    version bigint not null,
    ciphertext text not null,
    content_hash text not null,
    deleted_at timestamptz,
    created_at timestamptz not null default now(),
    unique (vault_id, operation_id)
);

create index if not exists idx_sync_changes_vault_sequence
    on sync_changes(vault_id, change_sequence);

create index if not exists idx_sync_receipts_vault_created
    on sync_operation_receipts(vault_id, created_at);
