package com.unfinished.server.device;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/device")
public class DeviceController {

    private final SecureRandom secureRandom = new SecureRandom();
    private final JdbcTemplate jdbcTemplate;

    public DeviceController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/bootstrap")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public BootstrapResponse bootstrap() {
        UUID vaultId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        String token = randomToken();
        jdbcTemplate.update("insert into vaults(vault_id) values (?)", vaultId);
        jdbcTemplate.update("insert into devices(device_id, vault_id) values (?, ?)", deviceId, vaultId);
        jdbcTemplate.update("insert into device_tokens(token_id, device_id, token_hash) values (?, ?, ?)",
                UUID.randomUUID(), deviceId, hashToken(token));
        jdbcTemplate.update("insert into sync_cursors(vault_id, device_id) values (?, ?)", vaultId, deviceId);
        return new BootstrapResponse(vaultId.toString(), deviceId.toString(), token);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record BootstrapResponse(String vaultId, String deviceId, String deviceToken) {
    }
}
