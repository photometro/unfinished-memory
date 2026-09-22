package com.unfinished.server.media;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.unfinished.server.sync.DeviceAuthService;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final JdbcTemplate jdbcTemplate;
    private final DeviceAuthService authService;
    private final String publicBaseUrl;

    public MediaController(
            JdbcTemplate jdbcTemplate,
            DeviceAuthService authService,
            @Value("${unfinished.media.public-base-url:http://localhost:8080/media}") String publicBaseUrl
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.authService = authService;
        this.publicBaseUrl = publicBaseUrl;
    }

    @PostMapping("/upload-ticket")
    public UploadTicket createTicket(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody UploadRequest request
    ) {
        DeviceAuthService.DeviceIdentity identity = authService.authenticate(authorization);
        UUID objectId = UUID.randomUUID();
        String objectKey = identity.vaultId() + "/" + objectId + "/" + request.fileName();
        jdbcTemplate.update(
                "insert into media_objects(object_id, vault_id, entity_id, object_key, sha256, size_bytes, content_type) values (?, ?, ?, ?, ?, ?, ?)",
                objectId, identity.vaultId(), request.entityId(), objectKey, request.sha256(), request.sizeBytes(), request.contentType()
        );
        // OSS signing is intentionally isolated behind this endpoint; replace the URL builder with an OSS adapter in deployment.
        return new UploadTicket(objectId.toString(), objectKey, publicBaseUrl + "/upload/" + objectId, Instant.now().plus(Duration.ofMinutes(10)).toEpochMilli());
    }

    @DeleteMapping
    public void delete(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody DeleteRequest request
    ) {
        DeviceAuthService.DeviceIdentity identity = authService.authenticate(authorization);
        jdbcTemplate.update("update media_objects set deleted_at = now(), upload_state = 'DELETED' where object_id = ? and vault_id = ?", UUID.fromString(request.objectId()), identity.vaultId());
    }

    public record UploadRequest(@NotBlank String entityId, @NotBlank String fileName, @NotBlank String sha256, long sizeBytes, String contentType) {
    }

    public record DeleteRequest(@NotBlank String objectId) {
    }

    public record UploadTicket(String objectId, String objectKey, String uploadUrl, long expiresAt) {
    }
}
