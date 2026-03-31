package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.AppUser;
import com.alexgit95.MyTrips.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int PASSWORD_LENGTH = 12;

    public List<AppUser> findAll() {
        return userRepository.findAll();
    }

    /**
     * Crée un utilisateur avec un mot de passe auto-généré.
     * Retourne le mot de passe en clair (à afficher une seule fois).
     */
    @Transactional
    public String createUser(String username, String role) {
        String rawPassword = generatePassword();
        AppUser user = AppUser.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .build();
        userRepository.save(user);
        return rawPassword;
    }

    /**
     * Crée un utilisateur avec un mot de passe fourni (pour l'initialisation ou l'import).
     */
    @Transactional
    public void createUserWithPassword(String username, String password, String role) {
        AppUser user = AppUser.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build();
        userRepository.save(user);
    }

    /**
     * Importe un utilisateur avec un hash de mot de passe déjà encodé (pour l'import JSON).
     */
    @Transactional
    public void importUserWithEncodedPassword(String username, String encodedPassword, String role) {
        AppUser user = AppUser.builder()
                .username(username)
                .password(encodedPassword)
                .role(role)
                .build();
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public boolean hasUsers() {
        return userRepository.count() > 0;
    }

    private String generatePassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
