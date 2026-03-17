package com.alexgit95.MyTrips.config;

// Spring Boot 4 / Jackson 3 auto-configure nativement :
//  - JavaTimeModule (support LocalDate, LocalDateTime...)
//  - WRITE_DATES_AS_TIMESTAMPS=false (via application.properties)
// Ce fichier est conservé comme point d'extension si besoin de customisation.
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    // Personnalisations Jackson supplémentaires à ajouter ici si nécessaire.
}
