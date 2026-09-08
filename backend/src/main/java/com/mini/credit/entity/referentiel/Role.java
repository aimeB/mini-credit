package com.mini.credit.entity.referentiel;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.enums.security.RoleCode;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Entité représentant un rôle du système RBAC.
 * Chaque rôle possède un ensemble de permissions via RolePermission.
 *
 * RBAC professionnel NIST + OWASP (Étape 2)
 * - code : énumération RoleCode pour typage fort
 * - permissions : relation lazy vers RolePermission (charger à la demande)
 * - libelle/description : pour l'UI et documentation
 */
@Entity
@Table(name = "role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "code_role", nullable = false, unique = true, length = 50)
    private RoleCode code;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Permissions associées à ce rôle
     * EAGER-loaded pour que les permissions soient disponibles lors de l'authentification
     */
    @OneToMany(mappedBy = "role", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RolePermission> permissions = new HashSet<>();
}
