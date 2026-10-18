package com.unfinished.server.sync;

import com.unfinished.server.sync.DeviceAuthService.DeviceIdentity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
    @Transactional
    public PushResponse push(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody PushRequest request
    ) {
        DeviceIdentity identity = authService.authenticate(authorization);
        UUID vaultId = identity.vaultId();
        // Every writer to one vault obtains the same row lock before allocating change sequences.
        jdbcTemplate.update("insert into sync_vault_locks(vault_id) values (?) on conflict (vault_id) do nothing", vaultId);
        jdbcTemplate.queryForObject("select vault_id from sync_vault_locks where vault_id = ? for update", UUID.class, vaultId);

        List<Acknowledgement> acknowledgements = new ArrayList<>();
        for (EncryptedRecord record : request.records()) {
            UUID operationId = parseUuid(record.operationId(), "operationId");
            UUID recordId = parseUuid(record.recordId(), "recordId");
            validateRecord(record);
            String requestHash = requestHash(record);
            Receipt previousReceipt = findReceipt(vaultId, operationId);
            if (previousReceipt != null) {
                if (!previousReceipt.requestHash().equals(requestHash)) {
                    acknowledgements.add(new Acknowledgement(record.operationId(), "OPERATION_ID_CONFLICT", previousReceipt.changeSequence()));
                } else {
                    acknowledgements.add(new Acknowledgement(record.operationId(), previousReceipt.outcome(), previousReceipt.changeSequence()));
                }
                continue;
            }

            ExistingRecord existing = findRecord(vaultId, record.entityType(), record.entityId());
            String outcome;
            Long changeSequence = null;
            if (existing == null) {
                insertRecord(vaultId, recordId, record);
                outcome = "APPLIED";
            } else if (!existing.recordId().equals(recordId)) {
                outcome = "CONFLICT";
            } else if (record.version() < existing.version()) {
                outcome = "STALE";
            } else if (record.version() == existing.version()) {
                outcome = existing.sameSnapshot(record) ? "DUPLICATE" : "CONFLICT";
            } else if (existing.deletedAt() != null && record.deletedAt() == null) {
                outcome = "TOMBSTONE_REJECTED";
            } else {
                updateRecord(vaultId, record);
                outcome = "APPLIED";
            }

            if ("APPLIED".equals(outcome)) {
                changeSequence = appendChange(vaultId, operationId, recordId, record);
            }
            jdbcTemplate.update(
                    "insert into sync_operation_receipts(vault_id, operation_id, request_hash, outcome, change_sequence) values (?, ?, ?, ?, ?)",
                    vaultId, operationId, requestHash, outcome, changeSequence
            );
            acknowledgements.add(new Acknowledgement(record.operationId(), outcome, changeSequence));
        }
        long cursor = acknowledgements.stream().map(Acknowledgement::changeSequence)
                .filter(value -> value != null).mapToLong(Long::longValue).max().orElse(0L);
        return new PushResponse(acknowledgements, cursor);
    }

    @GetMapping("/pull")
    public PullResponse pull(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(defaultValue = "0") long cursor,
            @RequestParam(defaultValue = "200") int limit
    ) {
        if (cursor < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cursor must be non-negative");
        DeviceIdentity identity = authService.authenticate(authorization);
        int safeLimit = Math.min(Math.max(limit, 1), 500);
        List<SyncChange> rows = jdbcTemplate.query(
                "select change_sequence, record_id, operation_id, entity_type, entity_id, version, ciphertext, content_hash, deleted_at "
                        + "from sync_changes where vault_id = ? and change_sequence > ? order by change_sequence limit ?",
                ps -> {
                    ps.setObject(1, identity.vaultId());
                    ps.setLong(2, cursor);
                    ps.setInt(3, safeLimit + 1);
                },
                (rs, rowNum) -> new SyncChange(
                        rs.getObject("record_id", UUID.class).toString(),
                        rs.getObject("operation_id", UUID.class).toString(),
                        rs.getString("entity_type"), rs.getString("entity_id"), rs.getLong("version"),
                        rs.getString("ciphertext"), rs.getString("content_hash"),
                        rs.getTimestamp("deleted_at") == null ? null : rs.getTimestamp("deleted_at").getTime(),
                        rs.getLong("change_sequence")
                )
        );
        boolean hasMore = rows.size() > safeLimit;
        List<SyncChange> page = hasMore ? rows.subList(0, safeLimit) : rows;
        long nextCursor = page.isEmpty() ? cursor : page.get(page.size() - 1).changeSequence();
        return new PullResponse(page, nextCursor, hasMore);
    }

    private Receipt findReceipt(UUID vaultId, UUID operationId) {
        return jdbcTemplate.query(
                "select request_hash, outcome, change_sequence from sync_operation_receipts where vault_id = ? and operation_id = ?",
                ps -> { ps.setObject(1, vaultId); ps.setObject(2, operationId); },
                rs -> rs.next() ? new Receipt(rs.getString("request_hash"), rs.getString("outcome"),
                        (Long) rs.getObject("change_sequence")) : null
        );
    }

    private ExistingRecord findRecord(UUID vaultId, String entityType, String entityId) {
        return jdbcTemplate.query(
                "select record_id, version, ciphertext, content_hash, deleted_at from encrypted_records "
                        + "where vault_id = ? and entity_type = ? and entity_id = ? for update",
                ps -> { ps.setObject(1, vaultId); ps.setString(2, entityType); ps.setString(3, entityId); },
                rs -> rs.next() ? new ExistingRecord(
                        rs.getObject("record_id", UUID.class), rs.getLong("version"), rs.getString("ciphertext"),
                        rs.getString("content_hash"), rs.getTimestamp("deleted_at") == null ? null : rs.getTimestamp("deleted_at").getTime()
                ) : null
        );
    }

    private void insertRecord(UUID vaultId, UUID recordId, EncryptedRecord record) {
        jdbcTemplate.update(
                "insert into encrypted_records(record_id, vault_id, entity_type, entity_id, version, ciphertext, content_hash, deleted_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                recordId, vaultId, record.entityType(), record.entityId(), record.version(), record.ciphertext(),
                record.contentHash(), timestampOrNull(record.deletedAt())
        );
    }

    private void updateRecord(UUID vaultId, EncryptedRecord record) {
        jdbcTemplate.update(
                "update encrypted_records set version = ?, ciphertext = ?, content_hash = ?, deleted_at = ?, updated_at = now() "
                        + "where vault_id = ? and entity_type = ? and entity_id = ?",
                record.version(), record.ciphertext(), record.contentHash(),
                timestampOrNull(record.deletedAt()),
                vaultId, record.entityType(), record.entityId()
        );
    }

    private long appendChange(UUID vaultId, UUID operationId, UUID recordId, EncryptedRecord record) {
        return jdbcTemplate.queryForObject(
                "insert into sync_changes(vault_id, operation_id, record_id, entity_type, entity_id, version, ciphertext, content_hash, deleted_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?) returning change_sequence",
                Long.class, vaultId, operationId, recordId, record.entityType(), record.entityId(), record.version(),
                record.ciphertext(), record.contentHash(),
                timestampOrNull(record.deletedAt())
        );
    }

    private static Timestamp timestampOrNull(Long epochMillis) {
        return epochMillis == null ? null : Timestamp.from(Instant.ofEpochMilli(epochMillis));
    }

    private static void validateRecord(EncryptedRecord record) {
        if (record.version() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "version must be positive");
        if (record.entityType().length() > 80 || record.entityId().length() > 200 || record.ciphertext().length() > 1_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sync record exceeds the supported size");
        }
        String actualHash = sha256(record.ciphertext());
        if (!MessageDigest.isEqual(actualHash.getBytes(StandardCharsets.US_ASCII), record.contentHash().getBytes(StandardCharsets.US_ASCII))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentHash does not match ciphertext");
        }
    }

    private static UUID parseUuid(String value, String field) {
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be a UUID");
        }
    }

    private static String requestHash(EncryptedRecord record) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            add(digest, record.recordId());
            add(digest, record.entityType());
            add(digest, record.entityId());
            digest.update(ByteBuffer.allocate(Long.BYTES).putLong(record.version()).array());
            add(digest, record.ciphertext());
            add(digest, record.contentHash());
            digest.update((byte) (record.deletedAt() == null ? 0 : 1));
            if (record.deletedAt() != null) digest.update(ByteBuffer.allocate(Long.BYTES).putLong(record.deletedAt()).array());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void add(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record PushRequest(@NotNull @Size(max = 100) List<@Valid EncryptedRecord> records) { }
    public record EncryptedRecord(
            @NotBlank String recordId,
            @NotBlank String operationId,
            @NotBlank String entityType,
            @NotBlank String entityId,
            @Positive long version,
            @NotBlank String ciphertext,
            @NotBlank String contentHash,
            Long deletedAt
    ) { }
    public record Acknowledgement(String operationId, String outcome, Long changeSequence) { }
    public record PushResponse(List<Acknowledgement> acknowledgements, long cursor) { }
    public record SyncChange(
            String recordId, String operationId, String entityType, String entityId, long version,
            String ciphertext, String contentHash, Long deletedAt, long changeSequence
    ) { }
    public record PullResponse(List<SyncChange> records, long nextCursor, boolean hasMore) { }
    private record Receipt(String requestHash, String outcome, Long changeSequence) { }
    private record ExistingRecord(UUID recordId, long version, String ciphertext, String contentHash, Long deletedAt) {
        boolean sameSnapshot(EncryptedRecord record) {
            long incomingDeletedAt = record.deletedAt() == null ? -1L : record.deletedAt();
            long storedDeletedAt = deletedAt == null ? -1L : deletedAt;
            return ciphertext.equals(record.ciphertext()) && contentHash.equals(record.contentHash()) && incomingDeletedAt == storedDeletedAt;
        }
    }
}
