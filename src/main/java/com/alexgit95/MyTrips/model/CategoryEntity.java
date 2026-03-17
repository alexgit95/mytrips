package com.alexgit95.MyTrips.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "category")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom de la catégorie est obligatoire")
    @Column(nullable = false, unique = true)
    private String name;

    private String icon;  // emoji ou emoji unicode

    private String color; // couleur hex ou nom CSS

    @Builder.Default
    private Boolean editable = true;  // false pour catégories système non supprimables
}
