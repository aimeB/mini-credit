package com.mini.credit.service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect AOP pour auditer automatiquement les methodes marquees @Auditable.
 *
 * Intercepte :
 * - Execution reussie : log avec newValues
 * - Execution echouee : log avec errorMessage
 * - Parametres : capture pour oldValues
 *
 * Etape 8 : Audit automation
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditableAspect {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        Utilisateur currentUser = SecurityUtils.getCurrentUser();

        // Chercher l'ID d'entite
        Long entityId = extractEntityId(joinPoint, auditable, args);

        // Capturer les valeurs entrant (oldValues = parametres d'entree)
        String oldValuesJson = null;
        try {
            if (args.length > 0) {
                // Pour les methodes de creation, oldValues = null
                // Pour les updates, oldValues = l'objet original (param 0)
                oldValuesJson = objectMapper.writeValueAsString(args.length > 1 ? args[1] : args[0]);
            }
        } catch (Exception e) {
            log.warn("Impossible de serializer oldValues pour {}", methodName, e);
        }

        try {
            // Executer la methode
            Object result = joinPoint.proceed();

            // Capturer les valeurs sortant (newValues = retour de la methode)
            String newValuesJson = null;
            try {
                if (result != null) {
                    newValuesJson = objectMapper.writeValueAsString(result);
                }
            } catch (Exception e) {
                log.warn("Impossible de serializer newValues pour {}", methodName, e);
            }

            // Log succes
            auditService.logWithValues(
                    auditable.action(),
                    auditable.entityType(),
                    entityId,
                    true,
                    methodName + " execute avec succes",
                    oldValuesJson,
                    newValuesJson,
                    null
            );

            return result;

        } catch (Throwable e) {
            // Log echec
            auditService.logWithValues(
                    auditable.action(),
                    auditable.entityType(),
                    entityId,
                    false,
                    methodName + " a echoue",
                    oldValuesJson,
                    null,
                    e.getMessage()
            );

            throw e;
        }
    }

    /**
     * Extraire l'ID d'entite depuis les parametres de la methode.
     *
     * Strategie :
     * 1. Si entityIdParameter est specifie, chercher ce parametre par nom
     * 2. Sinon, prendre le premier parametre de type Long
     * 3. Sinon, essayer le deuxieme parametre de type Long
     *
     * @return ID d'entite ou null si non trouvable
     */
    private Long extractEntityId(ProceedingJoinPoint joinPoint, Auditable auditable, Object[] args) {
        String paramName = auditable.entityIdParameter();

        // Strategie 1 : parametre nomme
        if (paramName != null && !paramName.isEmpty()) {
            String[] paramNames = getParameterNames(joinPoint);
            for (int i = 0; i < paramNames.length; i++) {
                if (paramNames[i].equals(paramName) && args[i] instanceof Long) {
                    return (Long) args[i];
                }
            }
        }

        // Strategie 2 : premier Long
        if (args.length > 0 && args[0] instanceof Long) {
            return (Long) args[0];
        }

        // Strategie 3 : deuxieme Long (cas des methodes update(id, request))
        if (args.length > 1 && args[1] instanceof Long) {
            return (Long) args[1];
        }

        return null;
    }

    /**
     * Utilitaire pour obtenir les noms des parametres d'une methode (via reflection).
     * Simplifie : ne traite que les premiers noms connus.
     */
    private String[] getParameterNames(ProceedingJoinPoint joinPoint) {
        // NOTE: Pour une impl complete, utiliser spring-core CodeGeneratingMethodParameterNameDiscoverer
        // Ici, on retourne un array vide pour simplifier
        return new String[0];
    }
}
