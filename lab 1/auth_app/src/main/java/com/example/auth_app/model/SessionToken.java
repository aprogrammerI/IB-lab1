package com.example.auth_app.model;


import jakarta.persistence.*;
import java.time.Instant;


@Entity
@Table(name = "sessions")
public class SessionToken {
    @Id
    private String token; // random UUID string

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    public SessionToken() {}

    public SessionToken(String token, AppUser user, Instant createdAt, Instant expiresAt) {
        this.token = token; this.user = user; this.createdAt = createdAt; this.expiresAt = expiresAt;
    }


    public String getToken() { return token; }
    public AppUser getUser() { return user; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}

