package com.mini.credit.security;

import com.mini.credit.entity.referentiel.Utilisateur;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Provider for generating and validating JWT tokens.
 * Etape 6: Spring Security with JWT
 */
@Component
@Slf4j
public class JwtProvider {

    @Value("${app.security.jwt.secret:df7d3c8f9e2a1b4c6d7e8f9a0b1c2d3e4f5g6h7i8j9k0l1m2n3o4p5q6r7s8t9u0}")
    private String jwtSecret;

    @Value("${app.security.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", userDetails.getAuthorities()
                .stream()
                .map(auth -> auth.getAuthority())
                .toList());

        if (userDetails instanceof Utilisateur) {
            Utilisateur user = (Utilisateur) userDetails;
            if (user.getRole() != null) {
                claims.put("role", user.getRole().getCode().name());
            }
            claims.put("userId", user.getId());
            claims.put("username", user.getUsername());
        }

        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSignatureKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(getSignatureKey()).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.debug("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(getSignatureKey())
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSignatureKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}