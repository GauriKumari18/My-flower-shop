package com.flowershop.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT SERVICE
 * -----------
 * Handles all JWT token operations:
 * - generateToken(userDetails) → creates a signed JWT
 * - extractUsername(token) → gets email from token
 * - isTokenValid(token, userDetails) → checks signature + expiry + user match
 *
 * Token structure (HS256 signed):
 * Header : { "alg": "HS256" }
 * Payload : { "sub": "email", "role": "ADMIN", "iat": ..., "exp": ... }
 * Signature: HMAC-SHA256(header + payload + secret)
 *
 * Uses jjwt 0.11.5 API (already declared in pom.xml).
 * Secret key is injected from application.yml → jwt.secret
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey; // must be ≥ 32 characters for HS256

    @Value("${jwt.expiration}")
    private long jwtExpiration; // milliseconds, e.g. 86400000 = 24h

    // ─── Token Generation ─────────────────────────────────────────────────────

    /**
     * Generates a JWT for the given user (no extra claims).
     * Subject = email, expiry = jwt.expiration from config.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a JWT with additional claims (e.g. role).
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername()) // email as subject
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ─── Token Validation ─────────────────────────────────────────────────────

    /**
     * Returns true if:
     * 1. Token subject (email) matches the given UserDetails username
     * 2. Token has not expired
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    // ─── Claim Extraction ─────────────────────────────────────────────────────

    /** Extracts the email (subject) stored in the token. */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Builds the signing Key from the plain-text secret in application.yml.
     * UTF-8 byte array must be ≥ 32 bytes (256 bits) for HS256 — enforced by jjwt.
     */
    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
