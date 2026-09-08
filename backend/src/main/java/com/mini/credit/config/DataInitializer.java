package com.mini.credit.config;

import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.ParametreMetier;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.enums.CategorieParametre;
import com.mini.credit.enums.TypeParametre;
import com.mini.credit.repository.AgenceRepository;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.referentiel.RoleRepository;
import com.mini.credit.repository.referentiel.ParametreMetierRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@Slf4j
@Profile("!test")
public class DataInitializer {

    private static final List<RoleCode> OFFICIAL_BOOTSTRAP_ROLES = List.of(
        RoleCode.ADMIN,
        RoleCode.CHEF_BUREAU,
        RoleCode.GESTIONNAIRE,
        RoleCode.AGENT_TERRAIN,
        RoleCode.CAISSIER,
        RoleCode.CONTROLEUR,
        RoleCode.COO,
        RoleCode.RCI,
        RoleCode.GERANT_GENERAL,
        RoleCode.MEMBER
    );

    /**
     * Bootstrap minimal au démarrage.
     * Crée uniquement : rôles, paramètres métier, utilisateur ADMIN.
     * Agence / Site / Membres / Employés → à créer manuellement via l'interface.
     */
    @Bean
    public CommandLineRunner initializeDefaultUser(
            UtilisateurRepository utilisateurRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            ParametreMetierRepository parametreMetierRepository,
            AgenceRepository agenceRepository,
            JdbcTemplate jdbcTemplate) {
        return args -> {
            // ÉTAPE 1 : Rôles (obligatoires pour la sécurité)
            Role adminRole = ensureRoleExists(roleRepository, RoleCode.ADMIN);
            for (RoleCode roleCode : OFFICIAL_BOOTSTRAP_ROLES) {
                if (roleCode != RoleCode.ADMIN) {
                    ensureRoleExists(roleRepository, roleCode);
                }
            }
            log.info("Rôles bootstrap : OK (10 rôles actifs)");

            // ÉTAPE 2 : Paramètres métier (requis pour les règles de gestion)
            initializeParametresMétier(parametreMetierRepository);

            // ÉTAPE 3 : Utilisateur ADMIN initial (seul compte créé automatiquement)
                createUserIfMissing(utilisateurRepository, "admin", "admin123",
                    "admin@minicredit.com", "Administrateur Système", adminRole, passwordEncoder);
            log.info("Utilisateur ADMIN bootstrap : OK");

            // ÉTAPE 4 : Correction sécurisée données corrompues (upgrade depuis ancienne version)
            // N'est exécutée que si des sites avec agence_id invalide existent déjà en base.
            fixCorruptedSitesIfNeeded(agenceRepository, jdbcTemplate);

            log.info("=================================================");
            log.info("Bootstrap terminé. Prêt pour tests manuels E2E.");
            log.info("Agence / Site / Membres → à créer via l'interface.");
            log.info("=================================================");
        };
    }

    private Utilisateur createUserIfMissing(UtilisateurRepository utilisateurRepository, String username,
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
            return user;
        }

        log.info("Default user {} already exists - bootstrap keeps existing credentials unchanged", username);
        return userOptional.get();
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
            log.info("Paramètres métier déjà initialisés, vérification des paramètres manquants");
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
            "RATIO_GARANTIE_MATERIELLE_MINIMUM", "Ratio minimum garantie matérielle",
            TypeParametre.DECIMAL, CategorieParametre.SEUIL,
            BigDecimal.valueOf(200.0), null, null, "%",
            "Ratio minimum de couverture pour les garanties matérielles", "200.0");

        createParametreIfNotExists(parametreMetierRepository,
            "AUTORISER_GARANTIE_MATERIELLE_OBLIGATOIRE", "Garantie matérielle obligatoire",
            TypeParametre.TEXTE, CategorieParametre.GENERAL,
            null, null, "false", null,
            "Indique si une garantie matérielle est obligatoire sur le dossier crédit", "false");

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

        createParametreIfNotExists(parametreMetierRepository,
            "PRIX_CARNET", "Prix carnet",
            TypeParametre.DECIMAL, CategorieParametre.FRAIS,
            BigDecimal.valueOf(1000), null, null, "FC",
            "Prix unitaire d'un carnet vendu sur le terrain", "1000");

        createParametreIfNotExists(parametreMetierRepository,
            "FRAIS_CARNET_EPARGNE", "Frais carnet épargne",
            TypeParametre.DECIMAL, CategorieParametre.FRAIS,
            BigDecimal.valueOf(1000), null, null, "FC",
            "Frais obligatoire perçu à l'adhésion pour le carnet d'épargne", "1000");

        createParametreIfNotExists(parametreMetierRepository,
            "COUT_ACHAT_CARNET", "Coût d'achat carnet",
            TypeParametre.DECIMAL, CategorieParametre.FRAIS,
            BigDecimal.valueOf(300), null, null, "FC",
            "Coût unitaire d'achat d'un carnet d'épargne", "300");

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
     * Correction sécurisée : si des sites avec agence_id invalide existent (héritage d'une
     * ancienne version), crée une agence de bootstrap minimale et corrige les lignes.
     * Sur une base vide : aucune agence n'est créée, aucune correction n'est appliquée.
     */
    private void fixCorruptedSitesIfNeeded(AgenceRepository agenceRepository, JdbcTemplate jdbcTemplate) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM site WHERE agence_id IS NULL OR agence_id = 0",
                    Integer.class);
            if (count == null || count == 0) {
                return; // Base vide ou propre — rien à faire
            }
            log.warn("ATTENTION: {} site(s) avec agence_id invalide détectés. Correction automatique...", count);
            Agence bootstrap = agenceRepository.findByCodeAgence("AGENCE-BOOTSTRAP")
                    .orElseGet(() -> agenceRepository.save(Agence.builder()
                            .codeAgence("AGENCE-BOOTSTRAP")
                            .nomAgence("Agence Bootstrap (à reconfigurer via l'interface)")
                            .adresse("À configurer")
                            .ville("Kinshasa")
                            .actif(true)
                            .build()));
            int fixed = jdbcTemplate.update(
                    "UPDATE site SET agence_id = ? WHERE agence_id IS NULL OR agence_id = 0",
                    bootstrap.getId());
            log.warn("{} site(s) corrigés → agence '{}'. RECONFIGURER cette agence via l'interface.",
                    fixed, bootstrap.getNomAgence());
        } catch (Exception e) {
            // Table 'site' absente (toute première init) → normal, rien à corriger
            log.debug("Table 'site' non encore disponible : pas de correction nécessaire");
        }
    }
}

