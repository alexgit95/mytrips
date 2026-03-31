package com.alexgit95.MyTrips.config;

import com.alexgit95.MyTrips.model.AppUser;
import com.alexgit95.MyTrips.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.header.writers.PermissionsPolicyHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.time.LocalDateTime;

/**
 * Configuration Spring Security.
 * - Authentification par formulaire (/login) avec utilisateurs en base de données
 * - Trois rôles : ADMIN, REPORTER, GUEST
 * - Remember-me persistant en base (survie aux redémarrages)
 * - Durée du cookie "Se souvenir de moi" : 12 mois
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // 365 jours en secondes = 12 mois
    private static final int REMEMBER_ME_DURATION = 365 * 24 * 60 * 60;

    @Value("${app.security.remember-me-key:mytrips-remember-me-secret}")
    private String rememberMeKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(AppUserRepository userRepository) {
        return username -> {
            AppUser appUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + username));

            boolean accountLocked = appUser.getLockedUntil() != null
                && appUser.getLockedUntil().isAfter(LocalDateTime.now());

            return User.builder()
                    .username(appUser.getUsername())
                    .password(appUser.getPassword())
                .accountLocked(accountLocked)
                    .roles(appUser.getRole())
                    .build();
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ApiExportRateLimitFilter apiExportRateLimitFilter,
                                           ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                                           LoginAuthenticationSuccessHandler loginAuthenticationSuccessHandler,
                                           LoginAuthenticationFailureHandler loginAuthenticationFailureHandler,
                                           PersistentTokenRepository tokenRepository) throws Exception {
        http
            .headers(headers -> headers
                // Empêche la clé API (?apiKey=...) de fuiter dans le header Referer vers des tiers
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                // Restreint l'accès aux capteurs/caméra/micro du navigateur
                .addHeaderWriter(new PermissionsPolicyHeaderWriter("microphone=(), camera=(), payment=()")))
            // X-Frame-Options: DENY et X-Content-Type-Options: nosniff sont déjà ajoutés par Spring Security
            // X-Frame-Options: DENY et X-Content-Type-Options: nosniff sont déjà ajoutés par Spring Security
            .authorizeHttpRequests(auth -> auth
                // Pages publiques
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**",
                                 "/webjars/**", "/favicon.ico","/manifest.json").permitAll()
                // Health check endpoint accessible sans authentification (pour Portainer)
                .requestMatchers("/actuator/health").permitAll()
                // Export JSON via API key uniquement
                .requestMatchers(HttpMethod.GET, "/api/admin/export").hasRole("API_EXPORT")
                // Administration : ADMIN uniquement
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Tout le reste nécessite d'être authentifié
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(loginAuthenticationSuccessHandler)
                .failureHandler(loginAuthenticationFailureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .tokenRepository(tokenRepository)
                .tokenValiditySeconds(REMEMBER_ME_DURATION)
                .rememberMeParameter("remember-me")
                .rememberMeCookieName("remember-me")
                .key(rememberMeKey)
            )
            .addFilterBefore(apiExportRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
