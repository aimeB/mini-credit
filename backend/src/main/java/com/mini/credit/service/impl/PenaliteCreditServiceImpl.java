package com.mini.credit.service.impl;

import com.mini.credit.dto.credit.PenaliteCreditDTO;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.EcheanceCredit;
import com.mini.credit.entity.credit.PenaliteCredit;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutPenalite;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.PenaliteCreditMapper;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.EcheanceCreditRepository;
import com.mini.credit.repository.credit.PenaliteCreditRepository;
import com.mini.credit.service.PenaliteCreditService;
import com.mini.credit.service.ParametreMetierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PHASE 10: Implémentation service pénalités de retard crédit
 *
 * Logique:
 * - Montant = PENALITE_RETARD_JOURNALIERE × nombre_jours_retard
 * - Batch job quotidien: 00:01
 * - Vérifie EcheanceCredit.dateEcheance < today
 * - Crée PenaliteCredit automatiquement
 */
@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class PenaliteCreditServiceImpl implements PenaliteCreditService {

    private final CreditRepository creditRepository;
    private final EcheanceCreditRepository echeanceCreditRepository;
    private final PenaliteCreditRepository penaliteCreditRepository;
    private final PenaliteCreditMapper penaliteCreditMapper;
    private final ParametreMetierService parametreMetierService;

    /**
     * PHASE 10: Batch job quotidien - 00:01 chaque jour
     * Crée pénalités pour TOUTES les échéances en retard
     */
    @Scheduled(cron = "0 1 0 * * *")
    @Override
    public List<PenaliteCreditDTO> genererPenalitesTous() {
        log.info("PHASE 10: Démarrage batch génération pénalités retard (quotidien)");

        // Récupère toutes les échéances avec dateEcheance < today
        LocalDate today = LocalDate.now();
        List<EcheanceCredit> echancesEnRetard = echeanceCreditRepository.findAll()
                .stream()
                .filter(e -> e.getDateEcheance() != null)
                .filter(e -> e.getDateEcheance().isBefore(today))
                .collect(Collectors.toList());

        log.info("Nombre d'échéances en retard: {}", echancesEnRetard.size());

        // Génère pénalités pour chaque échéance en retard
        List<PenaliteCreditDTO> penalites = echancesEnRetard.stream()
                .map(echeance -> {
                    try {
                        // Vérifie si pénalité n'existe pas déjà pour cette échéance
                        List<PenaliteCredit> existantes = penaliteCreditRepository
                                .findByCreditIdOrderByDateCreationPenaliteDesc(echeance.getCredit().getId());
                        
                        if (existantes.stream()
                                .noneMatch(p -> p.getDateEchéance().isEqual(echeance.getDateEcheance()))) {
                            return creerPenaliteEcheance(echeance);
                        }
                        return null;
                    } catch (Exception e) {
                        log.error("Erreur création pénalité échéance {}: {}", echeance.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(p -> p != null)
                .collect(Collectors.toList());

        log.info("Batch pénalités terminé: {} pénalités créées", penalites.size());
        return penalites;
    }

    /**
     * Crée une pénalité pour une échéance en retard
     *
     * @param echeanceCredit L'échéance en retard
     * @return PenaliteCreditDTO créée
     */
    private PenaliteCreditDTO creerPenaliteEcheance(EcheanceCredit echeanceCredit) {
        log.debug("Création pénalité pour échéance: {}", echeanceCredit.getId());

        Credit credit = echeanceCredit.getCredit();
        LocalDate today = LocalDate.now();
        LocalDate dateEcheance = echeanceCredit.getDateEcheance();

        if (!dateEcheance.isBefore(today)) {
            throw new BusinessException("Échéance non en retard: " + dateEcheance);
        }

        // Calcule jours de retard
        long joursRetard = java.time.temporal.ChronoUnit.DAYS
                .between(dateEcheance, today);

        // Calcule montant
        BigDecimal montantPenalite = calculerMontantPenalite(joursRetard);
        BigDecimal tauxApplique = getTauxPenaliteJournaliere();

        log.debug("Échéance {}: {} jours retard, montant={}",
                echeanceCredit.getId(), joursRetard, montantPenalite);

        // Crée PenaliteCredit
        PenaliteCredit penalite = PenaliteCredit.builder()
                .credit(credit)
                .demandeCredit(credit.getDemandeCredit())
                .dateEchéance(dateEcheance)
                .nombreJoursRetard(joursRetard)
                .montantPenalite(montantPenalite)
                .tauxApplique(tauxApplique)
                .statut(StatutPenalite.CREEE)
                .dateCreationPenalite(LocalDateTime.now())
                .observation("Pénalité créée automatiquement - Échéance " + dateEcheance + " (" + joursRetard + " jours retard)")
                .build();

        penaliteCreditRepository.save(penalite);
        log.info("Pénalité créée pour échéance {} (crédit {}): montant={}, jours_retard={}", 
                echeanceCredit.getId(), credit.getId(), montantPenalite, joursRetard);

        return penaliteCreditMapper.toDTO(penalite);
    }

    /**
     * Crée une pénalité pour un crédit en retard (legacy - utilise première échéance)
     *
     * @param creditId ID du crédit
     * @return PenaliteCreditDTO créée
     */
    @Override
    public PenaliteCreditDTO creerPenalite(Long creditId) {
        log.debug("Création pénalité pour crédit: {}", creditId);

        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new ResourceNotFoundException("Crédit non trouvé: " + creditId));

        // Récupère la première échéance en retard
        List<EcheanceCredit> echancesEnRetard = credit.getEcheances()
                .stream()
                .filter(e -> e.getDateEcheance() != null)
                .filter(e -> e.getDateEcheance().isBefore(LocalDate.now()))
                .collect(Collectors.toList());

        if (echancesEnRetard.isEmpty()) {
            throw new BusinessException("Crédit sans échéance en retard: " + creditId);
        }

        return creerPenaliteEcheance(echancesEnRetard.get(0));
    }

    /**
     * Récupère pénalités d'un crédit
     *
     * @param creditId ID du crédit
     * @return List pénalités
     */
    @Override
    public List<PenaliteCreditDTO> getByCreditId(Long creditId) {
        return penaliteCreditRepository.findByCreditIdOrderByDateCreationPenaliteDesc(creditId)
                .stream()
                .map(penaliteCreditMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère pénalités d'une demande de crédit
     *
     * @param demandeCreditId ID de la demande
     * @return List pénalités
     */
    @Override
    public List<PenaliteCreditDTO> getByDemandeCreditId(Long demandeCreditId) {
        return penaliteCreditRepository.findByDemandeCreditIdOrderByDateEchéanceAsc(demandeCreditId)
                .stream()
                .map(penaliteCreditMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère pénalités en attente (CREEES)
     *
     * @return List pénalités
     */
    @Override
    public List<PenaliteCreditDTO> getEnAttente() {
        return penaliteCreditRepository.findByStatutInOrderByDateEchéanceAsc(
                List.of(StatutPenalite.CREEE))
                .stream()
                .map(penaliteCreditMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Acquitte une pénalité (marque comme payée)
     *
     * @param penaliteId ID de la pénalité
     * @return PenaliteCreditDTO mise à jour
     */
    @Override
    public PenaliteCreditDTO acquitterPenalite(Long penaliteId) {
        log.debug("Acquittement pénalité: {}", penaliteId);

        PenaliteCredit penalite = penaliteCreditRepository.findById(penaliteId)
                .orElseThrow(() -> new ResourceNotFoundException("Pénalité non trouvée: " + penaliteId));

        if (!penalite.canBeAcquitted()) {
            throw new BusinessException("Pénalité non en attente de paiement: " + penaliteId);
        }

        // Met à jour
        penalite.setStatut(StatutPenalite.ACQUITTEE);
        penalite.setDateAcquittement(LocalDateTime.now());
        
        // Récupère utilisateur courant
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            log.debug("Acquittée par: {}", auth.getName());
        }

        penaliteCreditRepository.save(penalite);
        log.info("Pénalité {} acquittée", penaliteId);

        return penaliteCreditMapper.toDTO(penalite);
    }

    /**
     * Efface une pénalité (pardon/remise)
     *
     * @param penaliteId ID de la pénalité
     * @param motif Motif de l'effacement
     * @return PenaliteCreditDTO mise à jour
     */
    @Override
    public PenaliteCreditDTO effacerPenalite(Long penaliteId, String motif) {
        log.debug("Effacement pénalité: {}", penaliteId);

        PenaliteCredit penalite = penaliteCreditRepository.findById(penaliteId)
                .orElseThrow(() -> new ResourceNotFoundException("Pénalité non trouvée: " + penaliteId));

        if (!penalite.canBeErased()) {
            throw new BusinessException("Pénalité non supprimable: " + penaliteId);
        }

        // Met à jour
        penalite.setStatut(StatutPenalite.EFFACEE);
        penalite.setDateEffacement(LocalDateTime.now());
        penalite.setMotifEffacement(motif);

        penaliteCreditRepository.save(penalite);
        log.info("Pénalité {} effacée - motif: {}", penaliteId, motif);

        return penaliteCreditMapper.toDTO(penalite);
    }

    /**
     * Montant total pénalités crédit (tous statuts)
     *
     * @param creditId ID du crédit
     * @return Somme
     */
    @Override
    public BigDecimal getTotalPenalitesCredit(Long creditId) {
        return penaliteCreditRepository.sumMontantPenaliteByCredit(creditId);
    }

    /**
     * Montant total pénalités EN ATTENTE pour crédit (CREEES seulement)
     *
     * @param creditId ID du crédit
     * @return Somme
     */
    @Override
    public BigDecimal getTotalPenalitesEnAttenteCredit(Long creditId) {
        return penaliteCreditRepository.sumMontantPenaliteCreeeByCredit(creditId);
    }

    /**
     * Calcule montant pénalité
     *
     * Formule: montant = jours_retard × PENALITE_RETARD_JOURNALIERE
     * Exemple: 5 jours × 2500 FC/jour = 12500 FC
     *
     * @param joursRetard Nombre de jours de retard
     * @return Montant calculé
     */
    @Override
    public BigDecimal calculerMontantPenalite(Long joursRetard) {
        if (joursRetard == null || joursRetard <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal taux = getTauxPenaliteJournaliere();
        return taux.multiply(BigDecimal.valueOf(joursRetard));
    }

    /**
     * Récupère taux pénalité journalière (PHASE 1)
     * Défaut: 2500 FC/jour si non trouvé
     *
     * @return Taux
     */
    @Override
    public BigDecimal getTauxPenaliteJournaliere() {
        BigDecimal taux = parametreMetierService.getDecimal("PENALITE_RETARD_JOURNALIERE");
        
        if (taux == null || taux.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Taux pénalité invalide, utilisation défaut 2500");
            return BigDecimal.valueOf(2500);  // Défaut 2500 FC/jour
        }

        return taux;
    }
}
