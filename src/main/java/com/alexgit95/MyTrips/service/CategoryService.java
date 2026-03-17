package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.CategoryEntity;
import com.alexgit95.MyTrips.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryEntity> findAll() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public CategoryEntity findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie introuvable : " + id));
    }

    public CategoryEntity findByName(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie introuvable : " + name));
    }

    public CategoryEntity save(CategoryEntity category) {
        return categoryRepository.save(category);
    }

    public void delete(Long id) {
        CategoryEntity cat = findById(id);
        if (Boolean.FALSE.equals(cat.getEditable())) {
            throw new IllegalArgumentException("Impossible de supprimer une catégorie système.");
        }
        categoryRepository.deleteById(id);
    }

    // Initialiser les catégories par défaut si absent
    public void initializeDefaultCategories() {
        if (categoryRepository.count() == 0) {
            categoryRepository.saveAll(List.of(
                    CategoryEntity.builder().name("Transport").icon("✈️").color("#4e79a7").editable(true).build(),
                    CategoryEntity.builder().name("Hébergement").icon("🏠").color("#59a14f").editable(true).build(),
                    CategoryEntity.builder().name("Restauration").icon("🍽️").color("#f28e2b").editable(true).build(),
                    CategoryEntity.builder().name("Loisirs").icon("🎟️").color("#b07aa1").editable(true).build(),
                    CategoryEntity.builder().name("Shopping").icon("🛍️").color("#e15759").editable(true).build(),
                    CategoryEntity.builder().name("Courses").icon("🛒").color("#76b7b2").editable(true).build(),
                    CategoryEntity.builder().name("Sorties").icon("🎉").color("#ff9da7").editable(true).build(),
                    CategoryEntity.builder().name("Autre").icon("🔖").color("#9c755f").editable(true).build()
            ));
        }
    }
}
