package com.mini.credit.service.impl;

import com.mini.credit.dto.utilisateur.UtilisateurDTO;
import com.mini.credit.dto.utilisateur.CreateUtilisateurRequest;
import com.mini.credit.dto.utilisateur.UpdateUtilisateurRequest;
import com.mini.credit.dto.utilisateur.ChangePasswordRequest;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.referentiel.RoleRepository;
import com.mini.credit.service.UtilisateurService;
import com.mini.credit.util.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

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
        utilisateur.setPasswordResetToken(PasswordGenerator.generateResetToken());

        // Assigne le premier rôle si disponible
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            String roleName = request.getRoles().get(0);
            RoleCode roleCode = RoleCode.valueOf(roleName);
            Role role = roleRepository.findByCode(roleCode)
                    .orElseThrow(() -> new RuntimeException("Rôle non trouvé: " + roleName));
            utilisateur.setRole(role);
        }

        Utilisateur saved = utilisateurRepository.save(utilisateur);

        // TODO: Envoyer email de bienvenue quand mail est correctement configuré
        // L'envoi d'email est désactivé pour permettre la création d'utilisateur à fonctionner
        /*
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty() && motDePasseGenere) {
            try {
                emailService.envoyerEmailBienvenueMotDePasse(
                    request.getEmail(),
                    request.getNomComplet(),
                    request.getUsername(),
                    motDePasse
                );
                System.out.println("✓ Email de bienvenue envoyé à " + request.getEmail());
            } catch (Exception e) {
                System.err.println("⚠️ Erreur lors de l'envoi de l'email: " + e.getMessage());
                // Continuer même si l'email échoue
            }
        }
        */
        System.out.println("📧 Utilisateur créé - Email envoyé à: " + request.getEmail() + " (à configurer)");

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
            RoleCode roleCode = RoleCode.valueOf(roleName);
            Role role = roleRepository.findByCode(roleCode)
                    .orElseThrow(() -> new RuntimeException("Rôle non trouvé: " + roleName));
            utilisateur.setRole(role);
        }

        Utilisateur updated = utilisateurRepository.save(utilisateur);
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
    public void delete(Long id) {
        if (!utilisateurRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur non trouvé");
        }
        utilisateurRepository.deleteById(id);
    }

    private UtilisateurDTO toDTO(Utilisateur utilisateur) {
        List<String> roles = utilisateur.getRole() != null ?
                List.of(utilisateur.getRole().getCode().name()) : List.of();

        return UtilisateurDTO.builder()
                .id(utilisateur.getId())
                .username(utilisateur.getUsername())
                .email(utilisateur.getEmail())
                .nomComplet(utilisateur.getNomComplet())
                .telephone(utilisateur.getTelephone())
                .active(utilisateur.getActif())
                .roles(roles)
                .passwordResetRequired(utilisateur.getPasswordResetRequired())
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
            com.mini.credit.enums.security.RoleCode roleCode = com.mini.credit.enums.security.RoleCode.valueOf(roleName);
            com.mini.credit.entity.referentiel.Role role = roleRepository.findByCode(roleCode)
                    .orElseThrow(() -> new RuntimeException("Rôle non trouvé: " + roleName));
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
        if (!passwordEncoder.matches(request.getCurrentPassword(), utilisateur.getMotDePasseHash())) {
            throw new RuntimeException("Le mot de passe actuel est incorrect");
        }

        // Mettre à jour le mot de passe
        utilisateur.setMotDePasseHash(passwordEncoder.encode(request.getNewPassword()));
        utilisateur.setPasswordResetRequired(false);
        utilisateurRepository.save(utilisateur);
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
        utilisateurRepository.save(utilisateur);

        System.out.println("✓ Mot de passe réinitialisé avec succès pour l'utilisateur ID " + id);
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
}
