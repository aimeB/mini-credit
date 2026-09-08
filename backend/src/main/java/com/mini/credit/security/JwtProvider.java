package com.mini.credit.security;

import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.security.RoleCode;
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

    public record TokenCompatibilityResult(
            boolean accepted,
            boolean legacyRoleDetected,
            String normalizedRole,
            String rejectionReason
    ) {}

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
            claims.put("credentialsVersion", user.getCredentialsVersion() == null ? 0 : user.getCredentialsVersion());
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
            Claims claims = Jwts.parser().setSigningKey(getSignatureKey()).parseClaimsJws(token).getBody();
            TokenCompatibilityResult compatibility = inspectClaimsCompatibility(claims);
            if (!compatibility.accepted()) {
                log.warn("JWT rejected due to incompatible role claims: {}", compatibility.rejectionReason());
                return false;
            }
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

    public Integer getCredentialsVersionFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object value = claims.get("credentialsVersion");
        if (value == null) {
            return 0;
        }
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public TokenCompatibilityResult inspectTokenCompatibility(String token) {
        return inspectClaimsCompatibility(getAllClaimsFromToken(token));
    }

    private TokenCompatibilityResult inspectClaimsCompatibility(Claims claims) {
        String roleClaim = claims.get("role", String.class);
        String legacyRoleClaim = claims.get("legacyRole", String.class);

        if (legacyRoleClaim != null) {
            return new TokenCompatibilityResult(false, true, null, "legacyRole claim is no longer accepted");
        }

        if (roleClaim != null && !isKnownRoleClaim(roleClaim)) {
            return new TokenCompatibilityResult(false, false, null, "unknown role claim: " + roleClaim);
        }

        return new TokenCompatibilityResult(true, false, roleClaim, null);
    }

    private boolean isKnownRoleClaim(String roleClaim) {
        try {
            RoleCode.valueOf(roleClaim);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private SecretKey getSignatureKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}