package com.alexgit95.MyTrips.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Enregistrer le module pour supporter LocalDate, LocalDateTime, etc.
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
