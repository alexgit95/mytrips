package com.alexgit95.MyTrips.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trip")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "La date de début est obligatoire")
    @Column(nullable = false)
    private LocalDate startDate;

    @NotNull(message = "La date de fin est obligatoire")
    @Column(nullable = false)
    private LocalDate endDate;

    @NotNull(message = "Le budget est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le budget doit être positif")
    @Column(nullable = false)
    private BigDecimal budget;

    private String imageUrl;

    @DecimalMin(value = "0.0", inclusive = true, message = "Le seuil doit être positif ou nul")
    @Column(nullable = false, columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    @Builder.Default
    private BigDecimal dailyBudgetThreshold = java.math.BigDecimal.ZERO;

    @DecimalMin(value = "0.0", inclusive = true, message = "La dépense journalière doit être positive ou nulle")
    @Column(nullable = false, columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    @Builder.Default
    private BigDecimal dailyExpenseBudget = java.math.BigDecimal.ZERO;

    // Pays principal du voyage (optionnel, ex: "France", "Italie", "USA")
    private String country;

    // Coordonnées géographiques (optionnelles)
    private Double latitude;
    private Double longitude;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Expense> expenses = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<PlannerEvent> plannerEvents = new ArrayList<>();

    /**
     * Vérifier si le voyage est en cours (entre startDate et endDate, aujourd'hui)
     */
    public boolean isOngoing() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }
}
