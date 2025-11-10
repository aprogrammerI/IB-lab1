package com.example.auth_app.service;

import com.example.auth_app.model.AppUser;
import com.example.auth_app.model.SessionToken;
import com.example.auth_app.repository.SessionRepository;
import com.example.auth_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final SessionRepository sessionRepo;

    @Value("${app.session.expiresSeconds}")
    private long sessionExpiresSeconds;

    // Simplified secure password hashing constants
    private static final int HASH_ROUNDS = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

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

        // Hash password with salt
        String hashed = hashPassword(password);
        AppUser u = new AppUser(username, email, hashed);
        return userRepo.save(u);
    }

    public Optional<AppUser> authenticate(String username, String password) {
        return userRepo.findByUsername(username)
                .filter(u -> verifyPassword(password, u.getPasswordHash()));
    }

    public String createSession(AppUser user) {
        String token = UUID.randomUUID().toString(); // random token
        var now = java.time.Instant.now();
        var exp = now.plusSeconds(sessionExpiresSeconds);
        SessionToken s = new SessionToken(token, user, now, exp);
        sessionRepo.save(s);
        return token;
    }

    public Optional<AppUser> validateSession(String token) {
        if (token == null) return Optional.empty();
        return sessionRepo.findByToken(token)
                .filter(s -> s.getExpiresAt().isAfter(java.time.Instant.now()))
                .map(SessionToken::getUser);
    }

    public void removeSession(String token) {
        if (token == null) return;
        sessionRepo.deleteByToken(token);
    }

    // Simplified secure password hashing with salt and multiple rounds
    private String hashPassword(String password) {
        try {
            // Generate random salt
            byte[] salt = new byte[16];
            RANDOM.nextBytes(salt);

            // Hash password with salt using multiple rounds
            byte[] hash = hashWithSaltAndRounds(password.getBytes(StandardCharsets.UTF_8), salt, HASH_ROUNDS);

            // Format: salt:hash (both base64 encoded)
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);
            return saltB64 + ":" + hashB64;

        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    private boolean verifyPassword(String password, String storedHash) {
        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 2) return false;

            // Decode salt and stored hash
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] storedHashBytes = Base64.getDecoder().decode(parts[1]);

            // Hash the input password with the same salt
            byte[] computedHash = hashWithSaltAndRounds(password.getBytes(StandardCharsets.UTF_8), salt, HASH_ROUNDS);

            // Compare hashes
            return MessageDigest.isEqual(storedHashBytes, computedHash);

        } catch (Exception e) {
            return false;
        }
    }

    private byte[] hashWithSaltAndRounds(byte[] password, byte[] salt, int rounds) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        // Start with salt + password
        md.update(salt);
        byte[] hash = md.digest(password);

        // Perform additional rounds
        for (int i = 1; i < rounds; i++) {
            md.reset();
            md.update(hash);
            hash = md.digest();
        }

        return hash;
    }
}