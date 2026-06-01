package com.mini.credit.entity.referentiel;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.enums.security.PermissionCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entité représentant une permission granulaire du système RBAC.
 * Les permissions sont assignées aux rôles via RolePermission.
 *
 * Principe : least privilege - chaque permission correspond à une action métier spécifique.
 * Les permissions critiques comme CREDIT_APPROVE et CREDIT_DISBURSE sont séparées
 * pour enforcer la séparation des tâches.
 *
 * Étape 2 : Architecture RBAC professionnel
 */
@Entity
@Table(name = "permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private PermissionCode code;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
