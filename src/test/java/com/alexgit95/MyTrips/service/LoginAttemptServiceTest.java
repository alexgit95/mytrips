package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.AppUser;
import com.alexgit95.MyTrips.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    @Test
    void recordFailure_shouldLockUserAfterFiveConsecutiveFailures() {
        AppUser user = AppUser.builder()
                .username("alice")
                .password("hash")
                .role("ADMIN")
                .build();

        ReflectionTestUtils.setField(loginAttemptService, "maxFailures", 5);
        ReflectionTestUtils.setField(loginAttemptService, "lockMinutes", 15);

        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertFalse(loginAttemptService.recordFailure("alice"));
        assertFalse(loginAttemptService.recordFailure("alice"));
        assertFalse(loginAttemptService.recordFailure("alice"));
        assertFalse(loginAttemptService.recordFailure("alice"));

        boolean locked = loginAttemptService.recordFailure("alice");
        assertTrue(locked);
        assertNotNull(user.getLockedUntil());
        assertTrue(user.getLockedUntil().isAfter(LocalDateTime.now().plusMinutes(14)));
    }

    @Test
    void recordSuccess_shouldResetCountersAndUnlock() {
        AppUser user = AppUser.builder()
                .username("bob")
                .password("hash")
                .role("REPORTER")
                .failedLoginAttempts(3)
                .lockedUntil(LocalDateTime.now().plusMinutes(10))
                .build();

        when(appUserRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        loginAttemptService.recordSuccess("bob");

        assertTrue(user.getFailedLoginAttempts() == 0);
        assertTrue(user.getLockedUntil() == null);
        verify(appUserRepository).save(user);
    }
}