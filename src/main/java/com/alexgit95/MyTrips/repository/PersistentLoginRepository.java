package com.alexgit95.MyTrips.repository;

import com.alexgit95.MyTrips.model.PersistentLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersistentLoginRepository extends JpaRepository<PersistentLogin, String> {

    List<PersistentLogin> findByUsername(String username);

    void deleteByUsername(String username);
}
