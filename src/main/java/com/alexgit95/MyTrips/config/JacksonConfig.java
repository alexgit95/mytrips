package com.alexgit95.MyTrips.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Spring Boot 4 / Jackson 3 auto-configure nativement :
//  - JavaTimeModule (support LocalDate, LocalDateTime...)
//  - WRITE_DATES_AS_TIMESTAMPS=false (via application.properties)
@Configuration
public class JacksonConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
