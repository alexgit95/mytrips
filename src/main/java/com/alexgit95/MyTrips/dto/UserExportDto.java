package com.alexgit95.MyTrips.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserExportDto {
    private Long id;
    private String username;
    private String password; // BCrypt hash — restauré tel quel lors de l'import
    private String role;
}
