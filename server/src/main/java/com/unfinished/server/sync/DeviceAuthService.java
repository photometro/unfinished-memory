package com.unfinished.server.sync;

import com.unfinished.server.device.DeviceController;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeviceAuthService {

    private final JdbcTemplate jdbcTemplate;

    public DeviceAuthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DeviceIdentity authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token required");
        }
        String tokenHash = DeviceController.hashToken(authorizationHeader.substring("Bearer ".length()).trim());
        DeviceIdentity identity = jdbcTemplate.query(
                "select d.device_id, d.vault_id from device_tokens t "
                        + "join devices d on d.device_id = t.device_id "
                        + "where t.token_hash = ? and t.revoked_at is null and d.revoked_at is null "
                        + "and (t.expires_at is null or t.expires_at > now())",
                ps -> ps.setString(1, tokenHash),
                rs -> rs.next()
                        ? new DeviceIdentity(rs.getObject("device_id", UUID.class), rs.getObject("vault_id", UUID.class))
                        : null
        );
        if (identity == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid device token");
        }
        return identity;
    }

    public record DeviceIdentity(UUID deviceId, UUID vaultId) {
    }
}
