package com.example.auth_app.repository;

import com.example.auth_app.model.SessionToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionRepository extends JpaRepository<SessionToken, String> {
    Optional<SessionToken> findByToken(String token);
    void deleteByToken(String token);
}

