package com.mini.credit.config;

import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.ParametreMetier;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.enums.TypeParametre;
import com.mini.credit.enums.CategorieParametre;
import com.mini.credit.enums.PosteEmploye;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.referentiel.RoleRepository;
import com.mini.credit.repository.referentiel.ParametreMetierRepository;
import com.mini.credit.repository.EmployeRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.UUID;
import java.math.BigDecimal;

@Configuration
@Slf4j
public class DataInitializer {

    @Bean
    public CommandLineRunner initializeDefaultUser(UtilisateurRepository utilisateurRepository,
                                                    MembreRepository membreRepository,
                                                    RoleRepository roleRepository,
                                                    PasswordEncoder passwordEncoder,
                                                    ParametreMetierRepository parametreMetierRepository,
                                                    EmployeRepository employeRepository,
                                                    SiteRepository siteRepository) {
        return args -> {
            // Assurer que tous les rôles existent
            Role adminRole = ensureRoleExists(roleRepository, RoleCode.ADMIN);
            Role responsableRole = ensureRoleExists(roleRepository, RoleCode.RESPONSABLE);
            Role agentBureauRole = ensureRoleExists(roleRepository, RoleCode.AGENT_BUREAU);
            Role agentTerrainRole = ensureRoleExists(roleRepository, RoleCode.AGENT_TERRAIN);
            Role caissierRole = ensureRoleExists(roleRepository, RoleCode.CAISSIER);
            Role controleurRole = ensureRoleExists(roleRepository, RoleCode.CONTROLEUR);
            Role memberRole = ensureRoleExists(roleRepository, RoleCode.MEMBER);

            // PHASE 1: Initialiser les paramètres métier
            initializeParametresMétier(parametreMetierRepository);

            // PHASE 3: Initialiser les postes métier et employés
            // Créer un site par défaut
            Site defaultSite = ensureDefaultSiteExists(siteRepository);
            initializeEmployes(employeRepository, defaultSite);

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

            // Créer contrôleur
            createOrUpdateUser(utilisateurRepository, "controleur", "controle123", "controleur@minicredit.com", 
                    "Contrôleur", controleurRole, passwordEncoder);

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

    /**
     * PHASE 1: Initialise les paramètres métier par défaut.
     * Crée les paramètres s'ils n'existent pas déjà.
     */
    private void initializeParametresMétier(ParametreMetierRepository parametreMetierRepository) {
        // Vérifier si les paramètres existent déjà
        if (parametreMetierRepository.existsByCle("MONTANT_CREDIT_MAX")) {
            log.info("Paramètres métier déjà initialisés, passage");
            return;
        }

        log.info("Initialisation des paramètres métier...");

        // TAUX_INTERET
        createParametreIfNotExists(parametreMetierRepository, 
            "TAUX_INTERET_EPARGNE", "Taux d'intérêt épargne",
            TypeParametre.DECIMAL, CategorieParametre.TAUX_INTERET,
            BigDecimal.valueOf(5.0), null, null, "%", 
            "Taux appliqué sur les comptes d'épargne", "5.0");

        createParametreIfNotExists(parametreMetierRepository,
            "TAUX_INTERET_CREDIT_MAX", "Taux d'intérêt crédit maximum",
            TypeParametre.DECIMAL, CategorieParametre.TAUX_INTERET,
            BigDecimal.valueOf(20.0), null, null, "%",
            "Taux maximum autorisé pour les crédits", "20.0");

        createParametreIfNotExists(parametreMetierRepository,
            "TAUX_COMMISSION_AGENT", "Taux commission agent",
            TypeParametre.DECIMAL, CategorieParametre.TAUX_INTERET,
            BigDecimal.valueOf(2.5), null, null, "%",
            "Commission sur recettes collectées par agent", "2.5");

        createParametreIfNotExists(parametreMetierRepository,
            "POURCENTAGE_GARANTIE", "Pourcentage garantie crédit",
            TypeParametre.DECIMAL, CategorieParametre.TAUX_INTERET,
            BigDecimal.valueOf(20.0), null, null, "%",
            "Dépôt de garantie requis pour crédit", "20.0");

        createParametreIfNotExists(parametreMetierRepository,
            "PENALITE_RETARD_JOURNALIERE", "Pénalité retard journalière",
            TypeParametre.DECIMAL, CategorieParametre.FRAIS,
            BigDecimal.valueOf(2500), null, null, "FC/jour",
            "Pénalité appliquée par jour de retard sur crédit", "2500");

        // FRAIS
        createParametreIfNotExists(parametreMetierRepository,
            "FRAIS_ANALYSE_DEMANDE", "Frais d'analyse demande",
            TypeParametre.DECIMAL, CategorieParametre.FRAIS,
            BigDecimal.valueOf(5000), null, null, "FC",
            "Frais à payer pour analyser une demande de crédit", "5000");

        createParametreIfNotExists(parametreMetierRepository,
            "FRAIS_DOSSIER", "Frais de dossier",
            TypeParametre.DECIMAL, CategorieParametre.FRAIS,
            BigDecimal.valueOf(10000), null, null, "FC",
            "Frais de constitution de dossier crédit", "10000");

        createParametreIfNotExists(parametreMetierRepository,
            "FRAIS_RETRAIT", "Frais de retrait épargne",
            TypeParametre.DECIMAL, CategorieParametre.FRAIS,
            BigDecimal.valueOf(500), null, null, "FC",
            "Frais appliqués pour retrait épargne", "500");

        // LIMITES CRÉDIT
        createParametreIfNotExists(parametreMetierRepository,
            "MONTANT_CREDIT_MIN", "Montant crédit minimum",
            TypeParametre.DECIMAL, CategorieParametre.LIMITE,
            BigDecimal.valueOf(1), null, null, "FC",
            "Montant minimum pour une demande de crédit", "1");

        createParametreIfNotExists(parametreMetierRepository,
            "MONTANT_CREDIT_MAX", "Montant crédit maximum",
            TypeParametre.DECIMAL, CategorieParametre.LIMITE,
            BigDecimal.valueOf(100000000), null, null, "FC",
            "Montant maximum pour une demande de crédit", "100000000");

        createParametreIfNotExists(parametreMetierRepository,
            "DUREE_CREDIT_MIN", "Durée crédit minimum",
            TypeParametre.ENTIER, CategorieParametre.LIMITE,
            null, 1L, null, "mois",
            "Durée minimum pour une demande de crédit", "1");

        createParametreIfNotExists(parametreMetierRepository,
            "DUREE_CREDIT_MAX", "Durée crédit maximum",
            TypeParametre.ENTIER, CategorieParametre.LIMITE,
            null, 60L, null, "mois",
            "Durée maximum pour une demande de crédit", "60");

        // SEUILS
        createParametreIfNotExists(parametreMetierRepository,
            "SEUIL_VARIANCE_CAISSE", "Seuil variance caisse",
            TypeParametre.DECIMAL, CategorieParametre.SEUIL,
            BigDecimal.valueOf(500), null, null, "FC",
            "Écart maximum toléré avant investigation", "500");

        createParametreIfNotExists(parametreMetierRepository,
            "SEUIL_DEPENSE_CHEF", "Seuil dépense chef",
            TypeParametre.DECIMAL, CategorieParametre.SEUIL,
            BigDecimal.valueOf(5000), null, null, "FC",
            "Montant max dépense approuvée par chef", "5000");

        createParametreIfNotExists(parametreMetierRepository,
            "SEUIL_DEPENSE_COO", "Seuil dépense COO",
            TypeParametre.DECIMAL, CategorieParametre.SEUIL,
            BigDecimal.valueOf(50000), null, null, "FC",
            "Montant min dépense nécessitant approbation COO", "50000");

        createParametreIfNotExists(parametreMetierRepository,
            "SEUIL_RETRAIT_COO", "Seuil retrait COO",
            TypeParametre.DECIMAL, CategorieParametre.SEUIL,
            BigDecimal.valueOf(100000), null, null, "FC",
            "Montant min retrait exceptionnelneeding COO approval", "100000");

        createParametreIfNotExists(parametreMetierRepository,
            "SEUIL_ECART_INVESTIGATION", "Seuil écart investigation",
            TypeParametre.DECIMAL, CategorieParametre.SEUIL,
            BigDecimal.valueOf(10000), null, null, "FC",
            "Écart important nécessitant validation RCI", "10000");

        createParametreIfNotExists(parametreMetierRepository,
            "PLAFOND_RETRAIT_JOUR", "Plafond retrait jour",
            TypeParametre.DECIMAL, CategorieParametre.LIMITE,
            BigDecimal.valueOf(500000), null, null, "FC",
            "Montant maximum de retrait par jour", "500000");

        // GÉNÉRAL
        createParametreIfNotExists(parametreMetierRepository,
            "DEVISE_DEFAUT", "Devise par défaut",
            TypeParametre.TEXTE, CategorieParametre.GENERAL,
            null, null, "FC", null,
            "Devise utilisée par défaut dans le système", "FC");

        log.info("Paramètres métier initialisés avec succès");
    }

    /**
     * Crée un paramètre s'il n'existe pas déjà.
     */
    private void createParametreIfNotExists(
            ParametreMetierRepository repository,
            String cle, String libelle,
            TypeParametre type, CategorieParametre categorie,
            BigDecimal valeurDecimale, Long valeurEntiere, String valeurTexte, String unite,
            String description, String valeurParDefaut) {
        
        if (!repository.existsByCle(cle)) {
            ParametreMetier param = 
                ParametreMetier.builder()
                .cle(cle)
                .libelle(libelle)
                .typeParametre(type)
                .categorie(categorie)
                .valeurDecimale(valeurDecimale)
                .valeurEntiere(valeurEntiere)
                .valeurTexte(valeurTexte)
                .unite(unite)
                .description(description)
                .valeurParDefaut(valeurParDefaut)
                .actif(true)
                .modifiable(true)
                .build();
            
            repository.save(param);
            log.debug("Paramètre {} créé", cle);
        }
    }

    /**
     * PHASE 3: Assure qu'un site par défaut existe.
     * Crée le site "Siège Social" s'il n'existe pas.
     */
    private Site ensureDefaultSiteExists(SiteRepository siteRepository) {
        return siteRepository.findByCodeSite("SIEGE")
                .orElseGet(() -> {
                    Site site = Site.builder()
                            .codeSite("SIEGE")
                            .nomSite("Siège Social")
                            .adresse("Kinshasa")
                            .commune("Gombe")
                            .ville("Kinshasa")
                            .actif(true)
                            .build();
                    siteRepository.save(site);
                    log.info("Default site (Siège Social) created");
                    return site;
                });
    }

    /**
     * PHASE 3: Initialise les postes métier et employés par défaut.
     * Crée les employés de base s'ils n'existent pas.
     */
    private void initializeEmployes(EmployeRepository employeRepository, Site defaultSite) {
        // Vérifier si les employés existent déjà
        if (employeRepository.existsByMatricule("MAT-001")) {
            log.info("Employés déjà initialisés, passage");
            return;
        }

        log.info("Initialisation des employés...");

        // Chef de Bureau
        createEmployeIfNotExists(employeRepository, 
            "MAT-001", "Chief", "Bureau", new BigDecimal("500000"));

        // Gestionnaire
        createEmployeIfNotExists(employeRepository,
            "MAT-002", "Manager", "Terrain", new BigDecimal("400000"));

        // Contrôleur
        createEmployeIfNotExists(employeRepository,
            "MAT-003", "Controller", "Caisse", new BigDecimal("350000"));

        // Caissier
        createEmployeIfNotExists(employeRepository,
            "MAT-004", "Cashier", "Main", new BigDecimal("300000"));

        // Agent Terrain
        createEmployeIfNotExists(employeRepository,
            "MAT-005", "Agent", "Terrain", new BigDecimal("250000"));

        log.info("Employés initialisés avec succès");
    }

    /**
     * Crée un employé s'il n'existe pas déjà.
     */
    private void createEmployeIfNotExists(
            EmployeRepository employeRepository,
            String matricule, String prenom, String nom, BigDecimal salaire) {
        
        if (!employeRepository.existsByMatricule(matricule)) {
            Employe employe = Employe.builder()
                    .matricule(matricule)
                    .prenom(prenom)
                    .nom(nom)
                    .salaireBase(salaire)
                    .dateEmbauche(LocalDate.now())
                    .actif(true)
                    .build();
            
            employeRepository.save(employe);
            log.debug("Employé {} créé avec salaire {}", matricule, salaire);
        }
    }
}

