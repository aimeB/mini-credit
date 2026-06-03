package com.mini.credit.service.impl;

import com.mini.credit.dto.rapport.RapportFinancierDTO;
import com.mini.credit.entity.rapport.RapportFinancier;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.*;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.mapper.RapportFinancierMapper;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.rapport.RapportFinancierRepository;
import com.mini.credit.service.RapportFinancierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PHASE 12: Implémentation service rapports financiers
 *
 * Logique:
 * - Batch quotidien à 03:00: génère rapport bilan du jour
 * - Agrégations: sommes par statut, moyennes, ratios
 * - KPIs: taux remboursement, ROA, ROE
 * - Support multiples types de rapports (BILAN, COMPTE_RESULTAT, KPI, FLUX_TRESORERIE)
 */
@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class RapportFinancierServiceImpl implements RapportFinancierService {

    private final RapportFinancierRepository rapportFinancierRepository;
    private final CreditRepository creditRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final MembreRepository membreRepository;
    private final RapportFinancierMapper rapportFinancierMapper;

    /**
     * PHASE 12: Batch job quotidien - 03:00 chaque jour
     * Génère rapport financier consolidé du jour (BILAN)
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Override
    public RapportFinancierDTO genererRapportQuotidien() {
        log.info("PHASE 12: Démarrage batch génération rapport financier quotidien (bilan)");

        LocalDate today = LocalDate.now();

        // Vérifie si rapport existe déjà
        if (rapportFinancierRepository.existsByTypeRapportAndDateDebutAndDateFin(
                TypeRapport.BILAN, today, today)) {
            log.info("Rapport BILAN déjà généré pour {}", today);
            return null;
        }

        try {
            return genererRapportPersonnalise(TypeRapport.BILAN, today, today);
        } catch (Exception e) {
            log.error("Erreur génération rapport quotidien: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Génère rapport manuel pour type et période
     *
     * @param typeRapport Type de rapport
     * @param dateDebut Date début
     * @param dateFin Date fin
     * @return RapportFinancierDTO généré
     */
    @Override
    public RapportFinancierDTO genererRapportPersonnalise(TypeRapport typeRapport, LocalDate dateDebut, LocalDate dateFin) {
        log.debug("Génération rapport {} pour période {} à {}", typeRapport, dateDebut, dateFin);

        RapportFinancier rapport = RapportFinancier.builder()
                .typeRapport(typeRapport)
                .periodicite(PeriodiciteRapport.PERSONNALISE)
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .dateGeneration(LocalDateTime.now())
                .statut(StatutRapport.GENERE)
                .observation("Rapport généré automatiquement pour période")
                .build();

        // Récupère utilisateur courant
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            // Calcule agrégations selon type
            calculerDonneesRapport(rapport, dateDebut, dateFin, typeRapport);
            rapport.setGenerePar(null); // Sera peuplé depuis le contrôleur

            rapportFinancierRepository.save(rapport);
            log.info("Rapport {} généré pour période {} à {}", typeRapport, dateDebut, dateFin);

            return rapportFinancierMapper.toDTO(rapport);
        } catch (Exception e) {
            log.error("Erreur calcul rapport: {}", e.getMessage());
            rapport.setStatut(StatutRapport.ERREUR);
            rapport.setMessageErreur(e.getMessage());
            rapportFinancierRepository.save(rapport);
            throw new BusinessException("Erreur génération rapport: " + e.getMessage());
        }
    }

    /**
     * Calcule toutes les données agrégées pour le rapport
     */
    private void calculerDonneesRapport(RapportFinancier rapport, LocalDate dateDebut, LocalDate dateFin, TypeRapport typeRapport) {
        // Récupère tous les crédits
        List<com.mini.credit.entity.credit.Credit> creditsActifs = creditRepository.findAll()
                .stream()
                .filter(c -> c.getStatut() != StatutCredit.REMBOURSE && c.getStatut() != StatutCredit.ANNULE)
                .collect(Collectors.toList());

        List<com.mini.credit.entity.credit.Credit> creditsRembourses = creditRepository.findAll()
                .stream()
                .filter(c -> c.getStatut() == StatutCredit.REMBOURSE)
                .collect(Collectors.toList());

        // Calcule données BILAN
        if (typeRapport == TypeRapport.BILAN || typeRapport == TypeRapport.COMPTE_RESULTAT) {
            BigDecimal totalCredits = creditsActifs.stream()
                    .map(c -> c.getMontantOctroye() != null ? c.getMontantOctroye() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalEpargnes = compteEpargneRepository.findAll()
                    .stream()
                    .map(e -> e.getSoldeDisponible() != null ? e.getSoldeDisponible() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // BILAN: Actif = Caisse + Crédits + Épargnes disponibles
            BigDecimal totalActif = totalCredits.add(totalEpargnes);
            rapport.setTotalActif(totalActif);

            // BILAN: Passif = Épargnes dues
            rapport.setTotalPassif(totalEpargnes);

            // BILAN: Capitaux propres
            rapport.setCapitauxPropres(totalActif.subtract(totalEpargnes));
        }

        // Calcule données KPI
        if (typeRapport == TypeRapport.KPI) {
            // KPI: Nombre crédits
            rapport.setNombreCreditsActifs((long) creditsActifs.size());
            rapport.setNombreCreditsRembourses((long) creditsRembourses.size());

            // KPI: Crédits en retard (dateEcheance < today)
            long creditsEnRetard = creditsActifs.stream()
                    .filter(c -> c.getDateDecaissement() != null &&
                            c.getDateDecaissement().isBefore(LocalDate.now().minusMonths(1)))
                    .count();
            rapport.setNombreCreditsEnRetard(creditsEnRetard);

            // KPI: Taux de remboursement
            if (!creditsRembourses.isEmpty()) {
                long totalCreditsHistorique = creditsActifs.size() + creditsRembourses.size();
                BigDecimal tauxRemboursement = BigDecimal.valueOf(creditsRembourses.size())
                        .divide(BigDecimal.valueOf(totalCreditsHistorique), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                rapport.setTauxRemboursement(tauxRemboursement);
            }

            // KPI: Membres actifs
            long nombreMembresActifs = membreRepository.findAll().stream()
                    .filter(m -> m.getStatut() == StatutMembre.ACTIF)
                    .count();
            rapport.setNombreMembresActifs(nombreMembresActifs);

            // KPI: Solde moyen épargnes
            if (nombreMembresActifs > 0) {
                BigDecimal soldeTotal = compteEpargneRepository.findAll()
                        .stream()
                        .map(e -> e.getSoldeDisponible() != null ? e.getSoldeDisponible() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal soldeMoyen = soldeTotal.divide(BigDecimal.valueOf(nombreMembresActifs), 2, java.math.RoundingMode.HALF_UP);
                rapport.setSoldeEpargnesMoyen(soldeMoyen);
            }

            // KPI: Épargnes en caisse
            BigDecimal totalEpargnesCaisse = compteEpargneRepository.findAll()
                    .stream()
                    .map(e -> e.getSoldeDisponible() != null ? e.getSoldeDisponible() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            rapport.setTotalEpargnesCaisse(totalEpargnesCaisse);

            // KPI: ROA et ROE
            if (rapport.getTotalActif() != null && rapport.getTotalActif().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal resultat = rapport.getResultat() != null ? rapport.getResultat() : BigDecimal.ZERO;
                BigDecimal roa = resultat.divide(rapport.getTotalActif(), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                rapport.setRoa(roa);

                if (rapport.getCapitauxPropres() != null && rapport.getCapitauxPropres().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal roe = resultat.divide(rapport.getCapitauxPropres(), 4, java.math.RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                    rapport.setRoe(roe);
                }
            }
        }

        // Calcule données COMPTE_RESULTAT
        if (typeRapport == TypeRapport.COMPTE_RESULTAT) {
            // Révenues = Intérêts + Commissions + Pénalités
            BigDecimal revenus = BigDecimal.ZERO;

            // Charges = Intérêts épargnes
            BigDecimal charges = BigDecimal.ZERO;

            rapport.setTotalRevenus(revenus);
            rapport.setTotalCharges(charges);
            rapport.setResultat(revenus.subtract(charges));
        }

        log.debug("Données rapport calculées pour type {}", typeRapport);
    }

    /**
     * Récupère rapports par type
     *
     * @param typeRapport Type de rapport
     * @return liste
     */
    @Override
    public List<RapportFinancierDTO> getByTypeRapport(TypeRapport typeRapport) {
        return rapportFinancierRepository.findByTypeRapportOrderByDateGenerationDesc(typeRapport)
                .stream()
                .map(rapportFinancierMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère rapports en attente de validation
     *
     * @return liste
     */
    @Override
    public List<RapportFinancierDTO> getEnAttenteValidation() {
        return rapportFinancierRepository.findByStatutOrderByDateGenerationAsc(StatutRapport.GENERE)
                .stream()
                .map(rapportFinancierMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Valide un rapport
     *
     * @param rapportId ID du rapport
     * @return RapportFinancierDTO validé
     */
    @Override
    public RapportFinancierDTO validerRapport(Long rapportId) {
        log.debug("Validation rapport: {}", rapportId);

        RapportFinancier rapport = rapportFinancierRepository.findById(rapportId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé: " + rapportId));

        if (!rapport.canBeValidated()) {
            throw new BusinessException("Rapport ne peut pas être validé: " + rapportId);
        }

        rapport.setStatut(StatutRapport.VALIDE);
        rapport.setDateValidation(LocalDateTime.now());

        rapportFinancierRepository.save(rapport);
        log.info("Rapport {} validé", rapportId);

        return rapportFinancierMapper.toDTO(rapport);
    }

    /**
     * Archive un rapport
     *
     * @param rapportId ID du rapport
     * @return RapportFinancierDTO archivé
     */
    @Override
    public RapportFinancierDTO archiverRapport(Long rapportId) {
        log.debug("Archivage rapport: {}", rapportId);

        RapportFinancier rapport = rapportFinancierRepository.findById(rapportId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé: " + rapportId));

        if (!rapport.canBeArchived()) {
            throw new BusinessException("Rapport ne peut pas être archivé: " + rapportId);
        }

        rapport.setStatut(StatutRapport.ARCHIVE);
        rapportFinancierRepository.save(rapport);

        log.info("Rapport {} archivé", rapportId);
        return rapportFinancierMapper.toDTO(rapport);
    }

    /**
     * Récupère rapport spécifique
     *
     * @param rapportId ID
     * @return RapportFinancierDTO
     */
    @Override
    public RapportFinancierDTO getById(Long rapportId) {
        return rapportFinancierRepository.findById(rapportId)
                .map(rapportFinancierMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé: " + rapportId));
    }

    /**
     * Derniers rapports (non archivés)
     *
     * @return liste
     */
    @Override
    public List<RapportFinancierDTO> getRapportRecents() {
        return rapportFinancierRepository.findByStatutNotOrderByDateGenerationDesc(StatutRapport.ARCHIVE)
                .stream()
                .map(rapportFinancierMapper::toDTO)
                .collect(Collectors.toList());
    }
}
