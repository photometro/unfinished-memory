create table if not exists vaults (
    vault_id uuid primary key,
    created_at timestamptz not null default now()
);

create table if not exists devices (
    device_id uuid primary key,
    vault_id uuid not null references vaults(vault_id),
    public_key text,
    created_at timestamptz not null default now(),
    revoked_at timestamptz
);

create table if not exists device_tokens (
    token_id uuid primary key,
    device_id uuid not null references devices(device_id),
    token_hash text not null unique,
    created_at timestamptz not null default now(),
    expires_at timestamptz,
    revoked_at timestamptz
);

create index if not exists idx_devices_vault_id on devices(vault_id);
create index if not exists idx_device_tokens_device_id on device_tokens(device_id);
