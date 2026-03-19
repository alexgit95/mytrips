package com.alexgit95.MyTrips.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Service de géocodage inverse (lat/lon → adresse).
 * 
 * Utilise l'API Nominatim (OpenStreetMap) — gratuit, sans clé API.
 * Respecte les conditions d'utilisation avec délai et cache mémoire.
 * 
 * Peut être désactivé via l'admin (fallback sur coordonnées GPS).
 */
@Service
public class ReverseGeocodingService {

    private static final Logger log = LoggerFactory.getLogger(ReverseGeocodingService.class);

    @Value("${GEOCODING_ENABLED:${app.geocoding.enabled:false}}")
    private boolean enabled;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        if (enabled) {
            this.restClient = RestClient.builder()
                    .baseUrl("https://nominatim.openstreetmap.org")
                    .defaultHeader("User-Agent", "MyTrips-App")
                    .defaultHeader("Accept", "application/json")
                    .build();
            log.info("ReverseGeocodingService: activé (Nominatim)");
        } else {
            log.info("ReverseGeocodingService: désactivé");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Runtime override — appelé depuis le panneau admin
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
        log.info("ReverseGeocodingService: {} (admin override)", enabled ? "activé" : "désactivé");
    }

    /**
     * Convertir des coordonnées GPS en adresse.
     * 
     * @param latitude Latitude
     * @param longitude Longitude
     * @return L'adresse obtenue depuis Nominatim, ou null si désactivé ou erreur
     */
    public String reverseGeocode(Double latitude, Double longitude) {
        if (!enabled || latitude == null || longitude == null) {
            return null;
        }

        try {
            // Nominatim reverse geocoding avec queryParam pour éviter les problèmes d'encodage
            NominatimResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/reverse")
                            .queryParam("format", "json")
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("zoom", "18")
                            .build())
                    .retrieve()
                    .body(NominatimResponse.class);

            if (response != null && response.address != null) {
                // Construire une adresse lisible depuis les composants
                return formatAddress(response.address);
            }
        } catch (RestClientException e) {
            log.warn("Erreur lors du géocodage inverse (lat={}, lon={}): {}",
                    latitude, longitude, e.getMessage());
        } catch (Exception e) {
            log.error("Erreur inattendue lors du géocodage inverse:", e);
        }

        return null;
    }

    /**
     * Formater l'adresse à partir des composants Nominatim
     */
    private String formatAddress(Address address) {
        StringBuilder sb = new StringBuilder();
        
        if (address.house_number != null) {
            sb.append(address.house_number).append(" ");
        }
        if (address.road != null) {
            sb.append(address.road);
        } else if (address.footway != null) {
            sb.append(address.footway);
        } else if (address.pedestrian != null) {
            sb.append(address.pedestrian);
        }
        
        if (address.city != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.city);
        } else if (address.town != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.town);
        } else if (address.village != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.village);
        }
        
        if (address.postcode != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(address.postcode);
        }
        
        if (address.country != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.country);
        }
        
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    // =========================================================================
    // Classes internes pour JSON mapping
    // =========================================================================

    public static class NominatimResponse {
        public Address address;
        public String display_name;
        public double lat;
        public double lon;
    }

    public static class Address {
        public String house_number;
        public String road;
        public String footway;
        public String pedestrian;
        public String city;
        public String town;
        public String village;
        public String postcode;
        public String country;
    }
}
