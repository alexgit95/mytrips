package com.alexgit95.MyTrips.config;

import com.alexgit95.MyTrips.model.CategoryEntity;
import com.alexgit95.MyTrips.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryConverter implements Converter<String, CategoryEntity> {

    private final CategoryService categoryService;

    @Override
    public CategoryEntity convert(String source) {
        try {
            Long id = Long.parseLong(source);
            return categoryService.findById(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID de catégorie invalide : " + source, e);
        }
    }
}
