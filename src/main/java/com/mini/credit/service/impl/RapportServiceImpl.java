package com.mini.credit.service.impl;

import com.mini.credit.dto.rapport.*;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.credit.ContratCreditRepository;
import com.mini.credit.repository.credit.EcheanceCreditRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.service.RapportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RapportServiceImpl implements RapportService {

    private final OperationCaisseRepository operationCaisseRepository;
    private final MembreRepository membreRepository;
    private final ContratCreditRepository contratCreditRepository;
    private final EcheanceCreditRepository echeanceCreditRepository;
    private final CompteEpargneRepository compteEpargneRepository;

    @Override
    public BilanJournalierDTO genererBilanJournalier(LocalDate date) {
        LocalDateTime debutJournee = date.atStartOfDay();
        LocalDateTime finJournee = date.atTime(LocalTime.MAX);

        // Calculer les totaux des opérations de caisse pour la journée
        BigDecimal totalEncaissements = operationCaisseRepository
                .findTotalByTypeAndDate("DEBIT", debutJournee, finJournee);

        BigDecimal totalDecaissements = operationCaisseRepository
                .findTotalByTypeAndDate("CREDIT", debutJournee, finJournee);

        BigDecimal soldeJournee = totalEncaissements.subtract(totalDecaissements);

        return BilanJournalierDTO.builder()
                .date(date)
                .totalEncaissements(totalEncaissements)
                .totalDecaissements(totalDecaissements)
                .soldeJournee(soldeJournee)
                .nombreOperations(operationCaisseRepository.countByDateBetween(debutJournee, finJournee))
                .build();
    }

    @Override
    public BilanHebdomadaireDTO genererBilanHebdomadaire(LocalDate dateDebut) {
        LocalDate dateFin = dateDebut.plusDays(6);
        LocalDateTime debutSemaine = dateDebut.atStartOfDay();
        LocalDateTime finSemaine = dateFin.atTime(LocalTime.MAX);

        // Calculer les totaux pour la semaine
        BigDecimal totalEncaissementsSemaine = operationCaisseRepository
                .findTotalByTypeAndDate("DEBIT", debutSemaine, finSemaine);

        BigDecimal totalDecaissementsSemaine = operationCaisseRepository
                .findTotalByTypeAndDate("CREDIT", debutSemaine, finSemaine);

        // Calculer l'évolution par jour (simplifié)
        List<BilanJournalierDTO> bilansJournaliers = dateDebut.datesUntil(dateFin.plusDays(1))
                .map(this::genererBilanJournalier)
                .toList();

        return BilanHebdomadaireDTO.builder()
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .totalEncaissementsSemaine(totalEncaissementsSemaine)
                .totalDecaissementsSemaine(totalDecaissementsSemaine)
                .soldeSemaine(totalEncaissementsSemaine.subtract(totalDecaissementsSemaine))
                .bilansJournaliers(bilansJournaliers)
                .build();
    }

    @Override
    public BilanMensuelDTO genererBilanMensuel(LocalDate mois) {
        LocalDate debutMois = mois.withDayOfMonth(1);
        LocalDate finMois = mois.withDayOfMonth(mois.lengthOfMonth());
        LocalDateTime debutPeriode = debutMois.atStartOfDay();
        LocalDateTime finPeriode = finMois.atTime(LocalTime.MAX);

        // Calculer les totaux pour le mois
        BigDecimal totalEncaissementsMois = operationCaisseRepository
                .findTotalByTypeAndDate("DEBIT", debutPeriode, finPeriode);

        BigDecimal totalDecaissementsMois = operationCaisseRepository
                .findTotalByTypeAndDate("CREDIT", debutPeriode, finPeriode);

        // Calculer les métriques supplémentaires (à implémenter selon vos besoins)
        BigDecimal portefeuillesCredits = BigDecimal.ZERO; // À calculer depuis les contrats
        BigDecimal tauxRecouvrement = BigDecimal.ZERO; // À calculer depuis les échéances

        return BilanMensuelDTO.builder()
                .mois(mois)
                .totalEncaissementsMois(totalEncaissementsMois)
                .totalDecaissementsMois(totalDecaissementsMois)
                .soldeMois(totalEncaissementsMois.subtract(totalDecaissementsMois))
                .portefeuillesCredits(portefeuillesCredits)
                .tauxRecouvrement(tauxRecouvrement)
                .build();
    }

    @Override
    public KPIDashboardDTO getKPIDashboard() {
        long totalClients = membreRepository.count();
        long totalCreditActifs = contratCreditRepository.count();
        BigDecimal totalPortefeuille = BigDecimal.ZERO; // À calculer
        BigDecimal tauxDefaut = BigDecimal.ZERO; // À calculer
        BigDecimal revenus30j = BigDecimal.ZERO; // À calculer
        BigDecimal depenses30j = BigDecimal.ZERO; // À calculer
        BigDecimal benefice30j = BigDecimal.ZERO; // À calculer
        BigDecimal epargne = BigDecimal.ZERO; // À calculer

        return KPIDashboardDTO.builder()
                .totalClients(totalClients)
                .totalCreditActifs(totalCreditActifs)
                .totalPortefeuilleActuel(totalPortefeuille)
                .tauxDefautPortefeuille(tauxDefaut)
                .revenus30Jours(revenus30j)
                .depenses30Jours(depenses30j)
                .benefice30Jours(benefice30j)
                .epargneCollectee(epargne)
                .build();
    }

    @Override
    public RapportFinancierDTO getRapportFinancier(LocalDate dateDebut, LocalDate dateFin) {
        LocalDateTime debutPeriode = dateDebut.atStartOfDay();
        LocalDateTime finPeriode = dateFin.atTime(LocalTime.MAX);

        BigDecimal totalEncaissements = operationCaisseRepository
                .findTotalByTypeAndDate("DEBIT", debutPeriode, finPeriode);

        BigDecimal totalDecaissements = operationCaisseRepository
                .findTotalByTypeAndDate("CREDIT", debutPeriode, finPeriode);

        BigDecimal solde = totalEncaissements.subtract(totalDecaissements);

        return RapportFinancierDTO.builder()
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .totalEncaissements(totalEncaissements != null ? totalEncaissements : BigDecimal.ZERO)
                .totalDecaissements(totalDecaissements != null ? totalDecaissements : BigDecimal.ZERO)
                .solde(solde != null ? solde : BigDecimal.ZERO)
                .interets(BigDecimal.ZERO)
                .penalites(BigDecimal.ZERO)
                .totalRevenues(BigDecimal.ZERO)
                .build();
    }

    @Override
    public RapportRisqueDTO getRapportRisque() {
        List<RapportRisqueDTO.RisqueAlerte> alertes = new ArrayList<>();

        return RapportRisqueDTO.builder()
                .creditsEnRisque(0)
                .creditsEnRetard(0)
                .creditsEnDefaut(0)
                .niveauRisqueGlobal("FAIBLE")
                .alertes(alertes)
                .build();
    }

    @Override
    public RapportCollecteDTO getRapportCollecte(LocalDate dateDebut, LocalDate dateFin) {
        LocalDateTime debutPeriode = dateDebut.atStartOfDay();
        LocalDateTime finPeriode = dateFin.atTime(LocalTime.MAX);

        BigDecimal totalCollecte = operationCaisseRepository
                .findTotalByTypeAndDate("DEBIT", debutPeriode, finPeriode);

        long nombreMembres = membreRepository.count();
        long nombreOperations = operationCaisseRepository.countByDateBetween(debutPeriode, finPeriode);

        BigDecimal moyenneParMembre = nombreMembres > 0
                ? totalCollecte != null ? totalCollecte.divide(BigDecimal.valueOf(nombreMembres)) : BigDecimal.ZERO
                : BigDecimal.ZERO;

        return RapportCollecteDTO.builder()
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .totalCollecte(totalCollecte != null ? totalCollecte : BigDecimal.ZERO)
                .totalEpargne(BigDecimal.ZERO)
                .nombreMembres(nombreMembres)
                .nombreOperations(nombreOperations)
                .moyenneParMembre(moyenneParMembre)
                .tendance("STABLE")
                .build();
    }
}