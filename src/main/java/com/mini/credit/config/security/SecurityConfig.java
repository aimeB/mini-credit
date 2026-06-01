package com.mini.credit.config.security;

import com.mini.credit.service.security.CustomUserDetailsService;
import com.mini.credit.security.JwtAuthenticationFilter;
import com.mini.credit.security.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration Spring Security RBAC professionnel + JWT.
 *
 * OWASP recommandations :
 * - AuthenticationProvider (DaoAuthenticationProvider) pour vérifier credentiels
 * - PasswordEncoder (BCrypt) pour hasher les mots de passe
 * - @EnableMethodSecurity pour activer @PreAuthorize sur les méthodes
 * - SessionCreationPolicy.STATELESS pour JWT (pas de session côté serveur)
 * - JwtAuthenticationFilter pour valider les tokens JWT
 * - @EnableAspectJAutoProxy pour les annotations @Auditable
 *
 * Étape 6 : Spring Security configuration
 * Étape 8 : Audit automation
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtProvider jwtProvider;

    /**     * Bean pour le filtre JWT
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider, customUserDetailsService);
    }

    /**
     * PasswordEncoder avec BCrypt (recommandé OWASP)
     * Force : facteur 12 (plus sûr mais plus lent)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * ObjectMapper pour la sérialisation JSON (AuditableAspect)
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * AuthenticationManager - nécessaire pour AuthService.login()
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * AuthenticationProvider : charge les utilisateurs et valide les mots de passe
     * Configuration directe dans le SecurityFilterChain pour Spring Security 6.x
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/audit/**").hasAnyRole("ADMIN", "RESPONSABLE", "AGENT_BUREAU")
                .requestMatchers("/api/health", "/api/status").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl("/api/auth/logout-success")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
            );

        return http.build();
    }
}
