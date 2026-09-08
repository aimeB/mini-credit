package com.mini.credit.security;

import com.mini.credit.entity.referentiel.Permission;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.RolePermission;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.security.PermissionCode;
import com.mini.credit.enums.security.RoleCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtProvider - rôles officiels")
class JwtProviderRoleTest {

    private static final String SECRET = "1234567890123456789012345678901234567890123456789012345678901234";

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtProvider, "jwtExpirationMs", 3600000L);
    }

    @Test
    void generatedJwtForChefBureau_shouldContainRoleChefBureauAuthority() {
        Utilisateur utilisateur = buildUtilisateur(RoleCode.CHEF_BUREAU, PermissionCode.AUDIT_READ);
        utilisateur.setCredentialsVersion(7);

        String token = jwtProvider.generateToken(utilisateur);
        Claims claims = jwtProvider.getAllClaimsFromToken(token);

        assertThat(claims.get("role", String.class)).isEqualTo("CHEF_BUREAU");
        assertThat((List<String>) claims.get("authorities")).contains("ROLE_CHEF_BUREAU", "AUDIT_READ");
        assertThat(jwtProvider.getCredentialsVersionFromToken(token)).isEqualTo(7);
    }

    @Test
    void generatedJwtForGestionnaire_shouldContainMembreReadAuthority() {
        Utilisateur utilisateur = buildUtilisateur(RoleCode.GESTIONNAIRE, PermissionCode.MEMBRE_READ);
        utilisateur.setUsername("gestionnaire.gest");

        String token = jwtProvider.generateToken(utilisateur);
        Claims claims = jwtProvider.getAllClaimsFromToken(token);

        assertThat(claims.get("role", String.class)).isEqualTo("GESTIONNAIRE");
        assertThat((List<String>) claims.get("authorities"))
            .contains("ROLE_GESTIONNAIRE", "MEMBRE_READ");
    }

    @Test
    void tokenWithUnknownRole_shouldBeRefused() {
        String token = createToken("unknown.user", "DIRECTEUR_FANTOME", null, List.of("ROLE_DIRECTEUR_FANTOME"));

        assertThat(jwtProvider.validateToken(token)).isFalse();
    }

    @Test
    void compatibilityRoleClaim_shouldBeRefused() {
        String token = createToken("compat.user", "CHEF_BUREAU", "CHEF_BUREAU", List.of("ROLE_CHEF_BUREAU"));

        assertThat(jwtProvider.validateToken(token)).isFalse();
    }

    private Utilisateur buildUtilisateur(RoleCode roleCode, PermissionCode permissionCode) {
        Permission permission = Permission.builder().code(permissionCode).build();
        Role role = Role.builder().code(roleCode).build();
        role.setPermissions(Set.of(RolePermission.builder().role(role).permission(permission).build()));

        Utilisateur utilisateur = Utilisateur.builder()
                .username("user." + roleCode.name().toLowerCase())
                .motDePasseHash("hash")
                .nomComplet("Utilisateur Test")
                .role(role)
                .build();
        utilisateur.setId(10L);
        return utilisateur;
    }

    private String createToken(String subject, String role, String legacyRole, List<String> authorities) {
        Date now = new Date();
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        var builder = Jwts.builder()
                .setSubject(subject)
                .claim("authorities", authorities)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + 3600000L))
                .signWith(key, SignatureAlgorithm.HS512);

        if (role != null) {
            builder.claim("role", role);
        }
        if (legacyRole != null) {
            builder.claim("legacyRole", legacyRole);
        }

        return builder.compact();
    }
}