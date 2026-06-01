package com.mini.credit.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, UserDetailsService userDetailsService) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        log.warn("⚠️  REQUÊTE REÇUE: {} {} [IP: {}]", request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Endpoints publics qui ne nécessitent pas de JWT
        String requestURI = request.getRequestURI();
        if (isPublicEndpoint(requestURI)) {
            log.warn("ℹ️ ENDPOINT PUBLIC: {} | Pas de validation JWT requise", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = extractJwtFromRequest(request);
            if (jwt != null && jwtProvider.validateToken(jwt)) {
                String username = jwtProvider.getUsernameFromToken(jwt);
                log.warn("🔐 JWT VALIDE: username={}", username);
                
                log.warn("👤 CHARGEMENT utilisateur: {}", username);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                log.warn("👤 UTILISATEUR CHARGÉ: {} | Authorities COUNT: {} | Details: {}", 
                        username, userDetails.getAuthorities().size(), userDetails.getAuthorities());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.warn("✓ AUTH_OK: {} | Context set | Authorities: {}",
                        username, SecurityContextHolder.getContext().getAuthentication().getAuthorities());
            } else if (jwt != null) {
                log.warn("❌ JWT INVALIDE OU EXPIRÉ");
            } else {
                log.warn("ℹ️ PAS DE JWT dans Authorization header");
            }
        } catch (Exception e) {
            log.error("❌ ERREUR JWT AUTHENTICATION: {}", e.getMessage(), e);
        }
        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Endpoints publics qui ne nécessitent pas de JWT
     */
    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/api/auth/login") ||
               requestURI.startsWith("/api/auth/register") ||
               requestURI.startsWith("/api/auth/activate") ||
               requestURI.startsWith("/api/health") ||
               requestURI.startsWith("/api/status") ||
               requestURI.startsWith("/v3/api-docs") ||
               requestURI.startsWith("/swagger-ui");
    }
}