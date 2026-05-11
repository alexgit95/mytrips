package com.alexgit95.MyTrips.config;

import com.alexgit95.MyTrips.service.AppUserService;
import com.alexgit95.MyTrips.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CategoryService categoryService;
    private final AppUserService appUserService;

    @Value("${app.security.username:admin}")
    private String defaultUsername;

    @Value("${app.security.password:admin}")
    private String defaultPassword;

    @Override
    public void run(String... args) throws Exception {
        // Initialiser les catégories par défaut si absent
        categoryService.initializeDefaultCategories();

        // Créer l'utilisateur admin par défaut si aucun utilisateur n'existe
        if (!appUserService.hasUsers()) {
            appUserService.createUserWithPassword(defaultUsername, defaultPassword, "ADMIN");
            log.info("[INIT] Utilisateur admin par défaut créé : {}", defaultUsername);
        }
    }
}
