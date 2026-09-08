package com.mini.credit.service.impl;

import com.mini.credit.dto.utilisateur.UtilisateurDTO;
import com.mini.credit.dto.utilisateur.CreateUtilisateurRequest;
import com.mini.credit.dto.utilisateur.UpdateUtilisateurRequest;
import com.mini.credit.dto.utilisateur.ChangePasswordRequest;
import com.mini.credit.dto.utilisateur.AdminPasswordResetRequest;
import com.mini.credit.dto.utilisateur.ResetPasswordResponse;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.enums.PosteEmploye;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.EmployeRepository;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.referentiel.RoleRepository;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.service.UtilisateurService;
import com.mini.credit.service.security.SecurityUtils;
import com.mini.credit.util.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final EmployeRepository employeRepository;
    private final AgentTerrainRepository agentTerrainRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    /**
     * R\u00f4les op\u00e9rationnels : employeId obligatoire lors de la cr\u00e9ation.
     * Exceptions : ADMIN et MEMBER peuvent \u00eatre cr\u00e9\u00e9s sans fiche employ\u00e9.
     */
    private static final Set<RoleCode> OPERATIONAL_ROLES = Set.of(
            RoleCode.AGENT_TERRAIN,
            RoleCode.GESTIONNAIRE,
            RoleCode.CONTROLEUR,
            RoleCode.CAISSIER,
            RoleCode.CHEF_BUREAU
    );

    /**
     * Mapping rôle applicatif → fonction employé obligatoire.
     * Validation BLOQUANTE à la création d'un utilisateur opérationnel.
     * Non appliqué à ADMIN ni MEMBER.
     */
    private static final Map<RoleCode, PosteEmploye> ROLE_FONCTION_ATTENDUE;
    static {
        ROLE_FONCTION_ATTENDUE = new EnumMap<>(RoleCode.class);
        ROLE_FONCTION_ATTENDUE.put(RoleCode.AGENT_TERRAIN, PosteEmploye.AGENT_TERRAIN);
        ROLE_FONCTION_ATTENDUE.put(RoleCode.GESTIONNAIRE,  PosteEmploye.GESTIONNAIRE);
        ROLE_FONCTION_ATTENDUE.put(RoleCode.CONTROLEUR,    PosteEmploye.CONTROLEUR);
        ROLE_FONCTION_ATTENDUE.put(RoleCode.CAISSIER,      PosteEmploye.CAISSIER);
        ROLE_FONCTION_ATTENDUE.put(RoleCode.CHEF_BUREAU,   PosteEmploye.CHEF_BUREAU);
        ROLE_FONCTION_ATTENDUE.put(RoleCode.RCI,           PosteEmploye.RCI);
        ROLE_FONCTION_ATTENDUE.put(RoleCode.COO,           PosteEmploye.COO);
        ROLE_FONCTION_ATTENDUE.put(RoleCode.GERANT_GENERAL,PosteEmploye.GERANT_GENERAL);
    }

    private static final Set<RoleCode> PROTECTED_RESET_ROLES = Set.of(
            RoleCode.ADMIN,
            RoleCode.RCI
    );

    private static final Set<String> PROTECTED_RESET_USERNAMES = Set.of(
            "admin",
            "coo",
            "gerant_general"
    );

    private RoleCode parseActiveRoleCode(String roleName) {
        return RoleCode.valueOf(roleName);
    }

    @Override
    public UtilisateurDTO create(CreateUtilisateurRequest request) {
        // Vérifie que l'utilisateur n'existe pas
        if (utilisateurRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Utilisateur avec ce username existe déjà");
        }

        // Génère un mot de passe temporaire s'il n'est pas fourni
        String motDePasse = request.getPassword();
        boolean motDePasseGenere = false;
        if (motDePasse == null || motDePasse.trim().isEmpty()) {
            motDePasse = PasswordGenerator.generateTemporaryPassword();
            motDePasseGenere = true;
            System.out.println("\uD83D\uDD10 Mot de passe temporaire généré automatiquement");
        }

        // Résoudre le rôle avant la validation employe
        RoleCode roleCode = null;
        Role role = null;
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            roleCode = parseActiveRoleCode(request.getRoles().get(0));
            role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new RuntimeException("Rôle non trouvé: " + request.getRoles().get(0)));
        }

        // Validation : rôle opérationnel → employeId obligatoire
        if (roleCode != null && OPERATIONAL_ROLES.contains(roleCode) && request.getEmployeId() == null) {
            throw new RuntimeException(
                "Un employé lié est obligatoire pour le rôle " + roleCode.name()
                + ". Créez d'abord la fiche Employé, puis revenez créer le compte utilisateur.");
        }

        // Crée un nouvel utilisateur
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername(request.getUsername());
        utilisateur.setMotDePasseHash(passwordEncoder.encode(motDePasse));
        utilisateur.setEmail(request.getEmail());
        utilisateur.setActif(true);
        utilisateur.setIsEnabled(true);
        utilisateur.setIsLocked(false);
        utilisateur.setPasswordResetRequired(false);

        if (role != null) {
            utilisateur.setRole(role);
        }

        // Lier l'employé si fourni
        if (request.getEmployeId() != null) {
            Employe employe = employeRepository.findById(request.getEmployeId())
                    .orElseThrow(() -> new RuntimeException("Employé non trouvé: id=" + request.getEmployeId()));

            // L'employé doit être actif
            if (!Boolean.TRUE.equals(employe.getActif())) {
                throw new RuntimeException("L'employé sélectionné est inactif. Seuls les employés actifs peuvent recevoir un compte.");
            }

            // Unicité : vérifier qu'aucun utilisateur n'est déjà lié à cet employé
            if (employe.getUtilisateur() != null) {
                throw new RuntimeException("Cet employé est déjà associé à un compte utilisateur");
            }

            // Copie l'identité depuis l'employé (source de vérité)
            utilisateur.setNomComplet(employe.getNomComplet());
            utilisateur.setTelephone(employe.getTelephone());

            // Validation BLOQUANTE : cohérence rôle applicatif / fonction métier
            validateRoleMatchesEmployeFunction(roleCode, employe);
            validateSiteForAgentTerrain(roleCode, employe);

            utilisateur.setEmploye(employe);
        } else {
            // Pas d'employé lié (ADMIN ou MEMBER) : utiliser les champs du request
            utilisateur.setNomComplet(request.getNomComplet());
            utilisateur.setTelephone(request.getTelephone());
        }

        Utilisateur saved = utilisateurRepository.save(utilisateur);
        syncAgentTerrain(saved);

        System.out.println("\uD83D\uDCE7 Utilisateur créé: " + saved.getUsername());

        return toDTO(saved);
    }

    @Override
    public UtilisateurDTO update(Long id, UpdateUtilisateurRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (request.getEmail() != null) {
            utilisateur.setEmail(request.getEmail());
        }
        if (request.getNomComplet() != null) {
            utilisateur.setNomComplet(request.getNomComplet());
        }
        if (request.getTelephone() != null) {
            utilisateur.setTelephone(request.getTelephone());
        }
        if (request.getActive() != null) {
            utilisateur.setActif(request.getActive());
        }

        // Assigne le premier rôle si disponible
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            String roleName = request.getRoles().get(0);
            RoleCode roleCode = parseActiveRoleCode(roleName);
            Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new RuntimeException("Rôle non trouvé: " + roleCode.name()));
            utilisateur.setRole(role);
        }

        // Lier un employé si fourni
        if (request.getEmployeId() != null && utilisateur.getEmploye() == null) {
            Employe employe = employeRepository.findById(request.getEmployeId())
                    .orElseThrow(() -> new RuntimeException("Employé non trouvé: id=" + request.getEmployeId()));
            utilisateur.setEmploye(employe);
        }

        RoleCode roleCode = utilisateur.getRole() != null ? utilisateur.getRole().getCode() : null;
        if (utilisateur.getEmploye() != null) {
            validateRoleMatchesEmployeFunction(roleCode, utilisateur.getEmploye());
            validateSiteForAgentTerrain(roleCode, utilisateur.getEmploye());
        }

        Utilisateur updated = utilisateurRepository.save(utilisateur);
        syncAgentTerrain(updated);
        return toDTO(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public UtilisateurDTO getById(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return toDTO(utilisateur);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UtilisateurDTO> getAll() {
        return utilisateurRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UtilisateurDTO> getByRole(RoleCode roleCode) {
        return utilisateurRepository.findAll().stream()
                .filter(u -> u.getActif() != null && u.getActif())
                .filter(u -> u.getRole() != null && u.getRole().getCode() == roleCode)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        if (!utilisateurRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur non trouvé");
        }
        utilisateurRepository.deleteById(id);
    }

    /**
     * Valide que le r\u00f4le applicatif correspond \u00e0 la fonction m\u00e9tier de l'employ\u00e9.
     * <p>
     * R\u00e8gles :
     * <ul>
     *   <li>ADMIN et MEMBER sont exempts (pas de mapping d\u00e9fini).</li>
     *   <li>Tout autre r\u00f4le op\u00e9rationnel doit correspondre exactement.</li>
     * </ul>
    * Exemples valides : GESTIONNAIRE + GESTIONNAIRE, CHEF_BUREAU + CHEF_BUREAU.
     * Exemples refus\u00e9s : CAISSIER + CONTROLEUR, AGENT_TERRAIN + GESTIONNAIRE.
     *
     * @throws RuntimeException si le r\u00f4le ne correspond pas \u00e0 la fonction
     */
    private void validateRoleMatchesEmployeFunction(RoleCode roleCode, Employe employe) {
        if (roleCode == null || !ROLE_FONCTION_ATTENDUE.containsKey(roleCode)) {
            // ADMIN, MEMBER ou r\u00f4le sans mapping d\u00e9fini \u2014 validation ignor\u00e9e
            return;
        }
        PosteEmploye fonctionAttendue = ROLE_FONCTION_ATTENDUE.get(roleCode);
        if (employe.getFonction() != fonctionAttendue) {
            throw new RuntimeException(
                "Le r\u00f4le s\u00e9lectionn\u00e9 ne correspond pas \u00e0 la fonction de l'employ\u00e9 li\u00e9. "
                + "R\u00f4le " + roleCode.name() + " attend la fonction " + fonctionAttendue.name()
                + " mais l'employ\u00e9 a la fonction "
                + (employe.getFonction() != null ? employe.getFonction().name() : "null")
                + ". V\u00e9rifiez la fiche employ\u00e9 ou choisissez le r\u00f4le correspondant.");
        }
    }

    private void validateSiteForAgentTerrain(RoleCode roleCode, Employe employe) {
        if (roleCode == RoleCode.AGENT_TERRAIN && employe.getSite() == null) {
            throw new RuntimeException("Le site est obligatoire pour un Agent Terrain.");
        }
    }

    private void syncAgentTerrain(Utilisateur utilisateur) {
        if (utilisateur == null || utilisateur.getId() == null) {
            return;
        }

        RoleCode roleCode = utilisateur.getRole() != null ? utilisateur.getRole().getCode() : null;
        Employe employe = utilisateur.getEmploye();
        boolean agentTerrainValide = roleCode == RoleCode.AGENT_TERRAIN
                && Boolean.TRUE.equals(utilisateur.getActif())
                && Boolean.TRUE.equals(utilisateur.getIsEnabled())
                && !Boolean.TRUE.equals(utilisateur.getIsLocked())
                && !Boolean.TRUE.equals(utilisateur.getAccountLocked())
                && employe != null
                && Boolean.TRUE.equals(employe.getActif())
                && employe.getFonction() == PosteEmploye.AGENT_TERRAIN
                && employe.getSite() != null;

        Optional<AgentTerrain> existingAgent = agentTerrainRepository.findByUtilisateurId(utilisateur.getId());

        if (!agentTerrainValide) {
            existingAgent.ifPresent(agent -> {
                agent.setActif(false);
                agentTerrainRepository.save(agent);
            });
            return;
        }

        AgentTerrain agent = existingAgent.orElseGet(AgentTerrain::new);
        agent.setUtilisateur(utilisateur);
        agent.setSite(employe.getSite());
        agent.setMatricule(resolveAgentTerrainMatricule(utilisateur, employe));
        agent.setActif(true);
        agentTerrainRepository.save(agent);
    }

    private String resolveAgentTerrainMatricule(Utilisateur utilisateur, Employe employe) {
        String matricule = employe.getMatricule();
        if (matricule == null || matricule.isBlank()) {
            matricule = "AT-" + utilisateur.getUsername();
        }
        matricule = matricule.trim().toUpperCase();
        return matricule.length() <= 50 ? matricule : matricule.substring(0, 50);
    }

    private UtilisateurDTO toDTO(Utilisateur utilisateur) {
        List<String> roles = utilisateur.getRole() != null ?
                List.of(utilisateur.getRole().getCode().name()) : List.of();

        Employe employe = utilisateur.getEmploye();

        return UtilisateurDTO.builder()
                .id(utilisateur.getId())
                .username(utilisateur.getUsername())
                .email(utilisateur.getEmail())
                .nomComplet(utilisateur.getNomComplet())
                .telephone(utilisateur.getTelephone())
                .active(utilisateur.getActif())
                .roles(roles)
                .passwordResetRequired(utilisateur.getPasswordResetRequired())
                .passwordChangeRequired(utilisateur.getPasswordChangeRequired())
                .employeId(employe != null ? employe.getId() : null)
                .employeMatricule(employe != null ? employe.getMatricule() : null)
                .employeNomComplet(employe != null ? employe.getNomComplet() : null)
                .employeFonction(employe != null && employe.getFonction() != null
                        ? employe.getFonction().name() : null)
                .employeTelephone(employe != null ? employe.getTelephone() : null)
                .employeAgenceNom(employe != null && employe.getAgence() != null
                        ? employe.getAgence().getNomAgence() : null)
                .employeSiteNom(employe != null && employe.getSite() != null
                        ? employe.getSite().getNomSite() : null)
                .dateCreation(utilisateur.getDateCreation())
                .dateModification(utilisateur.getDateModification())
                .build();
    }

    /**
     * Crée une entité Utilisateur et l'enregistre en BD
     * Utilisé pour créer des utilisateurs depuis un processus parent (ex: création du Membre)
     * @param request Les données de l'utilisateur
     * @return L'entité Utilisateur créée et persistée
     */
    @Transactional
    public Utilisateur createRawEntity(CreateUtilisateurRequest request) {
        // Vérifie que l'utilisateur n'existe pas
        if (utilisateurRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Utilisateur avec ce username existe déjà");
        }

        // Génère un mot de passe temporaire s'il n'est pas fourni
        String motDePasse = request.getPassword();
        boolean motDePasseGenere = false;
        if (motDePasse == null || motDePasse.trim().isEmpty()) {
            motDePasse = com.mini.credit.util.PasswordGenerator.generateTemporaryPassword();
            motDePasseGenere = true;
            System.out.println("🔐 Mot de passe temporaire généré automatiquement");
        }

        // Crée un nouvel utilisateur
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername(request.getUsername());
        utilisateur.setMotDePasseHash(passwordEncoder.encode(motDePasse));
        utilisateur.setEmail(request.getEmail());
        utilisateur.setNomComplet(request.getNomComplet());
        utilisateur.setTelephone(request.getTelephone());
        utilisateur.setActif(true);
        utilisateur.setIsEnabled(true);
        
        // Met le flag pour forcer le changement de mot de passe à la première connexion
        utilisateur.setPasswordResetRequired(true);
        utilisateur.setPasswordResetToken(com.mini.credit.util.PasswordGenerator.generateResetToken());

        // Assigne le premier rôle si disponible
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            String roleName = request.getRoles().get(0);
            com.mini.credit.enums.security.RoleCode roleCode = parseActiveRoleCode(roleName);
            com.mini.credit.entity.referentiel.Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new RuntimeException("Rôle non trouvé: " + roleCode.name()));
            utilisateur.setRole(role);
        }

        Utilisateur saved = utilisateurRepository.save(utilisateur);

        // 📧 Email envoyé désactivé - Le système d'activation par code n'en dépend pas
        // L'utilisateur recevra un code d'activation unique à la place
        System.out.println("📧 Utilisateur créé - Code d'activation sera envoyé via le système d'activation");

        return saved;
    }

    /**
     * Crée une entité Utilisateur et retourne ses credentials temporaires
     * Utilisé pour les création de membres avec affichage du mot de passe
     * @param request Les données de l'utilisateur
     * @return Wrapper contenant le username et le mot de passe temporaire
     */
    @Transactional
    public com.mini.credit.dto.utilisateur.CreateUtilisateurWithCredentialsResponse createRawEntityWithCredentials(CreateUtilisateurRequest request) {
        // Crée d'abord l'utilisateur
        Utilisateur utilisateur = createRawEntity(request);
        
        // Le mot de passe a été généré dans createRawEntity, on doit le régénérer pour le retourner
        // (Il est hashé en BD donc on ne peut pas le récupérer)
        String motDePasse = request.getPassword();
        if (motDePasse == null || motDePasse.trim().isEmpty()) {
            motDePasse = com.mini.credit.util.PasswordGenerator.generateTemporaryPassword();
        }

        // Retourner le wrapper avec les credentials
        return com.mini.credit.dto.utilisateur.CreateUtilisateurWithCredentialsResponse.builder()
                .username(utilisateur.getUsername())
                .email(utilisateur.getEmail())
                .temporaryPassword(motDePasse)
                .message("✅ Compte créé! Mot de passe temporaire généré")
                .build();
    }

    @Override
    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Valider que les mots de passe correspondent
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        // Valider que le mot de passe actuel est correct
        if (!passwordEncoder.matches(request.resolveCurrentPassword(), utilisateur.getMotDePasseHash())) {
            throw new RuntimeException("Le mot de passe actuel est incorrect");
        }

        // Mettre à jour le mot de passe
        utilisateur.setMotDePasseHash(passwordEncoder.encode(request.getNewPassword()));
        utilisateur.setPasswordResetRequired(false);
        utilisateur.setPasswordChangeRequired(false);
        utilisateur.setCredentialsVersion((utilisateur.getCredentialsVersion() == null ? 0 : utilisateur.getCredentialsVersion()) + 1);
        utilisateurRepository.save(utilisateur);

        auditService.logSuccess(
                AuditAction.USER_PASSWORD_CHANGED,
                "Utilisateur",
                utilisateur.getId(),
                "Changement de mot de passe via endpoint legacy"
        );
    }

    @Override
    @Transactional
    public void changeOwnPassword(ChangePasswordRequest request) {
        Utilisateur currentUser = SecurityUtils.getCurrentUserOrThrow();

        if (request == null) {
            throw new RuntimeException("La requête est obligatoire");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        if (!passwordEncoder.matches(request.resolveCurrentPassword(), currentUser.getMotDePasseHash())) {
            throw new RuntimeException("Le mot de passe actuel est incorrect");
        }

        currentUser.setMotDePasseHash(passwordEncoder.encode(request.getNewPassword()));
        currentUser.setPasswordResetRequired(false);
        currentUser.setPasswordChangeRequired(false);
        currentUser.setPasswordResetToken(null);
        currentUser.setFailedLoginAttempts(0);
        currentUser.setAccountLocked(false);
        currentUser.setCredentialsVersion((currentUser.getCredentialsVersion() == null ? 0 : currentUser.getCredentialsVersion()) + 1);
        utilisateurRepository.save(currentUser);

        auditService.logSuccess(
                AuditAction.USER_PASSWORD_CHANGED,
                "Utilisateur",
                currentUser.getId(),
                "Changement de mot de passe utilisateur courant"
        );
    }

    /**
     * Réinitialiser le mot de passe sans demander l'ancien
     * Seulement accessible quand l'utilisateur est authentifié (JWT valide)
     * Idéal pour les utilisateurs qui ont oublié leur mot de passe
     * @param id L'ID de l'utilisateur
     * @param request Contient le nouveau mot de passe
     */
    @Override
    @Transactional
    public void resetPasswordSelf(Long id, com.mini.credit.dto.utilisateur.ResetPasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (isSensitiveAccount(utilisateur)) {
            throw new RuntimeException("Ce compte doit utiliser le changement de mot de passe avec ancien mot de passe.");
        }

        // Valider que les mots de passe correspondent
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        // Valider la longueur du mot de passe
        if (request.getNewPassword().length() < 8) {
            throw new RuntimeException("Le mot de passe doit avoir au moins 8 caractères");
        }

        // Mettre à jour le mot de passe
        utilisateur.setMotDePasseHash(passwordEncoder.encode(request.getNewPassword()));
        utilisateur.setPasswordResetRequired(false);
        utilisateur.setPasswordChangeRequired(false);
        utilisateur.setCredentialsVersion((utilisateur.getCredentialsVersion() == null ? 0 : utilisateur.getCredentialsVersion()) + 1);
        utilisateurRepository.save(utilisateur);

        auditService.logSuccess(
            AuditAction.USER_PASSWORD_CHANGED,
            "Utilisateur",
            utilisateur.getId(),
            "Réinitialisation self du mot de passe"
        );

        System.out.println("✓ Mot de passe réinitialisé avec succès pour l'utilisateur ID " + id);
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPasswordByAdmin(Long targetUserId, AdminPasswordResetRequest request) {
        Utilisateur actor = SecurityUtils.getCurrentUserOrThrow();
        Utilisateur target = utilisateurRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Utilisateur cible non trouvé"));

        if (request == null || request.getMotif() == null || request.getMotif().isBlank()) {
            throw new RuntimeException("Le motif de réinitialisation est obligatoire");
        }
        String motif = request.getMotif().trim();
        if (motif.length() < 5) {
            throw new RuntimeException("Le motif de réinitialisation est obligatoire (min 5 caractères)");
        }

        validateResetAuthorization(actor, target);

        String temporaryPassword = PasswordGenerator.generateTemporaryPassword();
        target.setMotDePasseHash(passwordEncoder.encode(temporaryPassword));
        target.setPasswordResetRequired(true);
        target.setPasswordChangeRequired(true);
        target.setPasswordResetToken(PasswordGenerator.generateResetToken());
        target.setLastPasswordResetAt(java.time.LocalDateTime.now());
        target.setLastPasswordResetBy(actor);
        target.setFailedLoginAttempts(0);
        target.setAccountLocked(false);
        target.setCredentialsVersion((target.getCredentialsVersion() == null ? 0 : target.getCredentialsVersion()) + 1);
        utilisateurRepository.save(target);

        String actorRole = actor.getRole() != null ? actor.getRole().getCode().name() : "UNKNOWN";
        String siteInfo = actor.getSite() != null ? actor.getSite().getNomSite() :
            (actor.getEmploye() != null && actor.getEmploye().getSite() != null ? actor.getEmploye().getSite().getNomSite() : "SITE_INCONNU");
        auditService.logSuccess(
                AuditAction.USER_PASSWORD_RESET,
                "Utilisateur",
                target.getId(),
            "Reset mot de passe | actor=" + actor.getUsername()
                + " | role=" + actorRole
                + " | site=" + siteInfo
                + " | target=" + target.getUsername()
            + " | motif=" + motif
        );

        return ResetPasswordResponse.builder()
                .username(target.getUsername())
                .temporaryPassword(temporaryPassword)
                .passwordChangeRequired(true)
            .advisoryMessage("Ce mot de passe temporaire est affiché une seule fois. Il doit être transmis immédiatement à l'utilisateur.")
                .build();
    }

    private void validateResetAuthorization(Utilisateur actor, Utilisateur target) {
        RoleCode actorRole = actor.getRole() != null ? actor.getRole().getCode() : null;

        if (isSensitiveAccount(target)) {
            throw new RuntimeException("La réinitialisation de ce compte sensible est interdite par l’endpoint standard.");
        }

        if (actorRole == null) {
            throw new RuntimeException("Rôle utilisateur courant introuvable");
        }

        if (actorRole == RoleCode.ADMIN) {
            return;
        }

        if (actorRole != RoleCode.CHEF_BUREAU) {
            throw new RuntimeException("Vous n'êtes pas autorisé à réinitialiser le mot de passe d'autres utilisateurs");
        }

        Long actorSiteId = resolveSiteId(actor);
        Long targetSiteId = resolveSiteId(target);
        if (actorSiteId == null || targetSiteId == null || !actorSiteId.equals(targetSiteId)) {
            throw new RuntimeException("Réinitialisation refusée: utilisateur hors périmètre site/antenne");
        }
    }

    private boolean isSensitiveAccount(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return false;
        }

        String username = utilisateur.getUsername();
        if (username != null && PROTECTED_RESET_USERNAMES.contains(username.trim().toLowerCase())) {
            return true;
        }

        RoleCode roleCode = utilisateur.getRole() != null ? utilisateur.getRole().getCode() : null;
        if (roleCode == null) {
            return false;
        }

        if (PROTECTED_RESET_ROLES.contains(roleCode)) {
            return true;
        }

        String roleName = roleCode.name();
        return "COO".equals(roleName) || "GERANT_GENERAL".equals(roleName);
    }

    private Long resolveSiteId(Utilisateur utilisateur) {
        if (utilisateur.getSite() != null && utilisateur.getSite().getId() != null) {
            return utilisateur.getSite().getId();
        }
        if (utilisateur.getEmploye() != null
                && utilisateur.getEmploye().getSite() != null
                && utilisateur.getEmploye().getSite().getId() != null) {
            return utilisateur.getEmploye().getSite().getId();
        }
        return null;
    }

    /**
     * Permet à un utilisateur de changer son username
     * Utilisé par les membres pour personnaliser leur nom d'utilisateur
     * @param id L'ID de l'utilisateur
     * @param request Contient le nouveau username
     * @return L'utilisateur mis à jour
     */
    @Override
    @Transactional
    public UtilisateurDTO changeUsername(Long id, com.mini.credit.dto.utilisateur.ChangeUsernameRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String newUsername = request.getNewUsername().trim().toLowerCase();

        // Valider que le nouveau username n'existe pas déjà
        if (utilisateurRepository.existsByUsername(newUsername)) {
            throw new RuntimeException("Ce username est déjà utilisé. Veuillez en choisir un autre.");
        }

        // Valider le format du username (alphanumériques, points, tirets)
        if (!newUsername.matches("^[a-z0-9._-]+$")) {
            throw new RuntimeException("Le username ne peut contenir que des lettres, chiffres, points, tirets et underscores");
        }

        // Mettre à jour l'username
        utilisateur.setUsername(newUsername);
        Utilisateur updated = utilisateurRepository.save(utilisateur);

        System.out.println("✓ Username changé avec succès pour l'utilisateur ID " + id + " → " + newUsername);

        return toDTO(updated);
    }

    /**
     * Vérifie si un username existe déjà dans le système
     * Utilisé pour la validation en temps réel lors de la saisie
     * @param username Le username à vérifier
     * @return true si le username existe, false sinon
     */
    @Override
    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        return utilisateurRepository.existsByUsername(username.trim().toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UtilisateurDTO> getDisponiblesAgentTerrain() {
        // IDs des utilisateurs déjà associés à un profil AgentTerrain
        Set<Long> dejaAgents = agentTerrainRepository.findAll().stream()
                .map(at -> at.getUtilisateur().getId())
                .collect(Collectors.toSet());

        return utilisateurRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getActif()))
                .filter(u -> u.getRole() != null
                          && u.getRole().getCode() == RoleCode.AGENT_TERRAIN)
                .filter(u -> u.getEmploye() != null
                          && u.getEmploye().getFonction() == PosteEmploye.AGENT_TERRAIN)
                .filter(u -> !dejaAgents.contains(u.getId()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
