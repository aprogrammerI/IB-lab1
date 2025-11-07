package com.example.auth_app.web;


import com.example.auth_app.repository.UserRepository;
import com.example.auth_app.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * UserController: only provides profile endpoint now. Encrypted note endpoints removed.
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepo;

    @Value("${app.session.cookieName}")
    private String cookieName;

    public UserController(UserService userService, UserRepository userRepo) {
        this.userService = userService;
        this.userRepo = userRepo;
    }

    // helper: resolve user from cookie token
    private com.example.auth_app.model.AppUser userFromRequest(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (cookieName.equals(c.getName())) {
                return userService.validateSession(c.getValue()).orElse(null);
            }
        }
        return null;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest req) {
        com.example.auth_app.model.AppUser u = userFromRequest(req);
        if (u == null) return ResponseEntity.status(401).body("unauthenticated");
        // return only safe fields
        return ResponseEntity.ok(Map.of("id", u.getId(), "username", u.getUsername(), "email", u.getEmail()));
    }
}
