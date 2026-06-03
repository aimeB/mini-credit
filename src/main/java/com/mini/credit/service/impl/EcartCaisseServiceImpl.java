package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.EcartCaisseDTO;
import com.mini.credit.entity.caisse.EcartCaisse;
import com.mini.credit.entity.caisse.RecetteJournaliereTerrain;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutEcartCaisse;
import com.mini.credit.enums.TypeEcartCaisse;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.EcartCaisseMapper;
import com.mini.credit.repository.caisse.EcartCaisseRepository;
import com.mini.credit.repository.caisse.RecetteJournaliereTerrainRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.service.EcartCaisseService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PHASE 7: Implémentation service écarts caisse.
 *
 * Logique:
 * 1. Détection automatique variance (recettes, session)
 * 2. Investigation CONTROLEUR (notes, statut)
 * 3. Validation R.C.I. si montant > seuil
 */
@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class EcartCaisseServiceImpl implements EcartCaisseService {

    private final EcartCaisseRepository ecartRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final RecetteJournaliereTerrainRepository recetteRepository;
    private final EcartCaisseMapper ecartMapper;

    /**
     * PHASE 7: Détecte et crée un écart caisse
     */
    @Override
    public EcartCaisseDTO detecterEcart(
            Long sessionCaisseId, Long recetteId, LocalDate dateJour, String typeEcart,
            BigDecimal montantEcart, String description, Boolean seuilDepassé) {

        log.info("Détection écart caisse: date={}, type={}, montant={}, seuil={}",
                dateJour, typeEcart, montantEcart, seuilDepassé);

        // Validations
        if (montantEcart == null || montantEcart.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant d'écart doit être > 0");
        }

        // Récupère session si fournie
        SessionCaisse sessionCaisse = null;
        if (sessionCaisseId != null) {
            sessionCaisse = sessionCaisseRepository.findById(sessionCaisseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Session caisse non trouvée: " + sessionCaisseId));
        }

        // Récupère recette si fournie
        RecetteJournaliereTerrain recette = null;
        if (recetteId != null) {
            recette = recetteRepository.findById(recetteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Recette non trouvée: " + recetteId));
        }

        // Crée l'écart
        EcartCaisse ecart = EcartCaisse.builder()
                .sessionCaisse(sessionCaisse)
                .recette(recette)
                .dateJour(dateJour)
                .typeEcart(TypeEcartCaisse.valueOf(typeEcart))
                .montantEcart(montantEcart)
                .description(description)
                .statut(StatutEcartCaisse.DETECTE)
                .seuilDepassé(seuilDepassé != null ? seuilDepassé : false)
                .build();

        ecart = ecartRepository.save(ecart);
        log.info("Écart créé: id={}, montant={}, statut={}", ecart.getId(), ecart.getMontantEcart(), ecart.getStatut());

        return ecartMapper.toDTO(ecart);
    }

    /**
     * Récupère un écart par ID
     */
    @Override
    public EcartCaisseDTO getById(Long id) {
        EcartCaisse ecart = ecartRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Écart non trouvé: " + id));

        return ecartMapper.toDTO(ecart);
    }

    /**
     * Récupère les écarts détectés pour un jour
     */
    @Override
    public List<EcartCaisseDTO> getByDateJour(LocalDate dateJour) {
        List<EcartCaisse> ecarts = ecartRepository.findByDateJourOrderByMontantEcartDesc(dateJour);

        return ecarts.stream()
                .map(ecartMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les écarts en investigation (CONTROLEUR)
     */
    @Override
    public List<EcartCaisseDTO> getEnInvestigation() {
        List<EcartCaisse> ecarts = ecartRepository.findByStatutInOrderByDateJourDesc(
                Arrays.asList(StatutEcartCaisse.DETECTE, StatutEcartCaisse.EN_INVESTIGATION));

        return ecarts.stream()
                .map(ecartMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les écarts nécessitant validation R.C.I.
     */
    @Override
    public List<EcartCaisseDTO> getRequiringRCIValidation() {
        List<EcartCaisse> ecarts = ecartRepository.findEcartsRequiringRCIValidation(
                Arrays.asList(StatutEcartCaisse.DETECTE, StatutEcartCaisse.EN_INVESTIGATION));

        return ecarts.stream()
                .map(ecartMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * PHASE 7: Enquête d'un écart (CONTROLEUR)
     * Passe en EN_INVESTIGATION
     */
    @Override
    public EcartCaisseDTO enqueterEcart(Long ecartId, String notesInvestigation) {
        log.info("Enquête écart: id={}", ecartId);

        EcartCaisse ecart = ecartRepository.findById(ecartId)
                .orElseThrow(() -> new ResourceNotFoundException("Écart non trouvé: " + ecartId));

        ecart.setStatut(StatutEcartCaisse.EN_INVESTIGATION);
        ecart.setNotesInvestigation(notesInvestigation);
        ecart.setEnquetePar(getCurrentUtilisateur());
        ecart.setDateEnquete(LocalDateTime.now());

        ecart = ecartRepository.save(ecart);
        log.info("Écart en investigation: id={}", ecartId);

        return ecartMapper.toDTO(ecart);
    }

    /**
     * PHASE 7: Résout un écart (CONTROLEUR)
     * Passe en RESOLU
     */
    @Override
    public EcartCaisseDTO resoudreEcart(Long ecartId, String raisonResolution) {
        log.info("Résolution écart: id={}, raison={}", ecartId, raisonResolution);

        EcartCaisse ecart = ecartRepository.findById(ecartId)
                .orElseThrow(() -> new ResourceNotFoundException("Écart non trouvé: " + ecartId));

        if (!ecart.canBeResolved()) {
            throw new BusinessException("Écart ne peut pas être résolu: statut=" + ecart.getStatut());
        }

        ecart.setStatut(StatutEcartCaisse.RESOLU);
        ecart.setRaisonResolution(raisonResolution);
        ecart.setEnquetePar(getCurrentUtilisateur());
        ecart.setDateEnquete(LocalDateTime.now());

        ecart = ecartRepository.save(ecart);
        log.info("Écart résolu: id={}", ecartId);

        return ecartMapper.toDTO(ecart);
    }

    /**
     * PHASE 7: Accepte un écart (R.C.I.)
     * Passe en ACCEPTE (variance acceptée comme normale)
     */
    @Override
    public EcartCaisseDTO accepterEcart(Long ecartId) {
        log.info("Acceptation écart: id={}", ecartId);

        EcartCaisse ecart = ecartRepository.findById(ecartId)
                .orElseThrow(() -> new ResourceNotFoundException("Écart non trouvé: " + ecartId));

        if (!ecart.requiresRCIValidation()) {
            throw new BusinessException("Écart ne nécessite pas validation R.C.I.: montant=" + ecart.getMontantEcart());
        }

        ecart.setStatut(StatutEcartCaisse.ACCEPTE);
        ecart.setValidePar(getCurrentUtilisateur());
        ecart.setDateValidation(LocalDateTime.now());

        ecart = ecartRepository.save(ecart);
        log.info("Écart accepté R.C.I.: id={}", ecartId);

        return ecartMapper.toDTO(ecart);
    }

    /**
     * Rejette un écart (présumé erreur système)
     */
    @Override
    public EcartCaisseDTO rejeterEcart(Long ecartId) {
        log.info("Rejet écart: id={}", ecartId);

        EcartCaisse ecart = ecartRepository.findById(ecartId)
                .orElseThrow(() -> new ResourceNotFoundException("Écart non trouvé: " + ecartId));

        ecart.setStatut(StatutEcartCaisse.REJETE);
        ecart = ecartRepository.save(ecart);

        return ecartMapper.toDTO(ecart);
    }

    /**
     * Total écarts non résolus pour un jour
     */
    @Override
    public BigDecimal getTotalEcartsNonResolusByDateJour(LocalDate dateJour) {
        BigDecimal total = ecartRepository.sumEcartsNonResolusByDateJour(dateJour);
        return total != null ? total : BigDecimal.ZERO;
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
