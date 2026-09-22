package com.unfinished.server.sync;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/backup")
public class BackupController {

    private final JdbcTemplate jdbcTemplate;
    private final DeviceAuthService authService;

    public BackupController(JdbcTemplate jdbcTemplate, DeviceAuthService authService) {
        this.jdbcTemplate = jdbcTemplate;
        this.authService = authService;
    }

    @PostMapping
    public BackupResponse create(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody BackupRequest request
    ) {
        DeviceAuthService.DeviceIdentity identity = authService.authenticate(authorization);
        UUID snapshotId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into backup_snapshots(snapshot_id, vault_id, device_id, ciphertext, content_hash) values (?, ?, ?, ?, ?)",
                snapshotId, identity.vaultId(), identity.deviceId(), request.ciphertext(), request.contentHash()
        );
        return new BackupResponse(snapshotId.toString(), Instant.now().toEpochMilli());
    }

    @GetMapping("/{snapshotId}")
    public BackupPayload get(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID snapshotId
    ) {
        DeviceAuthService.DeviceIdentity identity = authService.authenticate(authorization);
        BackupPayload payload = jdbcTemplate.query(
                "select ciphertext, content_hash, created_at from backup_snapshots where snapshot_id = ? and vault_id = ?",
                ps -> { ps.setObject(1, snapshotId); ps.setObject(2, identity.vaultId()); },
                rs -> rs.next()
                        ? new BackupPayload(snapshotId.toString(), rs.getString("ciphertext"), rs.getString("content_hash"), rs.getTimestamp("created_at").getTime())
                        : null
        );
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Snapshot not found");
        }
        return payload;
    }

    public record BackupRequest(@NotBlank String ciphertext, @NotBlank String contentHash) {
    }

    public record BackupResponse(String snapshotId, long createdAt) {
    }

    public record BackupPayload(String snapshotId, String ciphertext, String contentHash, long createdAt) {
    }
}
