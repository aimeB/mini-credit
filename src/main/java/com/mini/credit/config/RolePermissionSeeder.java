package com.mini.credit.config;

import com.mini.credit.entity.referentiel.Permission;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.RolePermission;
import com.mini.credit.enums.security.PermissionCode;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.repository.referentiel.PermissionRepository;
import com.mini.credit.repository.referentiel.RolePermissionRepository;
import com.mini.credit.repository.referentiel.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.*;

/**
 * Seeder pour initialiser le système RBAC professionnel.
 *
 * Crée :
 * - Tous les rôles (ADMIN, CHEF_BUREAU, RESPONSABLE[alias], AGENT_BUREAU,
 *   AGENT_TERRAIN, CONTROLEUR, CAISSIER, COO, RCI, GERANT_GENERAL, MEMBER)
 * - Toutes les permissions (38 permissions granulaires)
 * - Le mapping rôle-permissions selon la matrice NIST + OWASP
 *
 * Étape 5 : Seeder pour initialiser le RBAC
 */
@Configuration
@Slf4j
@Profile("!test")
public class RolePermissionSeeder {

    @Bean
    public CommandLineRunner seedRolesAndPermissions(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository
    ) {
        return args -> {
            log.info("========== INITIALISATION DU SYSTÈME RBAC ==========");

            // Étape 1 : Créer tous les rôles
            log.info("1. Création des rôles...");
            Map<RoleCode, Role> roles = createRoles(roleRepository);

            // Étape 2 : Créer toutes les permissions
            log.info("2. Création des permissions...");
            Map<PermissionCode, Permission> permissions = createPermissions(permissionRepository);

            // Étape 3 : Mapper les permissions aux rôles
            log.info("3. Mapping des permissions aux rôles...");
            assignPermissionsToRoles(roles, permissions, rolePermissionRepository);

            log.info("========== INITIALISATION RBAC TERMINÉE ==========");
        };
    }

    /**
     * Crée tous les rôles du système
     */
    private Map<RoleCode, Role> createRoles(RoleRepository roleRepository) {
        Map<RoleCode, Role> roles = new HashMap<>();

        Role[] roleArray = {
            Role.builder()
                    .code(RoleCode.ADMIN)
                    .libelle("Administrateur système")
                    .description("Super administrateur avec accès complet à toutes les fonctionnalités")
                    .isActive(true)
                    .build(),
            Role.builder()
                    .code(RoleCode.CHEF_BUREAU)
                    .libelle("Chef de Bureau")
                    .description("Supervision, validation des crédits, rapports")
                    .isActive(true)
                    .build(),
                Role.builder()
                    .code(RoleCode.RESPONSABLE)
                    .libelle("Alias legacy Chef de Bureau")
                    .description("Alias technique temporaire pour compatibilité migration vers CHEF_BUREAU")
                    .isActive(true)
                    .build(),
            Role.builder()
                    .code(RoleCode.AGENT_BUREAU)
                    .libelle("Agent de bureau")
                    .description("Saisie membres, demandes crédit, suivi administratif (équivalent métier: Gestionnaire 3N)")
                    .isActive(true)
                    .build(),
            Role.builder()
                    .code(RoleCode.AGENT_TERRAIN)
                    .libelle("Agent terrain")
                    .description("Prospection, suivi membres, saisie initiale")
                    .isActive(true)
                    .build(),
            Role.builder()
                    .code(RoleCode.CAISSIER)
                    .libelle("Caissier")
                    .description("Gestion caisse, versements, retraits, remboursements")
                    .isActive(true)
                    .build(),
            Role.builder()
                    .code(RoleCode.CONTROLEUR)
                    .libelle("Contrôleur")
                    .description("Validation recettes, retraits, crédits, réconciliation caisse")
                    .isActive(true)
                    .build(),
                Role.builder()
                    .code(RoleCode.COO)
                    .libelle("COO")
                    .description("Supervision transverse des opérations et du pilotage métier")
                    .isActive(true)
                    .build(),
                Role.builder()
                    .code(RoleCode.RCI)
                    .libelle("Responsable du Contrôle Interne")
                    .description("Audit, enquête, contrôle interne des opérations")
                    .isActive(true)
                    .build(),
                Role.builder()
                    .code(RoleCode.GERANT_GENERAL)
                    .libelle("Gérant Général")
                    .description("Gouvernance globale et supervision de haut niveau")
                    .isActive(true)
                    .build(),
            Role.builder()
                    .code(RoleCode.MEMBER)
                    .libelle("Membre client")
                    .description("Accès à son profil, ses crédits et comptes épargne")
                    .isActive(true)
                    .build()
        };

        for (Role role : roleArray) {
            if (roleRepository.findByCode(role.getCode()).isEmpty()) {
                Role saved = roleRepository.save(role);
                roles.put(role.getCode(), saved);
                log.info("  ✓ Rôle créé : {}", role.getCode());
            } else {
                Role existing = roleRepository.findByCode(role.getCode()).get();
                roles.put(role.getCode(), existing);
                log.info("  ✓ Rôle existant : {}", role.getCode());
            }
        }

        return roles;
    }

    /**
     * Crée toutes les permissions du système
     */
    private Map<PermissionCode, Permission> createPermissions(PermissionRepository permissionRepository) {
        Map<PermissionCode, Permission> permissions = new HashMap<>();

        Permission[] permissionArray = {
            // ============ Utilisateurs ============
            Permission.builder().code(PermissionCode.USER_READ).description("Voir les utilisateurs").isActive(true).build(),
            Permission.builder().code(PermissionCode.USER_CREATE).description("Créer un utilisateur").isActive(true).build(),
            Permission.builder().code(PermissionCode.USER_UPDATE).description("Modifier un utilisateur").isActive(true).build(),
            Permission.builder().code(PermissionCode.USER_ASSIGN_ROLE).description("Assigner des rôles").isActive(true).build(),
            Permission.builder().code(PermissionCode.USER_LOCK).description("Bloquer un utilisateur").isActive(true).build(),
            Permission.builder().code(PermissionCode.USER_UNLOCK).description("Débloquer un utilisateur").isActive(true).build(),
            Permission.builder().code(PermissionCode.USER_PASSWORD_RESET).description("Réinitialiser le mot de passe d'un utilisateur").isActive(true).build(),
            Permission.builder().code(PermissionCode.USER_PASSWORD_CHANGE).description("Changer son propre mot de passe").isActive(true).build(),

            // ============ Membres ============
            Permission.builder().code(PermissionCode.MEMBRE_READ).description("Voir les membres").isActive(true).build(),
            Permission.builder().code(PermissionCode.MEMBRE_CREATE).description("Créer un membre").isActive(true).build(),
            Permission.builder().code(PermissionCode.MEMBRE_UPDATE).description("Modifier un membre").isActive(true).build(),
            Permission.builder().code(PermissionCode.MEMBRE_CLOSE).description("Clôturer un membre").isActive(true).build(),
            Permission.builder().code(PermissionCode.MEMBRE_READ_SELF).description("Voir son propre profil").isActive(true).build(),
            Permission.builder().code(PermissionCode.MEMBRE_UPDATE_SELF).description("Modifier son profil").isActive(true).build(),

            // ============ Épargne ============
            Permission.builder().code(PermissionCode.EPARGNE_COMPTE_CREATE).description("Créer un compte épargne").isActive(true).build(),
            Permission.builder().code(PermissionCode.EPARGNE_COMPTE_READ).description("Voir les comptes épargne").isActive(true).build(),
            Permission.builder().code(PermissionCode.EPARGNE_OPERATION_CREATE).description("Créer une opération épargne").isActive(true).build(),
            Permission.builder().code(PermissionCode.EPARGNE_OPERATION_READ).description("Voir les opérations épargne").isActive(true).build(),

            // ============ Caisse (séparation critique) ============
            Permission.builder().code(PermissionCode.CAISSE_CREATE).description("Créer une caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.CAISSE_READ).description("Voir les caisses").isActive(true).build(),
            Permission.builder().code(PermissionCode.SESSION_CAISSE_OPEN).description("Ouvrir une session caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.SESSION_CAISSE_OPEN_OVERRIDE).description("Forcer une ouverture de session caisse avec solde manuel").isActive(true).build(),
            Permission.builder().code(PermissionCode.SESSION_CAISSE_PRE_CLOSE).description("Pré-clôturer une session caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.SESSION_CAISSE_CLOSE).description("Clôturer une session caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.SESSION_CAISSE_CONTROL_VALIDATE).description("Valider le contrôle d'une session caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.SESSION_CAISSE_FINAL_CLOSE).description("Clôturer définitivement une session caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.SESSION_CAISSE_ANOMALIE_READ).description("Consulter les anomalies de session caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.SESSION_CAISSE_ANOMALIE_REQUEST).description("Demander une annulation de session caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.SESSION_CAISSE_ANOMALIE_VALIDATE).description("Valider une annulation de session caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.SESSION_CAISSE_ADMIN_CANCEL).description("Annuler administrativement une session caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.SESSION_CAISSE_REOPEN_CONTROLLED).description("Réouvrir de manière contrôlée une session pré-clôturée").isActive(true).build(),
            Permission.builder().code(PermissionCode.OPERATION_CAISSE_CREATE).description("Enregistrer une opération caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.OPERATION_CAISSE_READ).description("Voir les opérations caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.JOURNAL_CAISSE_READ).description("Consulter le journal de caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.DEPENSE_CAISSE_CREATE).description("Créer une dépense caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.DEPENSE_CAISSE_SUBMIT).description("Soumettre une dépense caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.DEPENSE_CAISSE_VALIDATE).description("Valider une dépense caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.DEPENSE_CAISSE_REJECT).description("Rejeter une dépense caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.DEPENSE_CAISSE_PAY).description("Payer une dépense caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.DEPENSE_CAISSE_READ).description("Voir les dépenses caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.DEPENSE_CAISSE_CANCEL).description("Annuler une dépense caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.DASHBOARD_CAISSE_READ).description("Voir le dashboard caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.RAPPORT_CAISSE_READ).description("Consulter les rapports caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.RAPPORT_CAISSE_EXPORT).description("Exporter les rapports caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.RAPPORT_CAISSE_AUDIT_READ).description("Consulter les éléments d'audit des rapports caisse").isActive(true).build(),

            // ============ Crédits (séparation critique) ============
            Permission.builder().code(PermissionCode.DEMANDE_CREDIT_CREATE).description("Créer une demande crédit").isActive(true).build(),
            Permission.builder().code(PermissionCode.DEMANDE_CREDIT_READ).description("Voir une demande crédit").isActive(true).build(),
            Permission.builder().code(PermissionCode.ANALYSE_RISQUE_CREATE).description("Créer une analyse de risque").isActive(true).build(),
            Permission.builder().code(PermissionCode.ANALYSE_RISQUE_READ).description("Voir une analyse de risque").isActive(true).build(),
            Permission.builder().code(PermissionCode.CREDIT_APPROVE).description("Approuver un crédit (DÉCISION)").isActive(true).build(),
            Permission.builder().code(PermissionCode.CREDIT_DISBURSE).description("Décaisser un crédit (EXÉCUTION)").isActive(true).build(),
            Permission.builder().code(PermissionCode.CREDIT_READ).description("Voir un crédit").isActive(true).build(),
            Permission.builder().code(PermissionCode.GARANTIE_CONTROL).description("Contrôler les garanties crédit").isActive(true).build(),
            Permission.builder().code(PermissionCode.GARANTIE_READ).description("Lire la garantie crédit").isActive(true).build(),
            Permission.builder().code(PermissionCode.GARANTIE_VALIDATE).description("Valider la garantie crédit").isActive(true).build(),
            Permission.builder().code(PermissionCode.GARANTIE_BLOQUER_EPARGNE).description("Bloquer l'épargne de garantie").isActive(true).build(),
            Permission.builder().code(PermissionCode.GARANTIE_MATERIELLE_CREATE).description("Créer une garantie matérielle").isActive(true).build(),
            Permission.builder().code(PermissionCode.REMBOURSEMENT_CREATE).description("Enregistrer un remboursement").isActive(true).build(),
            Permission.builder().code(PermissionCode.REMBOURSEMENT_READ).description("Voir les remboursements").isActive(true).build(),

            // ============ Dashboard et audit ============
            Permission.builder().code(PermissionCode.DASHBOARD_GLOBAL_READ).description("Voir le dashboard global").isActive(true).build(),
            Permission.builder().code(PermissionCode.DASHBOARD_CONTROLE_INTERNE_READ).description("Voir le dashboard controle interne").isActive(true).build(),
            Permission.builder().code(PermissionCode.AUDIT_READ).description("Consulter l'audit").isActive(true).build(),
            Permission.builder().code(PermissionCode.AUDIT_LOG_READ).description("Consulter les logs d'audit").isActive(true).build(),
            Permission.builder().code(PermissionCode.AUDIT_LOG_EXPORT).description("Exporter les logs d'audit").isActive(true).build(),
            Permission.builder().code(PermissionCode.AUDIT_SECURITY_READ).description("Consulter les événements de sécurité").isActive(true).build(),

            // ============ Permissions CONTROLEUR ============
            Permission.builder().code(PermissionCode.CONTROLEUR_EPARGNE_READ).description("Voir les comptes épargne (CONTROLEUR)").isActive(true).build(),
            Permission.builder().code(PermissionCode.CONTROLEUR_EPARGNE_OPERATION_READ).description("Voir les opérations épargne (CONTROLEUR)").isActive(true).build(),
            Permission.builder().code(PermissionCode.CONTROLEUR_CREDIT_READ).description("Voir les crédits (CONTROLEUR)").isActive(true).build(),
            Permission.builder().code(PermissionCode.CONTROLEUR_DEMANDE_CREDIT_READ).description("Voir les demandes crédit (CONTROLEUR)").isActive(true).build(),
            Permission.builder().code(PermissionCode.CONTROLEUR_DEMANDE_CREDIT_VALIDATE).description("Valider/modifier analyse risque demandes crédit").isActive(true).build(),
            Permission.builder().code(PermissionCode.CONTROLEUR_SESSION_CAISSE_READ).description("Voir les SessionCaisse (CONTROLEUR)").isActive(true).build(),
            Permission.builder().code(PermissionCode.CONTROLEUR_SESSION_CAISSE_VALIDATE).description("Valider clôture SessionCaisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.CONTROLEUR_ECART_READ).description("Voir les écarts caisse").isActive(true).build(),
            Permission.builder().code(PermissionCode.CONTROLEUR_ECART_VALIDATE).description("Valider/modifier écarts investigation").isActive(true).build(),
            Permission.builder().code(PermissionCode.CONTROLEUR_RECETTES_VALIDATE).description("Valider recettes journalières (réconciliation)").isActive(true).build(),
            Permission.builder().code(PermissionCode.CONTROLEUR_RETRAITS_VALIDATE).description("Valider retraits épargne (vérif solde)").isActive(true).build(),
            Permission.builder().code(PermissionCode.CONTROLEUR_CREDITS_VALIDATE).description("Valider crédits (garanties, frais, décaissement)").isActive(true).build(),
            Permission.builder().code(PermissionCode.CONTROLEUR_AUDIT_READ).description("Consulter audit logs antenne").isActive(true).build()
        };

        for (Permission perm : permissionArray) {
            if (permissionRepository.findByCode(perm.getCode()).isEmpty()) {
                Permission saved = permissionRepository.save(perm);
                permissions.put(perm.getCode(), saved);
                log.info("  ✓ Permission créée : {}", perm.getCode());
            } else {
                Permission existing = permissionRepository.findByCode(perm.getCode()).get();
                permissions.put(perm.getCode(), existing);
                log.info("  ✓ Permission existante : {}", perm.getCode());
            }
        }

        return permissions;
    }

    /**
     * Assigne les permissions aux rôles selon la matrice RBAC
     */
    private void assignPermissionsToRoles(
            Map<RoleCode, Role> roles,
            Map<PermissionCode, Permission> permissions,
            RolePermissionRepository rolePermissionRepository
    ) {
        // Matrice rôle-permissions
        Map<RoleCode, Set<PermissionCode>> matrix = buildPermissionMatrix();

        for (Map.Entry<RoleCode, Set<PermissionCode>> entry : matrix.entrySet()) {
            Role role = roles.get(entry.getKey());
            Set<PermissionCode> permCodes = entry.getValue();

            log.info("  Assignation permissions au rôle : {}", entry.getKey());

            for (PermissionCode permCode : permCodes) {
                Permission perm = permissions.get(permCode);

                Optional<RolePermission> existing = rolePermissionRepository
                        .findByRoleIdAndPermissionId(role.getId(), perm.getId());

                if (existing.isEmpty()) {
                    RolePermission rp = RolePermission.builder()
                            .role(role)
                            .permission(perm)
                            .build();
                    rolePermissionRepository.save(rp);
                    log.debug("    → {} assignée", permCode);
                }
            }
        }
    }

    /**
     * Construit la matrice des permissions par rôle (source unique de vérité)
     */
    private Map<RoleCode, Set<PermissionCode>> buildPermissionMatrix() {
        Map<RoleCode, Set<PermissionCode>> matrix = new HashMap<>();

        // ADMIN : toutes les permissions
        matrix.put(RoleCode.ADMIN, Set.of(
                // Utilisateurs
                PermissionCode.USER_READ, PermissionCode.USER_CREATE, PermissionCode.USER_UPDATE,
                PermissionCode.USER_ASSIGN_ROLE, PermissionCode.USER_LOCK, PermissionCode.USER_UNLOCK,
                PermissionCode.USER_PASSWORD_RESET, PermissionCode.USER_PASSWORD_CHANGE,
                // Membres
                PermissionCode.MEMBRE_READ, PermissionCode.MEMBRE_CREATE, PermissionCode.MEMBRE_UPDATE, PermissionCode.MEMBRE_CLOSE,
                // Épargne
                PermissionCode.EPARGNE_COMPTE_CREATE, PermissionCode.EPARGNE_COMPTE_READ,
                PermissionCode.EPARGNE_OPERATION_CREATE, PermissionCode.EPARGNE_OPERATION_READ,
                // Caisse
                PermissionCode.CAISSE_CREATE, PermissionCode.CAISSE_READ,
                PermissionCode.SESSION_CAISSE_OPEN, PermissionCode.SESSION_CAISSE_OPEN_OVERRIDE, PermissionCode.SESSION_CAISSE_PRE_CLOSE,
                PermissionCode.SESSION_CAISSE_CLOSE, PermissionCode.SESSION_CAISSE_FINAL_CLOSE,
                PermissionCode.SESSION_CAISSE_CONTROL_VALIDATE,
                PermissionCode.SESSION_CAISSE_ANOMALIE_READ, PermissionCode.SESSION_CAISSE_ANOMALIE_REQUEST,
                PermissionCode.SESSION_CAISSE_ANOMALIE_VALIDATE, PermissionCode.SESSION_CAISSE_ADMIN_CANCEL,
                PermissionCode.SESSION_CAISSE_REOPEN_CONTROLLED,
                PermissionCode.OPERATION_CAISSE_CREATE, PermissionCode.OPERATION_CAISSE_READ,
                PermissionCode.JOURNAL_CAISSE_READ,
                PermissionCode.DEPENSE_CAISSE_CREATE, PermissionCode.DEPENSE_CAISSE_SUBMIT,
                PermissionCode.DEPENSE_CAISSE_VALIDATE, PermissionCode.DEPENSE_CAISSE_REJECT,
                PermissionCode.DEPENSE_CAISSE_PAY, PermissionCode.DEPENSE_CAISSE_READ,
                PermissionCode.DEPENSE_CAISSE_CANCEL,
                PermissionCode.DASHBOARD_CAISSE_READ,
                PermissionCode.RAPPORT_CAISSE_READ,
                PermissionCode.RAPPORT_CAISSE_EXPORT,
                PermissionCode.RAPPORT_CAISSE_AUDIT_READ,
                // Crédit
                PermissionCode.DEMANDE_CREDIT_CREATE, PermissionCode.DEMANDE_CREDIT_READ,
                PermissionCode.ANALYSE_RISQUE_CREATE, PermissionCode.ANALYSE_RISQUE_READ,
                PermissionCode.CREDIT_APPROVE, PermissionCode.CREDIT_DISBURSE,
                PermissionCode.GARANTIE_CONTROL, PermissionCode.GARANTIE_READ,
                PermissionCode.GARANTIE_VALIDATE, PermissionCode.GARANTIE_BLOQUER_EPARGNE,
                PermissionCode.GARANTIE_MATERIELLE_CREATE,
                PermissionCode.CREDIT_READ, PermissionCode.REMBOURSEMENT_CREATE, PermissionCode.REMBOURSEMENT_READ,
                // Dashboard
                PermissionCode.DASHBOARD_GLOBAL_READ, PermissionCode.AUDIT_READ,
                PermissionCode.DASHBOARD_CONTROLE_INTERNE_READ,
                PermissionCode.AUDIT_LOG_READ, PermissionCode.AUDIT_LOG_EXPORT, PermissionCode.AUDIT_SECURITY_READ,
                // Contrôles opérationnels fins
                PermissionCode.CONTROLEUR_RECETTES_VALIDATE,
                PermissionCode.CONTROLEUR_RETRAITS_VALIDATE,
                PermissionCode.CONTROLEUR_CREDITS_VALIDATE,
                PermissionCode.CONTROLEUR_AUDIT_READ
        ));

        // RESPONSABLE : supervision et validation
        matrix.put(RoleCode.RESPONSABLE, Set.of(
                // Membres
                PermissionCode.MEMBRE_READ, PermissionCode.MEMBRE_CREATE, PermissionCode.MEMBRE_UPDATE, PermissionCode.MEMBRE_CLOSE,
                // Épargne
                PermissionCode.EPARGNE_COMPTE_CREATE, PermissionCode.EPARGNE_COMPTE_READ,
                PermissionCode.EPARGNE_OPERATION_CREATE, PermissionCode.EPARGNE_OPERATION_READ,
                // Caisse (supervision)
                PermissionCode.SESSION_CAISSE_CONTROL_VALIDATE,
                PermissionCode.SESSION_CAISSE_FINAL_CLOSE,
                PermissionCode.SESSION_CAISSE_ANOMALIE_READ,
                PermissionCode.SESSION_CAISSE_ANOMALIE_VALIDATE,
                PermissionCode.SESSION_CAISSE_ADMIN_CANCEL,
                PermissionCode.SESSION_CAISSE_REOPEN_CONTROLLED,
                PermissionCode.CAISSE_READ, PermissionCode.OPERATION_CAISSE_READ, PermissionCode.JOURNAL_CAISSE_READ, PermissionCode.DEPENSE_CAISSE_READ, PermissionCode.DEPENSE_CAISSE_VALIDATE, PermissionCode.DASHBOARD_CAISSE_READ,
                PermissionCode.RAPPORT_CAISSE_READ, PermissionCode.RAPPORT_CAISSE_EXPORT, PermissionCode.RAPPORT_CAISSE_AUDIT_READ,
                // Crédit
                PermissionCode.DEMANDE_CREDIT_CREATE, PermissionCode.DEMANDE_CREDIT_READ,
                PermissionCode.ANALYSE_RISQUE_CREATE, PermissionCode.ANALYSE_RISQUE_READ,
                PermissionCode.CREDIT_APPROVE, PermissionCode.CREDIT_READ,
                PermissionCode.GARANTIE_READ, PermissionCode.GARANTIE_VALIDATE,
                PermissionCode.REMBOURSEMENT_CREATE, PermissionCode.REMBOURSEMENT_READ,
                // Dashboard
                PermissionCode.DASHBOARD_GLOBAL_READ, PermissionCode.AUDIT_READ,
                PermissionCode.DASHBOARD_CONTROLE_INTERNE_READ,
                PermissionCode.AUDIT_LOG_READ,
                PermissionCode.USER_PASSWORD_RESET, PermissionCode.USER_PASSWORD_CHANGE
        ));

            // CHEF_BUREAU : rôle officiel cible (mêmes permissions que RESPONSABLE pendant migration)
            matrix.put(RoleCode.CHEF_BUREAU, Set.of(
                // Membres
                PermissionCode.MEMBRE_READ, PermissionCode.MEMBRE_CREATE, PermissionCode.MEMBRE_UPDATE, PermissionCode.MEMBRE_CLOSE,
                // Épargne
                PermissionCode.EPARGNE_COMPTE_CREATE, PermissionCode.EPARGNE_COMPTE_READ,
                PermissionCode.EPARGNE_OPERATION_CREATE, PermissionCode.EPARGNE_OPERATION_READ,
                // Caisse (supervision + suppléance caissier)
                PermissionCode.CAISSE_CREATE, PermissionCode.CAISSE_READ,
                PermissionCode.SESSION_CAISSE_CONTROL_VALIDATE, PermissionCode.SESSION_CAISSE_FINAL_CLOSE,
                PermissionCode.SESSION_CAISSE_OPEN, PermissionCode.SESSION_CAISSE_PRE_CLOSE, PermissionCode.SESSION_CAISSE_CLOSE,
                PermissionCode.SESSION_CAISSE_ANOMALIE_READ,
                PermissionCode.SESSION_CAISSE_ANOMALIE_VALIDATE,
                PermissionCode.SESSION_CAISSE_ADMIN_CANCEL,
                PermissionCode.SESSION_CAISSE_REOPEN_CONTROLLED,
                PermissionCode.OPERATION_CAISSE_READ, PermissionCode.JOURNAL_CAISSE_READ, PermissionCode.DEPENSE_CAISSE_READ, PermissionCode.DEPENSE_CAISSE_VALIDATE, PermissionCode.DASHBOARD_CAISSE_READ,
                PermissionCode.RAPPORT_CAISSE_READ, PermissionCode.RAPPORT_CAISSE_EXPORT, PermissionCode.RAPPORT_CAISSE_AUDIT_READ,
                // Crédit
                PermissionCode.DEMANDE_CREDIT_CREATE, PermissionCode.DEMANDE_CREDIT_READ,
                PermissionCode.ANALYSE_RISQUE_CREATE, PermissionCode.ANALYSE_RISQUE_READ,
                PermissionCode.CREDIT_APPROVE, PermissionCode.CREDIT_READ,
                PermissionCode.GARANTIE_READ,
                PermissionCode.REMBOURSEMENT_CREATE, PermissionCode.REMBOURSEMENT_READ,
                // Dashboard
                PermissionCode.DASHBOARD_GLOBAL_READ, PermissionCode.AUDIT_READ,
                PermissionCode.DASHBOARD_CONTROLE_INTERNE_READ,
                PermissionCode.AUDIT_LOG_READ,
                PermissionCode.USER_PASSWORD_RESET, PermissionCode.USER_PASSWORD_CHANGE
            ));

        // AGENT_BUREAU : saisie et traitement
        matrix.put(RoleCode.AGENT_BUREAU, Set.of(
                // Membres
                PermissionCode.MEMBRE_READ, PermissionCode.MEMBRE_CREATE, PermissionCode.MEMBRE_UPDATE, PermissionCode.MEMBRE_CLOSE,
                // Épargne
                PermissionCode.EPARGNE_COMPTE_CREATE, PermissionCode.EPARGNE_COMPTE_READ,
                PermissionCode.EPARGNE_OPERATION_CREATE, PermissionCode.EPARGNE_OPERATION_READ,
                // Crédit
                PermissionCode.DEMANDE_CREDIT_CREATE, PermissionCode.DEMANDE_CREDIT_READ,
                PermissionCode.ANALYSE_RISQUE_CREATE, PermissionCode.ANALYSE_RISQUE_READ,
                PermissionCode.CREDIT_READ,
                PermissionCode.GARANTIE_READ,
                PermissionCode.REMBOURSEMENT_CREATE, PermissionCode.REMBOURSEMENT_READ,
                // Dashboard
                PermissionCode.DASHBOARD_GLOBAL_READ,
                PermissionCode.USER_PASSWORD_CHANGE
        ));

        // AGENT_TERRAIN : prospection et suivi
        matrix.put(RoleCode.AGENT_TERRAIN, Set.of(
                // Membres
                PermissionCode.MEMBRE_READ, PermissionCode.MEMBRE_CREATE, PermissionCode.MEMBRE_UPDATE,
                // Épargne
                PermissionCode.EPARGNE_COMPTE_READ,
                // Crédit
                PermissionCode.DEMANDE_CREDIT_CREATE, PermissionCode.DEMANDE_CREDIT_READ, PermissionCode.CREDIT_READ,
                // Dashboard (partiel)
                PermissionCode.DASHBOARD_GLOBAL_READ,
                PermissionCode.USER_PASSWORD_CHANGE
        ));

        // CAISSIER : opérations trésorerie uniquement
        matrix.put(RoleCode.CAISSIER, Set.of(
                // Caisse (opérations)
                PermissionCode.CAISSE_CREATE, PermissionCode.CAISSE_READ,
            PermissionCode.SESSION_CAISSE_OPEN, PermissionCode.SESSION_CAISSE_PRE_CLOSE, PermissionCode.SESSION_CAISSE_CLOSE,
                PermissionCode.SESSION_CAISSE_ANOMALIE_READ, PermissionCode.SESSION_CAISSE_ANOMALIE_REQUEST,
                PermissionCode.OPERATION_CAISSE_CREATE, PermissionCode.OPERATION_CAISSE_READ, PermissionCode.JOURNAL_CAISSE_READ,
            PermissionCode.DEPENSE_CAISSE_CREATE, PermissionCode.DEPENSE_CAISSE_SUBMIT,
            PermissionCode.DEPENSE_CAISSE_READ, PermissionCode.DEPENSE_CAISSE_PAY,
                PermissionCode.DASHBOARD_CAISSE_READ,
                PermissionCode.RAPPORT_CAISSE_READ, PermissionCode.RAPPORT_CAISSE_EXPORT,
                // Crédit (décaissement et remboursement)
                PermissionCode.CREDIT_DISBURSE, PermissionCode.CREDIT_READ,
                PermissionCode.REMBOURSEMENT_CREATE, PermissionCode.REMBOURSEMENT_READ,
                PermissionCode.USER_PASSWORD_CHANGE
        ));

        // CONTROLEUR : validation et réconciliation
        matrix.put(RoleCode.CONTROLEUR, Set.of(
            // Caisse (lecture et workflow final)
            PermissionCode.CAISSE_READ,
            PermissionCode.OPERATION_CAISSE_READ,
            PermissionCode.JOURNAL_CAISSE_READ,
            PermissionCode.SESSION_CAISSE_FINAL_CLOSE,
                PermissionCode.RAPPORT_CAISSE_READ, PermissionCode.RAPPORT_CAISSE_EXPORT,
                // Épargne (lecture et validation)
                PermissionCode.CONTROLEUR_EPARGNE_READ,
                PermissionCode.CONTROLEUR_EPARGNE_OPERATION_READ,
                // Crédit (lecture et validation)
                PermissionCode.CONTROLEUR_CREDIT_READ,
                PermissionCode.CONTROLEUR_DEMANDE_CREDIT_READ,
                PermissionCode.CONTROLEUR_DEMANDE_CREDIT_VALIDATE,
                // Caisse (lecture et validation)
                PermissionCode.CONTROLEUR_SESSION_CAISSE_VALIDATE,
                PermissionCode.SESSION_CAISSE_CONTROL_VALIDATE,
                PermissionCode.SESSION_CAISSE_ANOMALIE_READ,
                PermissionCode.DEPENSE_CAISSE_READ, PermissionCode.DEPENSE_CAISSE_VALIDATE,
                PermissionCode.DEPENSE_CAISSE_REJECT,
                PermissionCode.CONTROLEUR_ECART_READ,
                PermissionCode.CONTROLEUR_ECART_VALIDATE,
                // Recettes (validation)
                PermissionCode.CONTROLEUR_RECETTES_VALIDATE,
                // Retraits (validation)
                PermissionCode.CONTROLEUR_RETRAITS_VALIDATE,
                // Crédits (validation)
                PermissionCode.CONTROLEUR_CREDITS_VALIDATE,
                PermissionCode.GARANTIE_CONTROL,
                // Audit (consultation)
                PermissionCode.CONTROLEUR_AUDIT_READ,
                PermissionCode.AUDIT_LOG_READ,
                PermissionCode.RAPPORT_CAISSE_AUDIT_READ,
                // Dashboard
                PermissionCode.DASHBOARD_GLOBAL_READ,
                PermissionCode.DASHBOARD_CONTROLE_INTERNE_READ,
                PermissionCode.USER_PASSWORD_CHANGE
        ));

            // RCI : lecture audit et supervision contrôle interne
            matrix.put(RoleCode.RCI, Set.of(
                PermissionCode.CAISSE_READ,
                PermissionCode.OPERATION_CAISSE_READ,
                PermissionCode.JOURNAL_CAISSE_READ,
                PermissionCode.SESSION_CAISSE_ANOMALIE_READ,
                PermissionCode.DEPENSE_CAISSE_READ,
                PermissionCode.RAPPORT_CAISSE_READ,
                PermissionCode.RAPPORT_CAISSE_EXPORT,
                PermissionCode.RAPPORT_CAISSE_AUDIT_READ,
                PermissionCode.AUDIT_READ,
                PermissionCode.AUDIT_LOG_READ,
                PermissionCode.AUDIT_LOG_EXPORT,
                PermissionCode.AUDIT_SECURITY_READ,
                PermissionCode.DASHBOARD_GLOBAL_READ,
                PermissionCode.DASHBOARD_CONTROLE_INTERNE_READ,
                PermissionCode.USER_PASSWORD_CHANGE
            ));

        // MEMBER : self-service uniquement
        matrix.put(RoleCode.MEMBER, Set.of(
                // Membres
                PermissionCode.MEMBRE_READ_SELF, PermissionCode.MEMBRE_UPDATE_SELF,
                // Crédit
                PermissionCode.DEMANDE_CREDIT_CREATE, PermissionCode.DEMANDE_CREDIT_READ, PermissionCode.CREDIT_READ,
                // Épargne
                PermissionCode.EPARGNE_COMPTE_READ,
                PermissionCode.USER_PASSWORD_CHANGE
        ));

        return matrix;
    }
}
