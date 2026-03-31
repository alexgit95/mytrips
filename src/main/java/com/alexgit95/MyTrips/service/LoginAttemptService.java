package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.AppUser;
import com.alexgit95.MyTrips.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final AppUserRepository appUserRepository;

    @Value("${app.security.login-lock.max-failures:5}")
    private int maxFailures;

    @Value("${app.security.login-lock.lock-minutes:15}")
    private int lockMinutes;

    @Transactional
    public boolean recordFailure(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        AppUser user = appUserRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockedUntil = user.getLockedUntil();

        if (lockedUntil != null && lockedUntil.isAfter(now)) {
            return true;
        }

        if (lockedUntil != null) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }

        int attempts = user.getFailedLoginAttempts() + 1;
        if (attempts >= Math.max(1, maxFailures)) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(now.plusMinutes(Math.max(1, lockMinutes)));
            appUserRepository.save(user);
            return true;
        }

        user.setFailedLoginAttempts(attempts);
        appUserRepository.save(user);
        return false;
    }

    @Transactional
    public void recordSuccess(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        appUserRepository.findByUsername(username).ifPresent(user -> {
            if (user.getFailedLoginAttempts() == 0 && user.getLockedUntil() == null) {
                return;
            }
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            appUserRepository.save(user);
        });
    }
}