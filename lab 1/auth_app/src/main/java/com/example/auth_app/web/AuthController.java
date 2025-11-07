package com.example.auth_app.web;



import com.example.auth_app.dto.LoginRequest;
import com.example.auth_app.dto.RegisterRequest;
import com.example.auth_app.model.AppUser;
import com.example.auth_app.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
public class AuthController {

    private final UserService userService;

    @Value("${app.session.cookieName}")
    private String cookieName;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            AppUser u = userService.register(req.getUsername(), req.getEmail(), req.getPassword());
            return ResponseEntity.ok("Registered user id=" + u.getId());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletResponse resp) {
        var opt = userService.authenticate(req.getUsername(), req.getPassword());
        if (opt.isEmpty()) return ResponseEntity.status(401).body("invalid credentials");
        AppUser u = opt.get();
        String token = userService.createSession(u);


        Cookie cookie = new Cookie(cookieName, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) 86400);
        resp.addCookie(cookie);

        return ResponseEntity.ok("login successful");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest req, HttpServletResponse resp) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (cookieName.equals(c.getName())) {
                    userService.removeSession(c.getValue());
                    c.setMaxAge(0);
                    c.setPath("/");
                    resp.addCookie(c);
                    break;
                }
            }
        }
        return ResponseEntity.ok("logged out");
    }
}

