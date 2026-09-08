package com.mini.credit.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter - rôles officiels")
class JwtAuthenticationFilterRoleTest {

    private static final String SECRET = "1234567890123456789012345678901234567890123456789012345678901234";

    @Mock
    private UserDetailsService userDetailsService;

    private JwtProvider jwtProvider;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtProvider, "jwtExpirationMs", 3600000L);
        filter = new JwtAuthenticationFilter(jwtProvider, userDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jwtWithCompatibilityRoleClaim_shouldNotAuthenticateOrLoadUser() throws Exception {
        doFilter(createToken("compat.chef", "CHEF_BUREAU", "CHEF_BUREAU"));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername("compat.chef");
    }

    @Test
    void parsedJwt_shouldPreserveDistinctAuthoritiesForOperationalRoles() throws Exception {
        assertThat(authenticateAndExtractAuthorities("caissier", "CAISSIER", User.withUsername("caissier").password("n/a")
                .authorities("ROLE_CAISSIER", "OPERATION_CAISSE_CREATE", "CREDIT_DISBURSE").build()))
                .containsExactlyInAnyOrder("ROLE_CAISSIER", "OPERATION_CAISSE_CREATE", "CREDIT_DISBURSE");

        assertThat(authenticateAndExtractAuthorities("controleur", "CONTROLEUR", User.withUsername("controleur").password("n/a")
                .authorities("ROLE_CONTROLEUR", "SESSION_CAISSE_CONTROL_VALIDATE", "GARANTIE_CONTROL").build()))
                .containsExactlyInAnyOrder("ROLE_CONTROLEUR", "SESSION_CAISSE_CONTROL_VALIDATE", "GARANTIE_CONTROL");

        assertThat(authenticateAndExtractAuthorities("chef", "CHEF_BUREAU", User.withUsername("chef").password("n/a")
                .authorities("ROLE_CHEF_BUREAU", "CREDIT_APPROVE", "AUDIT_READ").build()))
                .containsExactlyInAnyOrder("ROLE_CHEF_BUREAU", "CREDIT_APPROVE", "AUDIT_READ");
    }

    private Set<String> authenticateAndExtractAuthorities(String username, String roleClaim, UserDetails userDetails) throws Exception {
        SecurityContextHolder.clearContext();
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        doFilter(createToken(username, roleClaim, null));

        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(java.util.stream.Collectors.toSet());
    }

    private void doFilter(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/secure-resource");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);
    }

    private String createToken(String subject, String role, String legacyRole) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();

        var builder = Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + 3600000L))
                .signWith(key, SignatureAlgorithm.HS512);

        if (legacyRole != null) {
            builder.claim("legacyRole", legacyRole);
        }

        return builder.compact();
    }
}