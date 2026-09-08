package com.mini.credit.repository.referentiel;

import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour gérer le mapping rôle-permissions.
 * Fournit les méthodes pour lister les permissions d'un rôle.
 *
 * Étape 4 : Repositories pour le système RBAC
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    
    /**
     * Récupère toutes les permissions d'un rôle
     */
    List<RolePermission> findByRole(Role role);
    
    /**
     * Vérifier si un rôle a une permission donnée
     */
    @Query("SELECT CASE WHEN COUNT(rp) > 0 THEN true ELSE false END " +
           "FROM RolePermission rp " +
           "WHERE rp.role = :role AND rp.permission.code = :permissionCode")
    boolean hasPermission(@Param("role") Role role, @Param("permissionCode") String permissionCode);
    
    /**
     * Récupère l'assignment d'une permission à un rôle
     */
    Optional<RolePermission> findByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
