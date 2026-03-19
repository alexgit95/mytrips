package com.alexgit95.MyTrips.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

/**
 * Configuration Spring Security.
 * - Authentification par formulaire (/login)
 * - Remember-me persistant en base (survie aux redémarrages)
 * - Durée du cookie "Se souvenir de moi" : 12 mois
 * - Identifiants configurables dans application.properties
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 365 jours en secondes = 12 mois
    private static final int REMEMBER_ME_DURATION = 365 * 24 * 60 * 60;

    @Value("${app.security.username:admin}")
    private String username;

    @Value("${app.security.password:admin}")
    private String password;

    @Value("${app.security.remember-me-key:mytrips-remember-me-secret}")
    private String rememberMeKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
                .username(username)
                .password(passwordEncoder().encode(password))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           PersistentTokenRepository tokenRepository) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Pages publiques
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**",
                                 "/webjars/**", "/favicon.ico","/manifest.json").permitAll()
                // Health check endpoint accessible sans authentification (pour Portainer)
                .requestMatchers("/actuator/health").permitAll()
                // Tout le reste nécessite d'être authentifié
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
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
            );

        return http.build();
    }
}
