package com.mini.credit.entity.referentiel;

import com.mini.credit.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entité de mapping N-N entre rôles et permissions (table de jonction).
 * Cette entité permet de définir quelles permissions appartiennent à quel rôle.
 *
 * Chaque ligne = 1 rôle a 1 permission (relation d'association).
 *
 * Unicité composée garantit : un même rôle ne peut avoir qu'une seule fois la même permission.
 *
 * Étape 2 : Architecture RBAC professionnel
 */
@Entity
@Table(
    name = "role_permissions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "permission_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_role_permission_role"))
    private Role role;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "permission_id", nullable = false, foreignKey = @ForeignKey(name = "fk_role_permission_permission"))
    private Permission permission;
}
