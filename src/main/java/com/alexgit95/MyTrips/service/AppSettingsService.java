package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.AppSettings;
import com.alexgit95.MyTrips.repository.AppSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppSettingsService {

    private final AppSettingsRepository appSettingsRepository;

    /**
     * Retourne la configuration (crée la ligne singleton avec "FR" si elle n'existe pas encore).
     */
    public AppSettings getSettings() {
        return appSettingsRepository.findById(1L)
                .orElseGet(() -> appSettingsRepository.save(
                        AppSettings.builder().id(1L).homeCountry("FR").build()));
    }

    /**
     * Retourne le code ISO du pays d'origine (ex: "FR").
     */
    public String getHomeCountry() {
        return getSettings().getHomeCountry();
    }

    /**
     * Met à jour le pays d'origine.
     *
     * @param isoCode code ISO 3166-1 alpha-2 (ex: "FR", "DE", "IT")
     */
    @Transactional
    public void setHomeCountry(String isoCode) {
        AppSettings settings = getSettings();
        settings.setHomeCountry(isoCode != null ? isoCode.trim().toUpperCase() : "FR");
        appSettingsRepository.save(settings);
    }
}
