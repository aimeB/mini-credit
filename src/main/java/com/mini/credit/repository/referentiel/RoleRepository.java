package com.mini.credit.repository.referentiel;

import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.enums.security.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour les roles du systeme RBAC.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(RoleCode code);
}
