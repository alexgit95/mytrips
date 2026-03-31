package com.alexgit95.MyTrips.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocationParserServiceTest {

    private final LocationParserService service = new LocationParserService();

    @Test
    void parse_shouldDetectFranceAndDepartmentFromCity() {
        LocationParserService.LocationInfo info = service.parse("Weekend in Paris, France");

        assertNotNull(info);
        assertEquals("FR", info.isoCode());
        assertEquals("France", info.countryFr());
        assertEquals("Europe", info.continent());
        assertEquals("Paris", info.subdivision());
    }

    @Test
    void parse_shouldDetectUsaStateFromAbbreviation() {
        LocationParserService.LocationInfo info = service.parse("Road trip in Austin, TX, USA");

        assertNotNull(info);
        assertEquals("US", info.isoCode());
        assertEquals("Texas", info.subdivision());
    }

    @Test
    void parse_shouldReturnNullWhenCountryIsUnknown() {
        assertNull(service.parse("Unknown place on Mars"));
    }

    @Test
    void lookupByIso_shouldBeCaseInsensitive() {
        LocationParserService.LocationInfo info = service.lookupByIso("fr");

        assertNotNull(info);
        assertEquals("FR", info.isoCode());
        assertEquals("France", info.countryFr());
    }
}
