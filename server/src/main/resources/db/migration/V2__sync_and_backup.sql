create table if not exists encrypted_records (
    record_id uuid primary key,
    vault_id uuid not null references vaults(vault_id),
    entity_type text not null,
    entity_id text not null,
    version bigint not null,
    ciphertext text not null,
    content_hash text not null,
    deleted_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (vault_id, entity_type, entity_id)
);

create index if not exists idx_encrypted_records_vault_updated
    on encrypted_records(vault_id, updated_at, record_id);

create table if not exists media_objects (
    object_id uuid primary key,
    vault_id uuid not null references vaults(vault_id),
    entity_id text not null,
    object_key text not null unique,
    sha256 text not null,
    size_bytes bigint not null default 0,
    content_type text,
    upload_state text not null default 'PENDING',
    created_at timestamptz not null default now(),
    deleted_at timestamptz
);

create table if not exists sync_cursors (
    vault_id uuid not null references vaults(vault_id),
    device_id uuid not null references devices(device_id),
    cursor_value bigint not null default 0,
    updated_at timestamptz not null default now(),
    primary key (vault_id, device_id)
);

create table if not exists backup_snapshots (
    snapshot_id uuid primary key,
    vault_id uuid not null references vaults(vault_id),
    device_id uuid not null references devices(device_id),
    ciphertext text not null,
    content_hash text not null,
    created_at timestamptz not null default now()
);

create index if not exists idx_backup_snapshots_vault_created
    on backup_snapshots(vault_id, created_at desc);
