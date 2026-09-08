package com.mini.credit.repository.audit;

import com.mini.credit.entity.audit.AuditLog;
import com.mini.credit.enums.security.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour l'audit des opérations sensibles.
 * Permet de tracer et rechercher les événements système.
 *
 * OWASP A09:2021 - Logging and Monitoring Failures
 * Journalise les accès, modifications, erreurs de sécurité, etc.
 *
 * Étape 4 : Repositories pour le système RBAC avec audit
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    /**
     * Récupère tous les logs d'un utilisateur
     */
    Page<AuditLog> findByUsername(String username, Pageable pageable);

    /**
     * Récupère tous les logs d'une action donnée (ex: LOGIN_FAILURE)
     */
    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    /**
     * Récupère les logs d'un type d'entité (ex: DemandeCredit)
     */
    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    /**
     * Récupère les logs d'une entité spécifique
     */
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByDateActionAsc(String entityType, Long entityId);

    /**
     * Version paginée pour éviter les retours massifs.
     */
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    /**
     * Recherche les opérations échouées
     */
    @Query("SELECT al FROM AuditLog al WHERE al.success = false ORDER BY al.dateCreation DESC")
    Page<AuditLog> findFailedOperations(Pageable pageable);

    /**
     * Recherche les accès refusés (sécurité)
     */
    @Query("SELECT al FROM AuditLog al WHERE al.action IN ('ACCESS_DENIED', 'PERMISSION_DENIED') ORDER BY al.dateCreation DESC")
    Page<AuditLog> findSecurityEvents(Pageable pageable);

    /**
     * Recherche entre deux dates
     */
    List<AuditLog> findByDateCreationBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Recherche les actions d'un utilisateur entre deux dates
     */
    @Query("SELECT al FROM AuditLog al " +
           "WHERE al.username = :username " +
           "AND al.dateCreation BETWEEN :startDate AND :endDate " +
           "ORDER BY al.dateCreation DESC")
    Page<AuditLog> findUserActionsBetween(
            @Param("username") String username,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
