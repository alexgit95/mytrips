package com.alexgit95.MyTrips.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entité JPA pour stocker les tokens "Se souvenir de moi" de Spring Security.
 * La table est créée/mise à jour automatiquement via ddl-auto=update.
 */
@Entity
@Table(name = "persistent_logins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersistentLogin {

    /** Série = identifiant unique de la session remember-me */
    @Id
    @Column(length = 64, nullable = false)
    private String series;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, length = 64)
    private String token;

    @Column(name = "last_used", nullable = false)
    private LocalDateTime lastUsed;
}
