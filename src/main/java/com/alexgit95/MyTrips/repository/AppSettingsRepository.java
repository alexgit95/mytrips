package com.alexgit95.MyTrips.repository;

import com.alexgit95.MyTrips.model.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingsRepository extends JpaRepository<AppSettings, Long> {
}
