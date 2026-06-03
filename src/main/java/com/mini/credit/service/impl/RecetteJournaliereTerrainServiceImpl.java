package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.RecetteJournaliereTerrainDTO;
import com.mini.credit.entity.caisse.RecetteJournaliereTerrain;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutRecetteJournaliere;
import com.mini.credit.enums.TypeRecette;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.RecetteJournaliereTerrainMapper;
import com.mini.credit.repository.caisse.RecetteJournaliereTerrainRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.utilisateur.UtilisateurRepository;
import com.mini.credit.service.RecetteJournaliereTerrainService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PHASE 6: Implémentation service recettes journalières terrain.
 *
 * Logique:
 * 1. Agent terrain crée recette (papier data → système)
 * 2. Contrôleur encode: montant + cashRemis
 * 3. Validation: réconciliation argent remis = total recettes
 */
@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class RecetteJournaliereTerrainServiceImpl implements RecetteJournaliereTerrainService {

    private final RecetteJournaliereTerrainRepository recetteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final MembreRepository membreRepository;
    private final RecetteJournaliereTerrainMapper recetteMapper;

    /**
     * PHASE 6: Crée une nouvelle recette journalière
     */
    @Override
    public RecetteJournaliereTerrainDTO creerRecette(
            Long agentId, Long membreId, LocalDate dateJour, String typeRecette,
            BigDecimal montant, String observation, String referencePapier) {

        log.info("Création recette journalière: agent={}, membre={}, date={}, montant={}, type={}",
                agentId, membreId, dateJour, montant, typeRecette);

        // Validations
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant de recette doit être > 0");
        }

        // Récupère agent
        Utilisateur agent = utilisateurRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent non trouvé: " + agentId));

        // Récupère membre
        Membre membre = membreRepository.findById(membreId)
                .orElseThrow(() -> new ResourceNotFoundException("Membre non trouvé: " + membreId));

        // Crée la recette
        RecetteJournaliereTerrain recette = RecetteJournaliereTerrain.builder()
                .agent(agent)
                .membre(membre)
                .dateJour(dateJour)
                .typeRecette(TypeRecette.valueOf(typeRecette))
                .montant(montant)
                .statut(StatutRecetteJournaliere.CREEE)
                .observation(observation)
                .referencePapier(referencePapier)
                .build();

        recette = recetteRepository.save(recette);
        log.info("Recette créée: id={}, statut={}", recette.getId(), recette.getStatut());

        return recetteMapper.toDTO(recette);
    }

    /**
     * Récupère une recette par ID
     */
    @Override
    public RecetteJournaliereTerrainDTO getById(Long id) {
        RecetteJournaliereTerrain recette = recetteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recette non trouvée: " + id));

        return recetteMapper.toDTO(recette);
    }

    /**
     * Récupère les recettes d'un jour donné
     */
    @Override
    public List<RecetteJournaliereTerrainDTO> getByDateJour(LocalDate dateJour) {
        List<RecetteJournaliereTerrain> recettes = recetteRepository.findByDateJourOrderByDateCreationAsc(dateJour);

        return recettes.stream()
                .map(recetteMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les recettes d'un agent pour un jour
     */
    @Override
    public List<RecetteJournaliereTerrainDTO> getByAgentAndDateJour(Long agentId, LocalDate dateJour) {
        List<RecetteJournaliereTerrain> recettes = recetteRepository
                .findByAgentIdAndDateJourOrderByDateCreationAsc(agentId, dateJour);

        return recettes.stream()
                .map(recetteMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les recettes en attente de validation (tous les jours)
     */
    @Override
    public List<RecetteJournaliereTerrainDTO> getEnAttenteValidation() {
        List<RecetteJournaliereTerrain> recettes = recetteRepository
                .findByStatutOrderByDateJourDesc(StatutRecetteJournaliere.EN_ATTENTE_VALIDATION);

        return recettes.stream()
                .map(recetteMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les recettes en attente de validation pour un jour
     */
    @Override
    public List<RecetteJournaliereTerrainDTO> getEnAttenteValidationByDateJour(LocalDate dateJour) {
        List<RecetteJournaliereTerrain> recettes = recetteRepository
                .findByDateJourAndStatutOrderByDateCreationAsc(dateJour, StatutRecetteJournaliere.EN_ATTENTE_VALIDATION);

        return recettes.stream()
                .map(recetteMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * PHASE 6: Valide une recette (réconciliation argent remis = montant)
     * 
     * Calcul variance:
     *   variance = abs(cashRemis - montant)
     *   Si variance = 0 → VALIDEE
     *   Si variance > 0 → VALIDEE quand même (mais notifiée dans SessionCaisse)
     */
    @Override
    public RecetteJournaliereTerrainDTO validerRecette(Long recetteId, BigDecimal cashRemis) {
        log.info("Validation recette: id={}, cashRemis={}", recetteId, cashRemis);

        RecetteJournaliereTerrain recette = recetteRepository.findById(recetteId)
                .orElseThrow(() -> new ResourceNotFoundException("Recette non trouvée: " + recetteId));

        if (!recette.canBeValidated()) {
            throw new BusinessException("Recette ne peut pas être validée: statut=" + recette.getStatut());
        }

        // Calcul variance
        BigDecimal variance = cashRemis.subtract(recette.getMontant()).abs();

        log.info("Variance calculée: montant={}, cashRemis={}, variance={}",
                recette.getMontant(), cashRemis, variance);

        // Valide la recette
        recette.setStatut(StatutRecetteJournaliere.VALIDEE);
        recette.setCashRemis(cashRemis);
        recette.setVariance(variance);
        recette.setValidePar(getCurrentUtilisateur());
        recette.setDateValidation(LocalDateTime.now());

        recette = recetteRepository.save(recette);
        log.info("Recette validée: id={}, variance={}", recetteId, variance);

        return recetteMapper.toDTO(recette);
    }

    /**
     * Rejette une recette
     */
    @Override
    public RecetteJournaliereTerrainDTO rejeterRecette(Long recetteId, String motif) {
        log.info("Rejet recette: id={}, motif={}", recetteId, motif);

        RecetteJournaliereTerrain recette = recetteRepository.findById(recetteId)
                .orElseThrow(() -> new ResourceNotFoundException("Recette non trouvée: " + recetteId));

        recette.setStatut(StatutRecetteJournaliere.REJETEE);
        recette.setMotifRejet(motif);
        recette.setValidePar(getCurrentUtilisateur());
        recette.setDateValidation(LocalDateTime.now());

        recette = recetteRepository.save(recette);

        return recetteMapper.toDTO(recette);
    }

    /**
     * Annule une recette
     */
    @Override
    public RecetteJournaliereTerrainDTO annulerRecette(Long recetteId) {
        log.info("Annulation recette: id={}", recetteId);

        RecetteJournaliereTerrain recette = recetteRepository.findById(recetteId)
                .orElseThrow(() -> new ResourceNotFoundException("Recette non trouvée: " + recetteId));

        if (recette.getStatut() == StatutRecetteJournaliere.VALIDEE) {
            throw new BusinessException("Impossible d'annuler une recette validée");
        }

        recette.setStatut(StatutRecetteJournaliere.ANNULEE);
        recette = recetteRepository.save(recette);

        return recetteMapper.toDTO(recette);
    }

    /**
     * Somme des montants pour un jour
     */
    @Override
    public BigDecimal getSommeByDateJour(LocalDate dateJour) {
        BigDecimal somme = recetteRepository.sumMontantByDateJour(dateJour);
        return somme != null ? somme : BigDecimal.ZERO;
    }

    /**
     * Somme des montants validés pour un jour
     */
    @Override
    public BigDecimal getSommeValideeByDateJour(LocalDate dateJour) {
        BigDecimal somme = recetteRepository.sumMontantByDateJourAndStatut(
                dateJour, StatutRecetteJournaliere.VALIDEE);
        return somme != null ? somme : BigDecimal.ZERO;
    }

    /**
     * Récupère l'utilisateur actuellement authentifié
     */
    private Utilisateur getCurrentUtilisateur() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Utilisateur) {
            return (Utilisateur) authentication.getPrincipal();
        }
        return null;
    }
}
