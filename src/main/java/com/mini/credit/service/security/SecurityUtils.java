package com.mini.credit.service.security;

import com.mini.credit.entity.referentiel.Utilisateur;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utilitaires de sécurité pour accéder au contexte Spring Security.
 *
 * Fournit :
 * - L'utilisateur actuellement authentifié
 * - Vérifications de rôle et permission
 * - IP client, User-Agent, etc.
 *
 * Utilisé par les services et controllers pour :
 * - Audit
 * - Scope de données
 * - Vérifications métier supplémentaires
 *
 * Étape 6 : Spring Security configuration
 */
@Component
public class SecurityUtils {

    /**
     * Récupère l'utilisateur actuellement authentifié
     * @return Utilisateur ou null si pas d'authentification
     */
    public static Utilisateur getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Utilisateur) {
            return (Utilisateur) auth.getPrincipal();
        }
        return null;
    }

    /**
     * Récupère l'utilisateur, lève une exception si pas authentifié
     */
    public static Utilisateur getCurrentUserOrThrow() {
        Utilisateur user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("Pas d'utilisateur authentifié dans le contexte");
        }
        return user;
    }

    /**
     * Récupère le username de l'utilisateur courant
     */
    public static String getCurrentUsername() {
        Utilisateur user = getCurrentUser();
        return user != null ? user.getUsername() : "ANONYMOUS";
    }

    /**
     * Récupère le rôle de l'utilisateur courant
     */
    public static String getCurrentRoleCode() {
        Utilisateur user = getCurrentUser();
        return user != null && user.getRole() != null
                ? user.getRole().getCode().name()
                : "ANONYMOUS";
    }

    /**
     * Vérifie si l'utilisateur a un rôle donné
     */
    public static boolean hasRole(String roleCode) {
        Utilisateur user = getCurrentUser();
        return user != null && user.getRole() != null
                && user.getRole().getCode().name().equals(roleCode);
    }

    /**
     * Vérifie si l'utilisateur a une permission donnée
     */
    public static boolean hasPermission(String permissionCode) {
        Utilisateur user = getCurrentUser();
        if (user == null) {
            return false;
        }
        return user.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(permissionCode));
    }

    /**
     * Récupère l'ID de l'utilisateur courant
     */
    public static Long getCurrentUserId() {
        Utilisateur user = getCurrentUser();
        return user != null ? user.getId() : null;
    }
}
