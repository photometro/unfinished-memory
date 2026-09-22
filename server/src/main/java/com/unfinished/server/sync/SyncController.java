package com.unfinished.server.sync;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final JdbcTemplate jdbcTemplate;
    private final DeviceAuthService authService;

    public SyncController(JdbcTemplate jdbcTemplate, DeviceAuthService authService) {
        this.jdbcTemplate = jdbcTemplate;
        this.authService = authService;
    }

    @PostMapping("/push")
    public PushResponse push(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody PushRequest request
    ) {
        DeviceAuthService.DeviceIdentity identity = authService.authenticate(authorization);
        int accepted = 0;
        for (EncryptedRecord record : request.records()) {
            accepted += jdbcTemplate.update(
                    "insert into encrypted_records(record_id, vault_id, entity_type, entity_id, version, ciphertext, content_hash, deleted_at) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?) "
                            + "on conflict (vault_id, entity_type, entity_id) do update set "
                            + "record_id = excluded.record_id, version = excluded.version, ciphertext = excluded.ciphertext, "
                            + "content_hash = excluded.content_hash, deleted_at = excluded.deleted_at, updated_at = now() "
                            + "where encrypted_records.version <= excluded.version",
                    UUID.fromString(record.recordId()), identity.vaultId(), record.entityType(), record.entityId(),
                    record.version(), record.ciphertext(), record.contentHash(),
                    record.deletedAt() == null ? null : Instant.ofEpochMilli(record.deletedAt())
            );
        }
        return new PushResponse(accepted, Instant.now().toEpochMilli());
    }

    @GetMapping("/pull")
    public PullResponse pull(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(defaultValue = "0") long cursor,
            @RequestParam(defaultValue = "200") int limit
    ) {
        DeviceAuthService.DeviceIdentity identity = authService.authenticate(authorization);
        int safeLimit = Math.min(Math.max(limit, 1), 500);
        List<EncryptedRecord> records = jdbcTemplate.query(
                "select record_id, entity_type, entity_id, version, ciphertext, content_hash, deleted_at "
                        + "from encrypted_records where vault_id = ? and extract(epoch from updated_at) * 1000 > ? "
                        + "order by updated_at, record_id limit ?",
                ps -> {
                    ps.setObject(1, identity.vaultId());
                    ps.setLong(2, cursor);
                    ps.setInt(3, safeLimit);
                },
                (rs, rowNum) -> new EncryptedRecord(
                        rs.getObject("record_id", UUID.class).toString(),
                        rs.getString("entity_type"), rs.getString("entity_id"), rs.getLong("version"),
                        rs.getString("ciphertext"), rs.getString("content_hash"),
                        rs.getTimestamp("deleted_at") == null ? null : rs.getTimestamp("deleted_at").getTime()
                )
        );
        long nextCursor = Instant.now().toEpochMilli();
        jdbcTemplate.update(
                "insert into sync_cursors(vault_id, device_id, cursor_value, updated_at) values (?, ?, ?, now()) "
                        + "on conflict (vault_id, device_id) do update set cursor_value = excluded.cursor_value, updated_at = now()",
                identity.vaultId(), identity.deviceId(), nextCursor
        );
        return new PullResponse(records, nextCursor, records.size() == safeLimit);
    }

    public record PushRequest(@NotNull List<@Valid EncryptedRecord> records) {
    }

    public record EncryptedRecord(
            @NotBlank String recordId,
            @NotBlank String entityType,
            @NotBlank String entityId,
            long version,
            @NotBlank String ciphertext,
            @NotBlank String contentHash,
            Long deletedAt
    ) {
    }

    public record PushResponse(int accepted, long cursor) {
    }

    public record PullResponse(List<EncryptedRecord> records, long nextCursor, boolean hasMore) {
    }
}
