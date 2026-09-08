package com.mini.credit.repository.utilisateur;

import com.mini.credit.entity.referentiel.Utilisateur;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("referentielUtilisateurRepository")
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

	@EntityGraph(attributePaths = {
			"role",
			"role.permissions",
			"role.permissions.permission",
			"employe",
			"employe.agence",
			"employe.site",
			"site"
	})
	@Query("select u from ReferentielUtilisateur u where u.id = :id")
	Optional<Utilisateur> findByIdWithValidationContext(@Param("id") Long id);

	@EntityGraph(attributePaths = {
			"role",
			"role.permissions",
			"role.permissions.permission",
			"employe",
			"employe.agence",
			"employe.site",
			"site"
	})
	@Query("select u from ReferentielUtilisateur u where u.username = :username")
	Optional<Utilisateur> findByUsername(@Param("username") String username);

	@EntityGraph(attributePaths = {
			"role",
			"role.permissions",
			"role.permissions.permission",
			"employe",
			"employe.agence",
			"employe.site",
			"site"
	})
	@Query("select u from ReferentielUtilisateur u where u.username = :username")
	Optional<Utilisateur> findByUsernameWithValidationContext(@Param("username") String username);
}