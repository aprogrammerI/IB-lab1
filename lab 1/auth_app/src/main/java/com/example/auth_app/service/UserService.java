package com.example.auth_app.service;

import com.example.auth_app.model.AppUser;
import com.example.auth_app.model.SessionToken;
import com.example.auth_app.repository.SessionRepository;
import com.example.auth_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;



@Service
public class UserService {

    private final UserRepository userRepo;
    private final SessionRepository sessionRepo;

    @Value("${app.session.expiresSeconds}")
    private long sessionExpiresSeconds;

    // BCrypt encoder instance (12 rounds)
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public UserService(UserRepository userRepo, SessionRepository sessionRepo) {
        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
    }


    public AppUser register(String username, String email, String password) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username required");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email required");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("password min 6 chars");

        if (userRepo.existsByUsername(username)) throw new IllegalArgumentException("username exists");
        if (userRepo.existsByEmail(email)) throw new IllegalArgumentException("email exists");

        // Use BCryptPasswordEncoder to hash the password
        String hashed = passwordEncoder.encode(password);
        AppUser u = new AppUser(username, email, hashed);
        return userRepo.save(u);
    }


    public Optional<AppUser> authenticate(String username, String password) {
        return userRepo.findByUsername(username)
                .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()));
    }


    public String createSession(AppUser user) {
        String token = UUID.randomUUID().toString(); // random token
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(sessionExpiresSeconds);
        SessionToken s = new SessionToken(token, user, now, exp);
        sessionRepo.save(s);
        return token;
    }


    public Optional<AppUser> validateSession(String token) {
        if (token == null) return Optional.empty();
        return sessionRepo.findByToken(token)
                .filter(s -> s.getExpiresAt().isAfter(Instant.now()))
                .map(SessionToken::getUser);
    }


    public void removeSession(String token) {
        if (token == null) return;
        sessionRepo.deleteByToken(token);
    }
}
