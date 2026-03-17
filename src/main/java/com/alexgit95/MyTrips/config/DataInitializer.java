package com.alexgit95.MyTrips.config;

import com.alexgit95.MyTrips.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryService categoryService;

    @Override
    public void run(String... args) throws Exception {
        // Initialiser les catégories par défaut si absent
        categoryService.initializeDefaultCategories();
    }
}
