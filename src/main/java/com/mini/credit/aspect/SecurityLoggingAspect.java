package com.mini.credit.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Aspect pour logger les tentatives d'accès aux endpoints sécurisés.
 * Permet de déboguer rapidement les problèmes d'authentification/autorisation.
 */
@Aspect
@Component
@Slf4j
public class SecurityLoggingAspect {

    /**
     * Log chaque appel à une méthode contrôleur avec les détails de l'utilisateur
     */
    @Before("@annotation(org.springframework.security.access.prepost.PreAuthorize) || " +
            "execution(* com.mini.credit.controller..*(..))")
    public void logSecurityAccess(JoinPoint joinPoint) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            String authorities = auth.getAuthorities().toString();
            log.info("✓ ACCÈS AUTORISÉ : {} → {}.{}() | USER: {} | ROLES/PERMS: {}",
                    joinPoint.getArgs().length > 0 ? joinPoint.getArgs()[0] : "GET",
                    className, methodName, username, authorities);
        } else {
            log.warn("✗ ACCÈS REFUSÉ (pas authentifié) : {}.{}()",
                    className, methodName);
        }
    }
}
