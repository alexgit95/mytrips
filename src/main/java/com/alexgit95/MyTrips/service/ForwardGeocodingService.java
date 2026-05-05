package com.alexgit95.MyTrips.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;

/**
 * Forward geocoding service: converts address text to GPS coordinates.
 * Uses OpenStreetMap Nominatim API (free, no API key required).
 *
 * Converts addresses like "Zion National Park, UT" → [37.3, -113.0]
 * 
 * Controlled by GEOCODING_ENABLED environment variable / app.geocoding.enabled property.
 * If disabled, returns null and skips API calls.
 */
@Service
public class ForwardGeocodingService {

    private static final Logger log = LoggerFactory.getLogger(ForwardGeocodingService.class);

    @Value("${GEOCODING_ENABLED:${app.geocoding.enabled:false}}")
    private boolean enabled;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RestClient restClient;

    public ForwardGeocodingService() {
        // RestClient will be lazily initialized on first use if enabled
        log.info("ForwardGeocodingService: initialized");
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Runtime override — called from admin panel to enable/disable without restart
     */
    public synchronized void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled && this.restClient == null) {
            this.restClient = RestClient.builder()
                    .baseUrl("https://nominatim.openstreetmap.org")
                    .defaultHeader("User-Agent", "MyTrips-App")
                    .defaultHeader("Accept", "application/json")
                    .build();
        }
        log.info("ForwardGeocodingService: {} (admin override)", enabled ? "enabled" : "disabled");
    }

    /**
     * Geocode an address to coordinates.
     *
     * @param address The address text (e.g. "Zion National Park, UT")
    * @return geocoding result with coordinates and display name, or null if disabled or not found
     */
    public GeocodingResult geocode(String address) {
        if (!enabled) {
            log.debug("Geocoding disabled, skipping: {}", address);
            return null;
        }

        if (address == null || address.isBlank()) {
            return null;
        }

        try {
            ensureRestClientInitialized();

            log.debug("Geocoding address: {}", address);

            NominatimSearchResponse[] results = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", address)
                            .queryParam("format", "json")
                            .queryParam("limit", "1")
                            .build())
                    .exchange((request, response) -> {
                        int statusCode = response.getStatusCode().value();
                        String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);

                        log.info("Nominatim response for '{}': status={} body={}", address, statusCode, responseBody);

                        if (!response.getStatusCode().is2xxSuccessful()) {
                            log.warn("Nominatim returned non-success status for '{}': status={} body={}",
                                    address,
                                    statusCode,
                                    responseBody);
                            return null;
                        }

                        if (responseBody == null || responseBody.isBlank()) {
                            return null;
                        }

                        return objectMapper.readValue(responseBody, NominatimSearchResponse[].class);
                    });

            if (results != null && results.length > 0) {
                NominatimSearchResponse result = results[0];
                if (result.lat != null && result.lon != null) {
                    log.info("✓ Geocoded '{}' to lat={}, lon={} using display_name='{}'",
                            address,
                            result.lat,
                            result.lon,
                            result.display_name);
                    return new GeocodingResult(result.lat, result.lon, result.display_name);
                }
            }

            log.warn("✗ No geocoding result for: {}", address);
            return null;

        } catch (RestClientException e) {
            log.warn("Geocoding HTTP error for '{}': {}", address, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error during geocoding", e);
            return null;
        }
    }

    private synchronized void ensureRestClientInitialized() {
        if (this.restClient != null) {
            return;
        }

        this.restClient = RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                .defaultHeader("User-Agent", "MyTrips-App")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    // =========================================================================
    // JSON response classes
    // =========================================================================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NominatimSearchResponse {
        public Double lat;
        public Double lon;
        public String display_name;
    }

    public record GeocodingResult(double latitude, double longitude, String displayName) {
    }
}
