package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.CategoryEntity;
import com.alexgit95.MyTrips.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService service;

    @Test
    void delete_shouldThrowForSystemCategory() {
        CategoryEntity system = CategoryEntity.builder().id(1L).name("System").editable(false).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(system));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.delete(1L));

        assertTrue(ex.getMessage().toLowerCase().contains("impossible"));
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    void delete_shouldDeleteEditableCategory() {
        CategoryEntity editable = CategoryEntity.builder().id(2L).name("Food").editable(true).build();
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(editable));

        service.delete(2L);

        verify(categoryRepository).deleteById(2L);
    }

    @Test
    void findByName_shouldThrowWhenMissing() {
        when(categoryRepository.findByName("Missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.findByName("Missing"));
    }

    @Test
    void initializeDefaultCategories_shouldInsertSeedOnlyWhenEmpty() {
        when(categoryRepository.count()).thenReturn(0L);

        service.initializeDefaultCategories();

        ArgumentCaptor<List<CategoryEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(categoryRepository).saveAll(captor.capture());
        assertEquals(8, captor.getValue().size());
    }

    @Test
    void initializeDefaultCategories_shouldDoNothingWhenNotEmpty() {
        when(categoryRepository.count()).thenReturn(3L);

        service.initializeDefaultCategories();

        verify(categoryRepository, never()).saveAll(anyList());
    }
}
