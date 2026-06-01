package com.mini.credit.entity.audit;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.RoleCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entité d'audit des opérations sensibles du système.
 *
 * OWASP A09:2021 - Logging and Monitoring Failures
 * Recommande de journaliser :
 * - Les événements de sécurité (login, accès refusé)
 * - Les opérations sensibles (approbation crédit, ouverture caisse, etc.)
 * - Avec contexte complet (utilisateur, rôle, action, résultat, erreur)
 * - Protection contre les manipulations (immuable après création)
 *
 * Cette entité enregistre TOUTES les actions importantes pour :
 * - Forensique en cas d'incident
 * - Responsabilité et traçabilité
 * - Détection d'anomalies
 * - Conformité réglementaire
 *
 * Étape 2 : Architecture RBAC professionnel avec audit
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    @Column(nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(nullable = false, length = 100)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private RoleCode roleCode;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "old_values_json", columnDefinition = "TEXT")
    private String oldValuesJson;

    @Column(name = "new_values_json", columnDefinition = "TEXT")
    private String newValuesJson;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;
}
