package com.alexgit95.MyTrips.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryExportDto {
    private Long id;
    private String name;
    private String icon;
    private String color;
    private Boolean editable;
}
