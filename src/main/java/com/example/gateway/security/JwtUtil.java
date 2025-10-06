package com.example.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.expiration:86400}")
    private long expiration;

    public String extractUserName(String token) {
        try {
            // Log token details before parsing
            logger.info("Token to Extract Username: {}", token);
            logger.info("Token Period Count: {}", token.chars().filter(chars -> chars == '.').count());

            // Detailed parsing
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (Exception e) {
            // Log full exception details
            logger.error("Username Extraction Error", e);

            // Log token parts for debugging
            String[] parts = token.split("\\.");
            for (int i = 0; i < parts.length; i++) {
                logger.info("Token Part {}: {}", i, parts[i]);
            }

            throw new RuntimeException("Token parsing failed: " + e.getMessage(), e);
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(String userName){
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userName);
    }

    public String createToken(Map<String, Object> claims, String userName) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userName)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration  * 1000))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token, String userName) {
        try {
            // Add detailed logging
            System.out.println("Token to validate: " + token);
            System.out.println("Token length: " + token.split("\\.").length);

            final String extractedUserName = extractUserName(token);
            return extractedUserName.equals(userName) && !isTokenExpired(token);
        } catch (Exception e) {
            // Log full stack trace for comprehensive debugging
            e.printStackTrace();
            return false;
        }
    }

}