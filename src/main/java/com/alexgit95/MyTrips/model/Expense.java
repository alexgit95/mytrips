package com.alexgit95.MyTrips.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expense")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    @Column(nullable = false)
    private BigDecimal amount;

    @NotNull(message = "La date est obligatoire")
    @Column(nullable = false)
    private LocalDate date;

    @NotNull(message = "La catégorie est obligatoire")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @NotBlank(message = "Le libellé est obligatoire")
    @Column(nullable = false)
    private String label;

    @Min(value = 1, message = "Le nombre de jours doit être au minimum 1")
    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 1")
    @Builder.Default
    private Integer numberOfDays = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Trip trip;
}
