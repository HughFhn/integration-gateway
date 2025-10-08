package com.example.gateway.controller;

import com.example.gateway.security.JwtUtil;
import com.example.gateway.security.DetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private DetailsService userDetailsService;

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest authRequest) {
        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.username,
                            authRequest.password
                    )
            );

            // Get user details
            final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.username);

            // Extract roles
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // Generate token
            final String jwt = jwtUtil.generateTokenWithRoles(userDetails.getUsername(), roles);

            logger.info("Authentication successful for user: {}", authRequest.username);
            logger.info("Roles: {}", roles);

            return ResponseEntity.ok(new AuthResponse("success", jwt));
        } catch (BadCredentialsException e) {
            logger.error("Authentication failed for user: {}", authRequest.username);
            return ResponseEntity.status(401).body(new AuthResponse("error", "Invalid username or password"));
        }
    }

    // Static inner classes with public fields instead of getter/setter methods
    public static class AuthRequest {
        public String username;
        public String password;
    }

    public static class AuthResponse {
        public String status;
        public String token;

        public AuthResponse(String status, String token) {
            this.status = status;
            this.token = token;
        }
    }
}
