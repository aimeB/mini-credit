package com.mini.credit.service.impl;

import com.mini.credit.dto.credit.CreditValidationResult;
import com.mini.credit.entity.credit.AnalyseRisque;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.credit.Garantie;
import com.mini.credit.entity.credit.GarantieCredit;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.repository.credit.AnalyseRisqueRepository;
import com.mini.credit.repository.credit.GarantieCreditRepository;
import com.mini.credit.repository.credit.GarantieRepository;
import com.mini.credit.service.CreditValidationService;
import com.mini.credit.service.ParametreMetierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implémentation du service de validation stricte des crédits (PHASE 4).
 *
 * Règles de validation:
 * 1. Frais demande intégralement payés si un montant est attendu
 * 2. Garantie >= 20% montant demandé
 * 3. Analyse terrain complète (adresse + emploi vérifiés)
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class CreditValidationServiceImpl implements CreditValidationService {

    private static final BigDecimal TAUX_GARANTIE_3N = BigDecimal.valueOf(20);

    private final AnalyseRisqueRepository analyseRisqueRepository;
    private final GarantieCreditRepository garantieCreditRepository;
    private final GarantieRepository garantieRepository;
    private final ParametreMetierService parametreMetierService;

    public CreditValidationServiceImpl(
            AnalyseRisqueRepository analyseRisqueRepository,
            GarantieCreditRepository garantieCreditRepository,
            GarantieRepository garantieRepository,
            ParametreMetierService parametreMetierService) {
        this.analyseRisqueRepository = analyseRisqueRepository;
        this.garantieCreditRepository = garantieCreditRepository;
        this.garantieRepository = garantieRepository;
        this.parametreMetierService = parametreMetierService;
    }

    /**
     * CRITÈRE 1: Frais demande intégralement payés si un montant est attendu
     */
    @Override
    public boolean validerFraisDemandePayes(DemandeCredit demandeCredit) {
        if (demandeCredit == null) {
            return false;
        }
        BigDecimal fraisAttendus = safeAmount(demandeCredit.getFraisDemande());
        BigDecimal fraisPayes = safeAmount(demandeCredit.getFraisDemandePayes());
        boolean isValid = fraisAttendus.compareTo(BigDecimal.ZERO) <= 0
                || fraisPayes.compareTo(fraisAttendus) >= 0;
        
        log.debug("Validation frais demande: demandeId={}, fraisAttendus={}, fraisPayes={}, isValid={}",
                demandeCredit.getId(), fraisAttendus, fraisPayes, isValid);
        
        return isValid;
    }

    @Override
    public void verifierFraisDemandeIntegralementPayes(DemandeCredit demandeCredit) {
        if (!validerFraisDemandePayes(demandeCredit)) {
            throw new BusinessException("Les frais de demande doivent être entièrement payés avant cette étape.");
        }
    }

    /**
     * CRITÈRE 2: Garantie >= 20% montant demandé
     */
    @Override
    public boolean validerGarantieDeposee(DemandeCredit demandeCredit) {
        if (demandeCredit == null || demandeCredit.getMontantDemande() == null) {
            return false;
        }

        // Récupère la garantie requise (20% du montant demandé)
        BigDecimal garantieRequise = calculerGarantieRequise(demandeCredit.getMontantDemande());
        if (garantieRequise == null) {
            return false;
        }

        // Cherche en priorité la nouvelle garantie de workflow, sinon fallback legacy
        BigDecimal totalGarantieBloquee = garantieCreditRepository.findByDemandeCreditId(demandeCredit.getId())
            .map(GarantieCredit::getMontantGarantieBloque)
            .orElseGet(() -> {
                List<Garantie> garanties = garantieRepository.findByDemandeCredit(demandeCredit);
                return garanties.stream()
                    .map(Garantie::getMontantBloque)
                    .filter(m -> m != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            });

        boolean isValid = totalGarantieBloquee.compareTo(garantieRequise) >= 0;
        
        log.debug("Validation garantie: demandeId={}, montantDemande={}, garantieRequise={}, " +
                "totalGarantieBloquee={}, isValid={}",
                demandeCredit.getId(), demandeCredit.getMontantDemande(), 
                garantieRequise, totalGarantieBloquee, isValid);
        
        return isValid;
    }

    /**
     * CRITÈRE 3: Analyse terrain validée (adresse + emploi)
     */
    @Override
    public boolean validerAnalyseTerrainComplete(DemandeCredit demandeCredit) {
        if (demandeCredit == null) {
            return false;
        }

        // Cherche l'analyse risque associée
        AnalyseRisque analyse = analyseRisqueRepository.findByDemandeCredit(demandeCredit);
        
        if (analyse == null) {
            log.debug("Pas d'analyse risque trouvée pour demandeId={}", demandeCredit.getId());
            return false;
        }

        boolean isValid = analyse.isAnalyseTerrainCompleteAndValidated();
        
        log.debug("Validation analyse terrain: demandeId={}, analyseTerrainComplete={}, " +
                "adresseValidee={}, emploiVerifie={}, isValid={}",
                demandeCredit.getId(), analyse.getAnalyseTerrainComplete(),
                analyse.getAdresseValidee(), analyse.getEmploiVerifie(), isValid);
        
        return isValid;
    }

    /**
     * VALIDATION COMPLÈTE: Tous les critères ensemble
     */
    @Override
    public CreditValidationResult validerCreditComplet(DemandeCredit demandeCredit) {
        log.info("Validation complète crédit: demandeId={}, numeroDemande={}",
                demandeCredit.getId(), demandeCredit.getNumeroDemande());

        CreditValidationResult result = CreditValidationResult.builder()
                .build();

        // Critère 1: Frais demande
        boolean fraisOk = validerFraisDemandePayes(demandeCredit);
        result.setFraisDemandePayesOk(fraisOk);
        result.setFraisDemandeMontant(demandeCredit.getFraisDemandePayes());
        if (!fraisOk) {
            result.setFraisDemandeMessage("Les frais de demande doivent être entièrement payés avant cette étape.");
        } else {
            result.setFraisDemandeMessage("Frais de demande payés ✓");
        }

        // Critère 2: Garantie
        boolean garantieOk = validerGarantieDeposee(demandeCredit);
        result.setGarantieOk(garantieOk);
        
        BigDecimal montantDemande = demandeCredit.getMontantDemande();
        BigDecimal garantieRequise = calculerGarantieRequise(montantDemande);
        result.setGarantieMontantRequis(garantieRequise);
        
        BigDecimal totalGarantieBloquee = garantieCreditRepository.findByDemandeCreditId(demandeCredit.getId())
            .map(GarantieCredit::getMontantGarantieBloque)
            .orElseGet(() -> {
                List<Garantie> garanties = garantieRepository.findByDemandeCredit(demandeCredit);
                return garanties.stream()
                    .map(Garantie::getMontantBloque)
                    .filter(m -> m != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            });
        result.setGarantieMontantBloque(totalGarantieBloquee);
        
        // Calcule le pourcentage
        if (montantDemande != null && montantDemande.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pourcentage = totalGarantieBloquee.divide(montantDemande, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            result.setGarantiePourcentageActuel(pourcentage);
        }
        
        if (!garantieOk) {
            result.setGarantieMessage("Garantie insuffisante: " + totalGarantieBloquee + 
                    " < " + garantieRequise);
        } else {
            result.setGarantieMessage("Garantie suffisante ✓");
        }

        // Critère 3: Analyse terrain
        boolean analyseOk = validerAnalyseTerrainComplete(demandeCredit);
        result.setAnalyseTerrainOk(analyseOk);
        
        AnalyseRisque analyse = analyseRisqueRepository.findByDemandeCredit(demandeCredit);
        if (analyse != null) {
            result.setAnalyseTerrainComplete(analyse.getAnalyseTerrainComplete());
            result.setAdresseValidee(analyse.getAdresseValidee());
            result.setEmploiVerifie(analyse.getEmploiVerifie());
        }
        
        if (!analyseOk) {
            StringBuilder sb = new StringBuilder("Analyse terrain incomplète:");
            if (analyse == null) {
                sb.append(" AUCUNE analyse trouvée");
            } else {
                if (!Boolean.TRUE.equals(analyse.getAnalyseTerrainComplete())) {
                    sb.append(" terrain non complet |");
                }
                if (!Boolean.TRUE.equals(analyse.getAdresseValidee())) {
                    sb.append(" adresse non validée |");
                }
                if (!Boolean.TRUE.equals(analyse.getEmploiVerifie())) {
                    sb.append(" emploi non vérifié |");
                }
            }
            result.setAnalyseTerrainMessage(sb.toString());
        } else {
            result.setAnalyseTerrainMessage("Analyse terrain validée ✓");
        }

        // Résumé global
        int score = (fraisOk ? 1 : 0) + (garantieOk ? 1 : 0) + (analyseOk ? 1 : 0);
        result.setValidationScore(score);
        result.setValid(fraisOk && garantieOk && analyseOk);

        if (result.isValid()) {
            result.setMessage("✓ Crédit validé: tous les critères PHASE 4 respectés");
            result.setMotifRejet(null);
        } else {
            StringBuilder motif = new StringBuilder();
            if (!fraisOk) motif.append("Frais de demande incomplets | ");
            if (!garantieOk) motif.append("Garantie insuffisante | ");
            if (!analyseOk) motif.append("Analyse terrain incomplète | ");
            
            result.setMessage("✗ Crédit NON validé: " + score + "/3 critères passés");
            result.setMotifRejet(motif.toString());
        }

        log.info("Résultat validation: demandeId={}, isValid={}, score={}/3",
                demandeCredit.getId(), result.isValid(), score);

        return result;
    }

    /**
     * Calcule 20% du montant demandé (garantie requise)
     */
    @Override
    public BigDecimal calculerGarantieRequise(BigDecimal montantDemande) {
        if (montantDemande == null || montantDemande.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return montantDemande.multiply(TAUX_GARANTIE_3N)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
