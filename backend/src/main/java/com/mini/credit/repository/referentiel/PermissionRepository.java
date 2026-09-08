package com.mini.credit.repository.referentiel;

import com.mini.credit.entity.referentiel.Permission;
import com.mini.credit.enums.security.PermissionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour gérer les permissions de l'application.
 * Chaque permission représente une action métier autorisée.
 *
 * Étape 4 : Repositories pour le système RBAC
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(PermissionCode code);
}
