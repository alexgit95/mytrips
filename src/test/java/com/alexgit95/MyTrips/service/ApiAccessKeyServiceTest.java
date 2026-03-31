package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.ApiAccessKey;
import com.alexgit95.MyTrips.repository.ApiAccessKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:./target/mytrips-api-keys-service-test.db",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create"
})
class ApiAccessKeyServiceTest {

    @Autowired
    private ApiAccessKeyService apiAccessKeyService;

    @Autowired
    private ApiAccessKeyRepository apiAccessKeyRepository;

    @BeforeEach
    void clean() {
        apiAccessKeyRepository.deleteAll();
    }

    @Test
    void generateKeyAndAuthenticate_shouldSucceed() {
        ApiAccessKeyService.GeneratedApiKey generated = apiAccessKeyService.generateKey(Duration.ofDays(7), null);

        assertNotNull(generated.rawKey());
        assertTrue(generated.rawKey().startsWith("mtk_"));
        assertNotNull(generated.expiresAt());
        assertTrue(generated.expiresAt().isAfter(LocalDateTime.now().minusSeconds(1)));

        assertTrue(apiAccessKeyService.authenticate(generated.rawKey()));
    }

    @Test
    void authenticate_shouldFailWhenKeyExpired() {
        ApiAccessKeyService.GeneratedApiKey generated = apiAccessKeyService.generateKey(Duration.ofDays(1), null);

        ApiAccessKey persisted = apiAccessKeyRepository.findTopByOrderByCreatedAtDesc().orElseThrow();
        persisted.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        apiAccessKeyRepository.save(persisted);

        assertFalse(apiAccessKeyService.authenticate(generated.rawKey()));
    }

    @Test
    void generateKey_shouldRejectNonPositiveValidity() {
        assertThrows(IllegalArgumentException.class, () -> apiAccessKeyService.generateKey(Duration.ZERO, null));
        assertThrows(IllegalArgumentException.class, () -> apiAccessKeyService.generateKey(Duration.ofDays(-1), null));
    }

    @Test
    void generateKey_shouldRejectValidityAboveTwelveMonths() {
        assertThrows(IllegalArgumentException.class, () -> apiAccessKeyService.generateKey(Duration.ofDays(366), null));
    }

    @Test
    void revokeAllActiveKeys_shouldRevokeEveryActiveKey() {
        ApiAccessKeyService.GeneratedApiKey key1 = apiAccessKeyService.generateKey(Duration.ofDays(10), null);
        ApiAccessKeyService.GeneratedApiKey key2 = apiAccessKeyService.generateKey(Duration.ofDays(20), null);

        int revokedCount = apiAccessKeyService.revokeAllActiveKeys();
        assertEquals(2, revokedCount);
        assertFalse(apiAccessKeyService.authenticate(key1.rawKey()));
        assertFalse(apiAccessKeyService.authenticate(key2.rawKey()));
    }
}
