package com.mini.credit.service.impl;

import com.mini.credit.dto.epargne.OperationEpargneDTO;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.epargne.OperationEpargne;
import com.mini.credit.enums.ModePaiement;
import com.mini.credit.enums.SensOperation;
import com.mini.credit.enums.TypeOperationEpargne;
import com.mini.credit.mapper.OperationEpargneMapper;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.epargne.OperationEpargneRepository;
import com.mini.credit.service.GenerationInteretService;
import com.mini.credit.service.ParametreMetierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PHASE 9: Implémentation génération automatique intérêts épargne
 *
 * Logique:
 * - Intérêt mensuel = soldeDisponible × TAUX_INTERET_EPARGNE / 12
 * - Crée OperationEpargne type INTERET
 * - Batch job: executes 1st of month at 00:00 (configurable)
 * - Full audit trail
 */
@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class GenerationInteretServiceImpl implements GenerationInteretService {

    private final CompteEpargneRepository compteEpargneRepository;
    private final OperationEpargneRepository operationEpargneRepository;
    private final OperationEpargneMapper operationEpargneMapper;
    private final ParametreMetierService parametreMetierService;

    /**
     * PHASE 9: Batch job exécuté automatiquement le 1er du mois à 00:00
     * Génère intérêts pour TOUS les comptes épargne actifs
     *
     * Cron: "0 0 1 * * *" = 1st day of month at 00:00
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Override
    public List<OperationEpargneDTO> genererInteretsTous() {
        log.info("PHASE 9: Démarrage batch génération intérêts épargne (mensuel)");

        // Récupère tous les comptes actifs
        List<CompteEpargne> comptesActifs = compteEpargneRepository.findAllByActifTrue();
        log.info("Nombre de comptes actifs: {}", comptesActifs.size());

        // Génère intérêts pour chaque compte
        List<OperationEpargneDTO> operations = comptesActifs.stream()
                .map(compte -> {
                    try {
                        return genererInteretCompte(compte.getId());
                    } catch (Exception e) {
                        log.error("Erreur génération intérêt compte {}: {}", compte.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(op -> op != null)
                .collect(Collectors.toList());

        log.info("Batch intérêts terminé: {} opérations créées", operations.size());
        return operations;
    }

    /**
     * Génère intérêt pour UN compte
     *
     * Étapes:
     * 1. Récupère le compte
     * 2. Calcule intérêt = solde × taux / 12
     * 3. Crée OperationEpargne type INTERET
     * 4. Incrémente soldeDisponible
     * 5. Sauvegarde
     *
     * @param compteEpargneId ID du compte
     * @return OperationEpargneDTO créée
     */
    @Override
    public OperationEpargneDTO genererInteretCompte(Long compteEpargneId) {
        log.debug("Génération intérêt pour compte: {}", compteEpargneId);

        // 1. Récupère le compte
        CompteEpargne compte = compteEpargneRepository.findById(compteEpargneId)
                .orElseThrow(() -> new RuntimeException("Compte épargne non trouvé: " + compteEpargneId));

        // 2. Calcule l'intérêt
        BigDecimal montantInteret = calculerMontantInteret(compte.getSoldeDisponible());
        log.debug("Compte {}: solde={}, intérêt calculé={}", 
                compteEpargneId, compte.getSoldeDisponible(), montantInteret);

        // 3. Crée l'OperationEpargne
        OperationEpargne operation = OperationEpargne.builder()
                .compteEpargne(compte)
                .membre(compte.getMembre())
                .typeOperation(TypeOperationEpargne.INTERET)
                .sens(SensOperation.ENTREE)
                .montant(montantInteret)
                .modePaiement(ModePaiement.SYSTEME)
                .dateOperation(LocalDateTime.now())
                .observation("Intérêt épargne automatique - Taux mensuel: " + 
                        getTauxInteretEpargne().divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP))
                .build();

        // 4. Incrémente solde
        compte.setSoldeDisponible(compte.getSoldeDisponible().add(montantInteret));

        // 5. Sauvegarde
        operationEpargneRepository.save(operation);
        compteEpargneRepository.save(compte);

        log.info("Intérêt généré pour compte {}: montant={}, nouveau solde={}",
                compteEpargneId, montantInteret, compte.getSoldeDisponible());

        return operationEpargneMapper.toDTO(operation);
    }

    /**
     * Génère intérêts pour tous les comptes d'un membre
     *
     * @param membreId ID du membre
     * @return List des opérations créées
     */
    @Override
    public List<OperationEpargneDTO> genererInteretsParMembre(Long membreId) {
        log.debug("Génération intérêts pour membre: {}", membreId);

        // Récupère comptes du membre
        List<CompteEpargne> comptes = compteEpargneRepository.findByMembreId(membreId);

        return comptes.stream()
                .map(compte -> genererInteretCompte(compte.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Calcule montant intérêt mensuel
     *
     * Formule: montantInteret = solde × TAUX_INTERET_EPARGNE / 12
     * Exemple: solde=100000, taux=5% → intérêt = 100000 × 0.05 / 12 = 416.67
     *
     * @param soldeDisponible Solde actuel du compte
     * @return Montant arrondi à 2 décimales
     */
    @Override
    public BigDecimal calculerMontantInteret(BigDecimal soldeDisponible) {
        if (soldeDisponible == null || soldeDisponible.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal taux = getTauxInteretEpargne();
        // Calcul: solde × taux / 12
        BigDecimal interet = soldeDisponible
                .multiply(taux)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        return interet;
    }

    /**
     * Récupère le taux intérêt épargne courant
     * Utilise TAUX_INTERET_EPARGNE (PHASE 1)
     * Défaut: 5% = 0.05
     *
     * @return Taux en BigDecimal (ex: 0.05)
     */
    @Override
    public BigDecimal getTauxInteretEpargne() {
        // Récupère paramètre TAUX_INTERET_EPARGNE (PHASE 1)
        BigDecimal taux = parametreMetierService.getDecimal("TAUX_INTERET_EPARGNE");
        
        if (taux == null || taux.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Taux intérêt épargne invalide, utilisation défaut 5%");
            return BigDecimal.valueOf(0.05);
        }

        return taux;
    }

    /**
     * Récupère total intérêts générés pour un membre
     * Somme de toutes les OperationEpargne type INTERET
     *
     * @param membreId ID du membre
     * @return Total intérêts
     */
    @Override
    public BigDecimal getTotalInteretsGeneres(Long membreId) {
        log.debug("Calcul total intérêts pour membre: {}", membreId);

        // Récupère comptes du membre
        List<CompteEpargne> comptes = compteEpargneRepository.findByMembreId(membreId);

        // Somme intérêts sur tous les comptes
        return comptes.stream()
                .flatMap(compte -> operationEpargneRepository
                        .findByCompteEpargneAndTypeOperation(compte, TypeOperationEpargne.INTERET)
                        .stream())
                .map(op -> op.getMontant())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
