package com.alexgit95.MyTrips.config;

import com.alexgit95.MyTrips.model.PersistentLogin;
import com.alexgit95.MyTrips.repository.PersistentLoginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.Date;

/**
 * Implémentation JPA du PersistentTokenRepository de Spring Security.
 * Les tokens "Se souvenir de moi" sont stockés en base (SQLite / PostgreSQL)
 * et survivent aux redémarrages du serveur.
 */
@Component
@RequiredArgsConstructor
public class JpaPersistentTokenRepository implements PersistentTokenRepository {

    private final PersistentLoginRepository loginRepository;

    @Override
    @Transactional
    public void createNewToken(PersistentRememberMeToken token) {
        PersistentLogin entity = PersistentLogin.builder()
                .series(token.getSeries())
                .username(token.getUsername())
                .token(token.getTokenValue())
                .lastUsed(token.getDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime())
                .build();
        loginRepository.save(entity);
    }

    @Override
    @Transactional
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        loginRepository.findById(series).ifPresent(entity -> {
            entity.setToken(tokenValue);
            entity.setLastUsed(lastUsed.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime());
            loginRepository.save(entity);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        return loginRepository.findById(seriesId)
                .map(entity -> new PersistentRememberMeToken(
                        entity.getUsername(),
                        entity.getSeries(),
                        entity.getToken(),
                        Date.from(entity.getLastUsed()
                                .atZone(ZoneId.systemDefault())
                                .toInstant())))
                .orElse(null);
    }

    @Override
    @Transactional
    public void removeUserTokens(String username) {
        loginRepository.deleteByUsername(username);
    }
}
