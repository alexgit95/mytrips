package com.alexgit95.MyTrips.repository;

import com.alexgit95.MyTrips.model.ApiAccessKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiAccessKeyRepository extends JpaRepository<ApiAccessKey, Long> {
    Optional<ApiAccessKey> findByKeyHashAndRevokedFalse(String keyHash);

    Optional<ApiAccessKey> findTopByOrderByCreatedAtDesc();

    List<ApiAccessKey> findByRevokedFalse();

    List<ApiAccessKey> findAllByOrderByCreatedAtDesc();
}
