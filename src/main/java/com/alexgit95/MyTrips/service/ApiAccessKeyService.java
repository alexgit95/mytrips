package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.ApiAccessKey;
import com.alexgit95.MyTrips.repository.ApiAccessKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiAccessKeyService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration MAX_VALIDITY = Duration.ofDays(365);

    private final ApiAccessKeyRepository apiAccessKeyRepository;

    @Transactional
    public GeneratedApiKey generateKey(Duration validity, String name) {
        if (validity == null || validity.isNegative() || validity.isZero()) {
            throw new IllegalArgumentException("La duree de validite doit etre strictement positive.");
        }
        if (validity.compareTo(MAX_VALIDITY) > 0) {
            throw new IllegalArgumentException("La duree de validite maximale est de 12 mois.");
        }

        LocalDateTime now = LocalDateTime.now();
        String rawKey = generateRawKey();
        String trimmedName = (name != null && !name.isBlank()) ? name.trim() : null;

        ApiAccessKey entity = ApiAccessKey.builder()
                .keyHash(hash(rawKey))
                .name(trimmedName)
                .createdAt(now)
                .expiresAt(now.plus(validity))
                .revoked(false)
                .build();

        apiAccessKeyRepository.save(entity);
        return new GeneratedApiKey(rawKey, entity.getExpiresAt());
    }

    public List<ApiAccessKey> findAllKeys() {
        return apiAccessKeyRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void deleteKey(Long id) {
        apiAccessKeyRepository.deleteById(id);
    }

    @Transactional
    public boolean authenticate(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        return apiAccessKeyRepository.findByKeyHashAndRevokedFalse(hash(rawKey))
                .filter(key -> key.getExpiresAt().isAfter(now))
                .map(key -> {
                    key.setLastUsedAt(now);
                    apiAccessKeyRepository.save(key);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public int revokeAllActiveKeys() {
        LocalDateTime now = LocalDateTime.now();
        var activeKeys = apiAccessKeyRepository.findByRevokedFalse();
        activeKeys.forEach(key -> {
            key.setRevoked(true);
            if (key.getExpiresAt() != null && key.getExpiresAt().isAfter(now)) {
                key.setExpiresAt(now);
            }
        });
        apiAccessKeyRepository.saveAll(activeKeys);
        return activeKeys.size();
    }

    private String generateRawKey() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return "mtk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponible sur cette JVM", e);
        }
    }

    public record GeneratedApiKey(String rawKey, LocalDateTime expiresAt) {
    }
}
