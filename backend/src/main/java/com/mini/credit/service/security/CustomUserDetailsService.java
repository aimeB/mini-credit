package com.mini.credit.service.security;

import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service Spring Security pour charger les utilisateurs depuis la BD.
 *
 * Intégration complète :
 * - Charge l'utilisateur avec son rôle (EAGER fetch)
 * - Retourne les authorities (rôle + permissions)
 * - Vérifie enabled/locked/credentialsNonExpired
 *
 * Étape 6 : Spring Security configuration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    /**
     * Charge un utilisateur par son username pour l'authentification.
     * Spring Security l'appelle lors du login.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.warn("🔍 RECHERCHE utilisateur: {}", username);
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("❌ UTILISATEUR INTROUVABLE: {}", username);
                    return new UsernameNotFoundException("Utilisateur non trouvé : " + username);
                });

        log.warn("✓ UTILISATEUR TROUVÉ: username={} | role={} | enabled={} | locked={}", 
                username, 
                utilisateur.getRole().getCode(),
                utilisateur.getIsEnabled(),
                utilisateur.getIsLocked());
        log.warn("  Authorities (avant return): {}", utilisateur.getAuthorities());
        return utilisateur;
    }
}
