package com.alexgit95.MyTrips.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Paramètres globaux de l'application (ligne singleton, id = 1).
 */
@Entity
@Table(name = "app_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSettings {

    /** Identifiant fixe = 1 (entrée singleton). */
    @Id
    @Column(name = "id")
    @Builder.Default
    private Long id = 1L;

    /**
     * Code ISO 3166-1 alpha-2 du pays d'origine de l'utilisateur (ex : "FR").
     * Utilisé pour filtrer les points du pays d'origine lors de l'affichage Road Trip.
     */
    @Column(nullable = false)
    @Builder.Default
    private String homeCountry = "FR";
}
