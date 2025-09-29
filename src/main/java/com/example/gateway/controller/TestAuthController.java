package com.example.gateway.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class TestAuthController {

    @GetMapping("/test")
    public Map<String, Object> testAuth(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        return Map.of(
                "message", "Authentication successful!",
                "username", jwt.getClaim("preferred_username"),
                "email", jwt.getClaim("email"),
                "roles", authentication.getAuthorities(),
                "subject", jwt.getSubject()
        );
    }

    @GetMapping("/user-info")
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "sub", jwt.getSubject(),
                "preferred_username", jwt.getClaim("preferred_username"),
                "email", jwt.getClaim("email"),
                "name", jwt.getClaim("name"),
                "given_name", jwt.getClaim("given_name"),
                "family_name", jwt.getClaim("family_name"),
                "realm_access", jwt.getClaim("realm_access")
        );
    }
}