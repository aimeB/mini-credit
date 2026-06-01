package com.mini.credit.config;

import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.referentiel.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.UUID;

@Configuration
@Slf4j
public class DataInitializer {

    @Bean
    public CommandLineRunner initializeDefaultUser(UtilisateurRepository utilisateurRepository,
                                                    MembreRepository membreRepository,
                                                    RoleRepository roleRepository,
                                                    PasswordEncoder passwordEncoder) {
        return args -> {
            // Assurer que tous les rôles existent
            Role adminRole = ensureRoleExists(roleRepository, RoleCode.ADMIN);
            Role responsableRole = ensureRoleExists(roleRepository, RoleCode.RESPONSABLE);
            Role agentBureauRole = ensureRoleExists(roleRepository, RoleCode.AGENT_BUREAU);
            Role agentTerrainRole = ensureRoleExists(roleRepository, RoleCode.AGENT_TERRAIN);
            Role caissierRole = ensureRoleExists(roleRepository, RoleCode.CAISSIER);
            Role memberRole = ensureRoleExists(roleRepository, RoleCode.MEMBER);

            // Créer admin
            createOrUpdateUser(utilisateurRepository, "admin", "admin123", "admin@minicredit.com", 
                    "Administrator", adminRole, passwordEncoder);

            // Créer responsable
            createOrUpdateUser(utilisateurRepository, "responsable", "resp123", "responsable@minicredit.com", 
                    "Responsable Bureau", responsableRole, passwordEncoder);

            // Créer agent bureau
            createOrUpdateUser(utilisateurRepository, "agentbureau", "bureau123", "agentbureau@minicredit.com", 
                    "Agent Bureau", agentBureauRole, passwordEncoder);

            // Créer agent terrain
            createOrUpdateUser(utilisateurRepository, "agentterrain", "terrain123", "agentterrain@minicredit.com", 
                    "Agent Terrain", agentTerrainRole, passwordEncoder);

            // Créer caissier
            createOrUpdateUser(utilisateurRepository, "caissier", "caisse123", "caissier@minicredit.com", 
                    "Caissier", caissierRole, passwordEncoder);

            // Créer membre avec son profil Membre
            Membre defaultMembre = null;
            var memberOptional = utilisateurRepository.findByUsername("member");
            if (memberOptional.isPresent() && memberOptional.get().getMembre() != null) {
                defaultMembre = memberOptional.get().getMembre();
            }

            if (defaultMembre == null) {
                defaultMembre = Membre.builder()
                        .codeMembre("MBR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .nom("Test")
                        .postnom("Member")
                        .prenom("")
                        .nomComplet("Test Member")
                        .dateAdhesion(LocalDate.now())
                        .statut(StatutMembre.ACTIF)
                        .ville("Kinshasa")
                        .adresse("Address Test")
                        .build();
                defaultMembre = membreRepository.save(defaultMembre);
                log.info("Default member profile created");
            }

            // Créer ou mettre à jour utilisateur member
            if (memberOptional.isEmpty()) {
                Utilisateur member = Utilisateur.builder()
                        .username("member")
                        .motDePasseHash(passwordEncoder.encode("member123"))
                        .email("member@minicredit.com")
                        .nomComplet("Test Member")
                        .role(memberRole)
                        .membre(defaultMembre)
                        .actif(true)
                        .isEnabled(true)
                        .isLocked(false)
                        .build();

                utilisateurRepository.save(member);
                log.info("Default member user created");
            } else {
                Utilisateur member = memberOptional.get();
                if (!passwordEncoder.matches("member123", member.getMotDePasseHash()) || !member.getActif()) {
                    member.setMotDePasseHash(passwordEncoder.encode("member123"));
                    member.setActif(true);
                    member.setIsEnabled(true);
                }
                if (member.getMembre() == null || !member.getMembre().getId().equals(defaultMembre.getId())) {
                    member.setMembre(defaultMembre);
                }
                utilisateurRepository.save(member);
                log.info("Default member user verified and linked to member profile");
            }
        };
    }

    private void createOrUpdateUser(UtilisateurRepository utilisateurRepository, String username, 
            String password, String email, String nomComplet, Role role, PasswordEncoder passwordEncoder) {
        var userOptional = utilisateurRepository.findByUsername(username);
        
        if (userOptional.isEmpty()) {
            Utilisateur user = Utilisateur.builder()
                    .username(username)
                    .motDePasseHash(passwordEncoder.encode(password))
                    .email(email)
                    .nomComplet(nomComplet)
                    .role(role)
                    .actif(true)
                    .isEnabled(true)
                    .isLocked(false)
                    .build();

            utilisateurRepository.save(user);
            log.info("Default user {} created", username);
        } else {
            Utilisateur user = userOptional.get();
            if (!passwordEncoder.matches(password, user.getMotDePasseHash()) || !user.getActif()) {
                user.setMotDePasseHash(passwordEncoder.encode(password));
                user.setActif(true);
                user.setIsEnabled(true);
                utilisateurRepository.save(user);
                log.info("Default user {} password reset and account activated", username);
            }
        }
    }

    private Role ensureRoleExists(RoleRepository roleRepository, RoleCode roleCode) {
        return roleRepository.findByCode(roleCode)
                .orElseGet(() -> {
                    Role role = Role.builder()
                            .code(roleCode)
                            .libelle(roleCode.getLibelle())
                            .description(roleCode.getDescription())
                            .isActive(true)
                            .build();
                    roleRepository.save(role);
                    log.info("Role {} created", roleCode);
                    return role;
                });
    }
}

