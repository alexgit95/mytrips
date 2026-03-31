package com.alexgit95.MyTrips.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoCountryResolverTest {

    @Test
    void resolve_shouldReturnIsoForKnownCoordinatesInLocalMode() {
        GeoCountryResolver resolver = new GeoCountryResolver();
        resolver.init();

        String iso = resolver.resolve(48.8566, 2.3522);

        assertEquals("FR", iso);
    }

    @Test
    void resolveSubdivision_shouldReturnFrenchDepartmentInLocalMode() {
        GeoCountryResolver resolver = new GeoCountryResolver();
        resolver.init();

        String subdivision = resolver.resolveSubdivision("FR", 48.8566, 2.3522);

        assertEquals("Paris", subdivision);
    }

    @Test
    void resolve_shouldReturnNullForNullCoordinates() {
        GeoCountryResolver resolver = new GeoCountryResolver();

        assertNull(resolver.resolve(null, 2.0));
        assertNull(resolver.resolve(48.0, null));
    }
}
