package com.mini.credit.service;

import com.mini.credit.dto.auth.ActivationCodeDTO;
import com.mini.credit.dto.auth.ActivationRequest;
import com.mini.credit.dto.auth.AuthResponse;
import com.mini.credit.entity.referentiel.ActivationToken;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.repository.ActivationTokenRepository;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.security.JwtProvider;
import com.mini.credit.util.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

/**
 * Service de gestion des activations de compte
 * Génère et valide les codes d'activation uniques
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ActivationService {

    private final ActivationTokenRepository activationTokenRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    
    @Value("${app.jwt.expiration:86400}")
    private long jwtExpiration;

    /**
     * Générer un code d'activation unique pour un utilisateur
     * Format: MBR-XXXXX-20260412 (12 caractères)
     */
    public ActivationCodeDTO generateActivationCode(Utilisateur utilisateur) {
        log.info("Génération du code d'activation pour l'utilisateur: {}", utilisateur.getUsername());
        
        // Annuler les anciens tokens non utilisés
        Optional<ActivationToken> existingToken = activationTokenRepository
                .findActiveTokenByUtilisateur(utilisateur);
        existingToken.ifPresent(token -> {
            token.setIsUsed(true);
            activationTokenRepository.save(token);
            log.info("Ancien token d'activation invalidé pour: {}", utilisateur.getUsername());
        });
        
        // Générer nouveau code unique
        String code = generateUniqueCode();
        
        // Créer le token
        ActivationToken token = ActivationToken.builder()
                .code(code)
                .utilisateur(utilisateur)
                .isUsed(false)
                .build();
        
        activationTokenRepository.save(token);
        log.info("Token d'activation créé: {}", code);
        
        // Améliorer le nom pour display
        String fullName = utilisateur.getNomComplet() != null ? utilisateur.getNomComplet() : utilisateur.getUsername();
        
        return ActivationCodeDTO.of(code, fullName, utilisateur.getEmail());
    }

    /**
     * Activer un compte avec code + nouveau password
     * Retourne un JWT Token pour auto-login (AuthResponse compatible avec le frontend)
     */
    public AuthResponse activateAccount(ActivationRequest request, String ipAddress) {
        log.info("Tentative d'activation avec code: {}", request.getActivationCode());
        
        // Valider les passwords
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Les mots de passe ne correspondent pas");
        }
        
        if (request.getNewPassword().length() < 8) {
            throw new BusinessException("Le mot de passe doit avoir au moins 8 caractères");
        }
        
        // Récupérer et valider le token
        ActivationToken token = activationTokenRepository.findByCode(request.getActivationCode())
                .orElseThrow(() -> new ResourceNotFoundException("Code d'activation invalide"));
        
        if (token.getIsUsed()) {
            log.warn("Tentative d'utilisation d'un code déjà utilisé: {}", request.getActivationCode());
            throw new BusinessException("Ce code d'activation a déjà été utilisé");
        }
        
        if (token.isExpired()) {
            log.warn("Tentative avec code expiré: {}", request.getActivationCode());
            throw new BusinessException("Le code d'activation a expiré (validité: 48h)");
        }
        
        // Récupérer et mettre à jour l'utilisateur
        Utilisateur utilisateur = token.getUtilisateur();
        utilisateur.setMotDePasseHash(passwordEncoder.encode(request.getNewPassword()));
        utilisateur.setPasswordResetRequired(false); // Compte complètement activé
        utilisateur.setActif(true);
        
        utilisateurRepository.save(utilisateur);
        
        // Marquer le token comme utilisé
        token.setIsUsed(true);
        token.setDateUtilisation(LocalDateTime.now());
        token.setIpActivation(ipAddress);
        activationTokenRepository.save(token);
        
        log.info("Compte activé avec succès pour: {}", utilisateur.getUsername());
        
        // Générer JWT pour auto-login
        String jwtToken = jwtProvider.generateToken(utilisateur);
        
        // Retourner AuthResponse (compatible avec le frontend)
        return AuthResponse.from(jwtToken, utilisateur);
    }

    /**
     * Regénérer un code d'activation (Admin seulement)
     * Utilisé quand un utilisateur a perdu ou refusé le code
     */
    public ActivationCodeDTO regenerateActivationCode(Long utilisateurId) {
        log.info("Regénération du code d'activation pour utilisateur ID: {}", utilisateurId);
        
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        // Vérifier que le compte n'est pas encore activé
        if (!utilisateur.getPasswordResetRequired() && utilisateur.getActif()) {
            log.warn("Tentative de regénération pour un compte déjà actif: {}", utilisateur.getUsername());
            throw new BusinessException("Ce compte est déjà activé");
        }
        
        return generateActivationCode(utilisateur);
    }

    /**
     * Générer un code unique aléatoire
     * Format: MBR-XXXXX-YYYYMMDD (12 caractères)
     */
    private String generateUniqueCode() {
        String code;
        boolean exists;
        Random random = new Random();
        
        do {
            // Générer: MBR-5CHARS-DATE
            String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));
            String randomPart = String.format("%05d", random.nextInt(100000));
            code = "MBR-" + randomPart + "-" + date;
            
            // Vérifier l'unicité
            exists = activationTokenRepository.existsByCodeAndIsUsedFalse(code);
        } while (exists);
        
        return code;
    }
}
