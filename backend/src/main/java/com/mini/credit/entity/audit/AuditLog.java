package com.mini.credit.entity.audit;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.enums.security.AuditSeverity;
import com.mini.credit.enums.security.RoleCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "module", length = 50)
    private AuditModule module;

    @Column(nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(nullable = false, length = 100)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private RoleCode roleCode;

    @Column(name = "user_role", length = 50)
    private String userRole;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "site_id")
    private Long siteId;

    @Column(name = "site_libelle", length = 150)
    private String siteLibelle;

    @Column(name = "caisse_id")
    private Long caisseId;

    @Column(name = "session_caisse_id")
    private Long sessionCaisseId;

    @Column(name = "date_action")
    private LocalDateTime dateAction;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "reference_metier", length = 120)
    private String referenceMetier;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "commentaire", length = 1000)
    private String commentaire;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "old_values_json", columnDefinition = "TEXT")
    private String oldValuesJson;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_values_json", columnDefinition = "TEXT")
    private String newValuesJson;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    private AuditSeverity severity;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @PrePersist
    void beforePersistAudit() {
        if (this.dateAction == null) {
            this.dateAction = LocalDateTime.now();
        }
    }
}
