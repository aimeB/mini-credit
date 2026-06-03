package com.mini.credit.service.impl;

import com.mini.credit.dto.dashboard.*;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.enums.StatutCredit;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.EcheanceCreditRepository;
import com.mini.credit.repository.projection.CategorieMontantProjection;
import com.mini.credit.service.DashboardService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardServiceImpl implements DashboardService {

    private final CreditRepository creditRepository;
    private final EcheanceCreditRepository echeanceCreditRepository;
    private final OperationCaisseRepository operationCaisseRepository;
    private final SessionCaisseRepository sessionCaisseRepository;

    @Override
    public PortefeuilleDashboardResponse getPortefeuilleDashboard() {
        long totalCredits = creditRepository.count();

        long creditsActifs = creditRepository.countByStatutIn(List.of(
                StatutCredit.APPROUVE,
                StatutCredit.DECAISSE,
                StatutCredit.EN_COURS,
                StatutCredit.EN_RETARD
        ));

        long creditsRembourses = creditRepository.countByStatut(StatutCredit.REMBOURSE);
        long creditsEnRetard = creditRepository.countByStatut(StatutCredit.EN_RETARD);

        return PortefeuilleDashboardResponse.builder()
                .totalCredits(totalCredits)
                .creditsActifs(creditsActifs)
                .creditsRembourses(creditsRembourses)
                .creditsEnRetard(creditsEnRetard)
                .montantTotalOctroye(safe(creditRepository.sumMontantOctroye()))
                .encoursPrincipal(safe(creditRepository.sumEncoursPrincipal()))
                .interetsTotaux(safe(creditRepository.sumInteretTotal()))
                .penalitesTotales(safe(creditRepository.sumPenaliteTotale()))
                .build();
    }

    @Override
    public RetardDashboardResponse getRetardDashboard(LocalDate dateReference) {
        long nombreCreditsEnRetard = echeanceCreditRepository.countCreditsEnRetard(dateReference);
        long nombreEcheancesEnRetard = echeanceCreditRepository.countEcheancesEnRetard(dateReference);
        BigDecimal montantTotalEnRetard = safe(echeanceCreditRepository.sumMontantEnRetard(dateReference));
        BigDecimal penalitesCumulees = safe(echeanceCreditRepository.sumPenalitesEnRetard(dateReference));

        List<RetardEcheanceItemResponse> echeances = echeanceCreditRepository.findEcheancesEnRetard(dateReference)
                .stream()
                .map(p -> RetardEcheanceItemResponse.builder()
                        .creditId(p.getCreditId())
                        .numeroCredit(p.getNumeroCredit())
                        .membreId(p.getMembreId())
                        .membreNomComplet(p.getMembreNomComplet())
                        .echeanceId(p.getEcheanceId())
                        .numeroEcheance(p.getNumeroEcheance())
                        .dateEcheance(p.getDateEcheance())
                        .resteAPayer(safe(p.getResteAPayer()))
                        .penaliteCumulee(safe(p.getPenaliteCumulee()))
                        .joursRetard(ChronoUnit.DAYS.between(p.getDateEcheance(), dateReference))
                        .build())
                .toList();

        return RetardDashboardResponse.builder()
                .nombreCreditsEnRetard(nombreCreditsEnRetard)
                .nombreEcheancesEnRetard(nombreEcheancesEnRetard)
                .montantTotalEnRetard(montantTotalEnRetard)
                .penalitesCumulees(penalitesCumulees)
                .echeances(echeances)
                .build();
    }

    @Override
    public CaisseDashboardResponse getCaisseDashboard(Long sessionCaisseId) {
        SessionCaisse session = sessionCaisseRepository.findById(sessionCaisseId)
                .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable"));

        BigDecimal totalEntrees = safe(operationCaisseRepository.sumBySessionAndType(
                sessionCaisseId, TypeOperationCaisse.ENTREE));

        BigDecimal totalSorties = safe(operationCaisseRepository.sumBySessionAndType(
                sessionCaisseId, TypeOperationCaisse.SORTIE));

        BigDecimal soldeTheorique = safe(session.getSoldeOuverture())
                .add(totalEntrees)
                .subtract(totalSorties);

        List<CaisseCategorieItemResponse> repartitionEntrees =
                mapCategories(operationCaisseRepository.sumBySessionAndTypeGroupByCategorie(
                        sessionCaisseId, TypeOperationCaisse.ENTREE));

        List<CaisseCategorieItemResponse> repartitionSorties =
                mapCategories(operationCaisseRepository.sumBySessionAndTypeGroupByCategorie(
                        sessionCaisseId, TypeOperationCaisse.SORTIE));

        return CaisseDashboardResponse.builder()
                .sessionCaisseId(session.getId())
                .caisseId(session.getCaisse().getId())
                .caisseCode(session.getCaisse().getCodeCaisse())
                .soldeOuverture(safe(session.getSoldeOuverture()))
                .totalEntrees(totalEntrees)
                .totalSorties(totalSorties)
                .soldeTheorique(soldeTheorique)
                .soldePhysique(session.getSoldePhysique())
                .ecartCaisse(session.getSoldePhysique() != null
                        ? session.getSoldePhysique().subtract(soldeTheorique)
                        : null)
                .repartitionEntrees(repartitionEntrees)
                .repartitionSorties(repartitionSorties)
                .build();
    }

    @Override
    public DashboardGlobalResponse getDashboardGlobal(Long sessionCaisseId, LocalDate dateReference) {
        return DashboardGlobalResponse.builder()
                .portefeuille(getPortefeuilleDashboard())
                .retard(getRetardDashboard(dateReference))
                .caisse(getCaisseDashboard(sessionCaisseId))
                .build();
    }

    private List<CaisseCategorieItemResponse> mapCategories(List<CategorieMontantProjection> rows) {
        return rows.stream()
                .map(r -> CaisseCategorieItemResponse.builder()
                        .categorie(r.getCategorie())
                        .montant(safe(r.getMontant()))
                        .build())
                .toList();
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
