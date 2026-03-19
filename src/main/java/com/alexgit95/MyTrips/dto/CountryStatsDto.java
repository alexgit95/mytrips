package com.alexgit95.MyTrips.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a visited country with its flag, sub-divisions, and source references.
 */
public class CountryStatsDto {
    private String isoCode;
    private String countryFr;
    private String flag;
    private String continent;
    /** Departments (France) or States (USA) found in locations */
    private List<String> subdivisions = new ArrayList<>();
    /** Source descriptions: trip names / planner event locations */
    private List<String> sources = new ArrayList<>();

    public CountryStatsDto() {}

    public CountryStatsDto(String isoCode, String countryFr, String flag, String continent) {
        this.isoCode = isoCode;
        this.countryFr = countryFr;
        this.flag = flag;
        this.continent = continent;
    }

    public String getIsoCode() { return isoCode; }
    public String getCountryFr() { return countryFr; }
    public String getFlag() { return flag; }
    public String getContinent() { return continent; }
    public List<String> getSubdivisions() { return subdivisions; }
    public List<String> getSources() { return sources; }

    public void setIsoCode(String isoCode) { this.isoCode = isoCode; }
    public void setCountryFr(String countryFr) { this.countryFr = countryFr; }
    public void setFlag(String flag) { this.flag = flag; }
    public void setContinent(String continent) { this.continent = continent; }
    public void setSubdivisions(List<String> subdivisions) { this.subdivisions = subdivisions; }
    public void setSources(List<String> sources) { this.sources = sources; }
}
