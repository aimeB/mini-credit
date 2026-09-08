package com.mini.credit.service.audit;

import com.mini.credit.enums.security.AuditAction;

import java.lang.annotation.*;

/**
 * Annotation pour marquer une methode comme devant etre auditee automatiquement.
 *
 * Utilisee par un AOP aspect pour logger automatiquement :
 * - L'entree/sortie d'une methode
 * - Les parametres
 * - Le resultat et les exceptions
 * - L'utilisateur actuel
 *
 * Exemple :
 * @Auditable(action = AuditAction.CREDIT_APPROVED, entityType = "DemandeCredit")
 * public CreditResponse approve(Long demandeId, ApprobationRequest request) { ... }
 *
 * Étape 8 : Audit automation
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * Action auditee
     */
    AuditAction action();

    /**
     * Type d'entite (ex: "DemandeCredit", "Caisse", "Membre")
     */
    String entityType();

    /**
     * Le nom du parametre methode qui contient l'ID de l'entite
     * Par defaut, c'est le premier parametre Long
     */
    String entityIdParameter() default "";
}
