package com.alexgit95.MyTrips.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportDto {
    private List<CategoryExportDto> categories;
    private List<TripExportDto> trips;
    private List<ExpenseExportDto> expenses;
    private List<PlannerEventExportDto> plannerEvents;
    private List<UserExportDto> users;
    /** Pays d'origine (code ISO 3166-1 alpha-2, ex: "FR"). Peut être null pour la compatibilité ascendante. */
    private String homeCountry;
}
