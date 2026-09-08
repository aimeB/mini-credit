package com.mini.credit.service.impl;

import com.mini.credit.dto.rapport.revenus.CarnetMargeDto;
import com.mini.credit.dto.rapport.revenus.CapaciteRetraitProprietaireDto;
import com.mini.credit.dto.rapport.revenus.ChargeDetailDto;
import com.mini.credit.dto.rapport.revenus.ChargeParCategorieDto;
import com.mini.credit.dto.rapport.revenus.ComparaisonAgenceDto;
import com.mini.credit.dto.rapport.revenus.ControleCoherenceDto;
import com.mini.credit.dto.rapport.revenus.ApportFinancementDetailDto;
import com.mini.credit.dto.rapport.revenus.ApportFinancementDto;
import com.mini.credit.dto.rapport.revenus.FondsMembresProtegesDto;
import com.mini.credit.dto.rapport.revenus.DetailMasseSalarialeDto;
import com.mini.credit.dto.rapport.revenus.MasseSalarialeDto;
import com.mini.credit.dto.rapport.revenus.MouvementNonRevenuParAntenneDto;
import com.mini.credit.dto.rapport.revenus.PositionCreditDto;
import com.mini.credit.dto.rapport.revenus.RapportRevenusResponse;
import com.mini.credit.dto.rapport.revenus.RevenuDetailDto;
import com.mini.credit.dto.rapport.revenus.RevenuKpiDto;
import com.mini.credit.dto.rapport.revenus.RevenuParAntenneDto;
import com.mini.credit.dto.rapport.revenus.RevenuParCategorieDto;
import com.mini.credit.dto.rapport.revenus.SyntheseComparaisonAgencesDto;
import com.mini.credit.dto.rapport.revenus.TresorerieDisponibleDto;
import com.mini.credit.dto.rapport.revenus.TransportFixeDetailDto;
import com.mini.credit.dto.rapport.revenus.TransportFixePrevuDto;
import com.mini.credit.dto.rapport.revenus.TransportFixeSiteDetailDto;
import com.mini.credit.constants.PaiePersonnelConstants;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.caisse.DepenseCaisse;
import com.mini.credit.entity.caisse.OperationCaisse;
import com.mini.credit.entity.caisse.RecetteJournaliereTerrain;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.credit.DemandeCredit;
import com.mini.credit.entity.credit.RemboursementCredit;
import com.mini.credit.entity.employe.Employe;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.entity.referentiel.CollecteJournaliereTerrain;
import com.mini.credit.entity.referentiel.CollecteMembreLigne;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.TransportSiteParametre;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.DepenseCaisseCategorie;
import com.mini.credit.enums.DepenseCaisseStatus;
import com.mini.credit.enums.NatureFinancementApprovisionnement;
import com.mini.credit.enums.PosteEmploye;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.StatutCredit;
import com.mini.credit.enums.StatutDemandeCredit;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeLigneCollecte;
import com.mini.credit.enums.TypePaiementPersonnel;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.AgenceRepository;
import com.mini.credit.repository.caisse.DepenseCaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.RecetteJournaliereTerrainRepository;
import com.mini.credit.repository.caisse.RemboursementApportProprietaireRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.collecteTerrain.CollecteJournaliereTerrainRepository;
import com.mini.credit.repository.collecteTerrain.CollecteMembreLigneRepository;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.credit.DemandeCreditRepository;
import com.mini.credit.repository.credit.RemboursementCreditRepository;
import com.mini.credit.repository.EmployeRepository;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.epargne.DemandeRetraitEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.referentiel.TransportSiteParametreRepository;
import com.mini.credit.service.ParametreMetierService;
import com.mini.credit.service.RapportRevenusService;
import com.mini.credit.service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RapportRevenusServiceImpl implements RapportRevenusService {

    private static final String CAT_FRAIS_ANALYSE_CREDIT = "FRAIS_ANALYSE_CREDIT";
    private static final String CAT_FRAIS_RETRAIT_EPARGNE = "FRAIS_RETRAIT_EPARGNE";
    private static final String CAT_INTERETS_CREDIT = "INTERETS_CREDIT";
    private static final String CAT_PENALITES_CREDIT = "PENALITES_CREDIT";
    private static final String CAT_CARNETS_VENDUS = "CARNETS_VENDUS";
    private static final String CAT_REVENUS_DIVERS = "REVENUS_DIVERS";

    private static final String NATURE_VENTE_CARNET = "Vente carnet";
    private static final String NATURE_FRAIS_ANALYSE_CREDIT = "Frais analyse crédit";
    private static final String NATURE_FRAIS_ANALYSE_TERRAIN = "Frais analyse terrain";
    private static final String NATURE_FRAIS_RETRAIT_EPARGNE = "Frais retrait épargne";
    private static final String NATURE_INTERET_CREDIT = "Intérêt crédit";
    private static final String NATURE_PENALITE_CREDIT = "Pénalité crédit";
    private static final String NATURE_FRAIS_DIVERS = "Frais divers";
    private static final String NATURE_EXCEDENT_CAISSE = "Excédent caisse";
    private static final String NATURE_AUTRE_REVENU = "Autre revenu";
    private static final String NATURE_NON_CATEGORISE = "Revenu non catégorisé";

    private static final String SOURCE_CAISSE = "CAISSE";
    private static final String SOURCE_CREDIT = "CREDIT";
    private static final String SOURCE_COLLECTE = "COLLECTE";

    private final OperationCaisseRepository operationCaisseRepository;
    private final RecetteJournaliereTerrainRepository recetteJournaliereTerrainRepository;
    private final DepenseCaisseRepository depenseCaisseRepository;
    private final RemboursementCreditRepository remboursementCreditRepository;
    private final CollecteMembreLigneRepository collecteMembreLigneRepository;
    private final CollecteJournaliereTerrainRepository collecteJournaliereTerrainRepository;
    private final CreditRepository creditRepository;
    private final DemandeCreditRepository demandeCreditRepository;
    private final RemboursementApportProprietaireRepository remboursementApportRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final DemandeRetraitEpargneRepository demandeRetraitEpargneRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final EmployeRepository employeRepository;
    private final AgentTerrainRepository agentTerrainRepository;
    private final TransportSiteParametreRepository transportSiteParametreRepository;
    private final AgenceRepository agenceRepository;
    private final MembreRepository membreRepository;
    private final ParametreMetierService parametreMetierService;

    @Override
    public RapportRevenusResponse getRapportRevenus(
            LocalDate dateDebut,
            LocalDate dateFin,
            Long agenceId,
            String categorie,
            String source
    ) {
        LocalDate debut = dateDebut != null ? dateDebut : LocalDate.now();
        LocalDate fin = dateFin != null ? dateFin : debut;
        if (fin.isBefore(debut)) {
            throw new BusinessException("La date de fin doit être supérieure ou égale à la date de début");
        }

        String categorieFilter = normalizeFilter(categorie);
        String sourceFilter = normalizeFilter(source);
        Long scopedAgenceId = resolveScopedAgenceId(agenceId);

        List<RevenuDetailDto> details = new ArrayList<>();
        RevenuKpiDto kpis = RevenuKpiDto.builder().build();
        Map<Long, AntenneAccumulator> parAntenne = new LinkedHashMap<>();
        Map<CategoryBreakdownKey, BigDecimal> parCategorie = new LinkedHashMap<>();
        CarnetAccumulator carnetAccumulator = new CarnetAccumulator();

        if (matchesSource(sourceFilter, SOURCE_CAISSE)) {
            addOperationCaisseRevenus(debut, fin, scopedAgenceId, categorieFilter, details, kpis, parAntenne, parCategorie);
        }
        if (matchesSource(sourceFilter, SOURCE_CREDIT)) {
            addCreditRevenus(debut, fin, scopedAgenceId, categorieFilter, details, kpis, parAntenne, parCategorie);
        }
        if (matchesSource(sourceFilter, SOURCE_COLLECTE)) {
            addCollecteLineRevenus(debut, fin, scopedAgenceId, categorieFilter, details, kpis, parAntenne, parCategorie, carnetAccumulator);
        }

        details.sort(Comparator.comparing(RevenuDetailDto::getDate, Comparator.nullsLast(Comparator.reverseOrder())));
        BigDecimal total = details.stream()
                .map(RevenuDetailDto::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        CarnetCostResult carnetCost = resolveCarnetCost(carnetAccumulator.nombreCarnets);
        Map<String, BigDecimal> charges = buildCharges(debut, fin, scopedAgenceId, carnetCost);
        List<ChargeDetailDto> chargeDetails = buildChargeDetails(debut, fin, scopedAgenceId);
        List<ChargeParCategorieDto> chargesParCategorie = buildChargesParCategorie(chargeDetails, carnetCost);
        List<MouvementNonRevenuParAntenneDto> mouvementsParAntenne = buildMouvementsNonRevenusParAntenne(debut, fin, scopedAgenceId);
        Map<String, BigDecimal> mouvementsNonRevenus = buildMouvementsNonRevenus(mouvementsParAntenne);
        PositionCreditDto positionCredit = buildPositionCredit(debut, fin, scopedAgenceId);
        List<ApportFinancementDetailDto> apportsDetails = buildApportsFinancementsDetails(debut, fin, scopedAgenceId);
        ApportFinancementDto apportsFinancements = buildApportsFinancements(apportsDetails, fin, scopedAgenceId);
        FondsMembresProtegesDto fondsMembresProteges = buildFondsMembresProteges(scopedAgenceId);
        List<ControleCoherenceDto> controles = buildControlesCoherence(debut, fin, scopedAgenceId, positionCredit);
        BigDecimal totalMouvementsNonRevenus = sumValues(mouvementsNonRevenus);
        BigDecimal totalCharges = sumValues(charges);
        BigDecimal beneficeNetEstime = total.subtract(totalCharges);
        MasseSalarialeDto masseSalariale = buildMasseSalariale(debut, fin, scopedAgenceId, beneficeNetEstime);
        BigDecimal resultatPrevisionnel = beneficeNetEstime.subtract(safeAmount(masseSalariale.getSalairesRestantAPayer()));
        masseSalariale.setResultatPrevisionnelApresSalairesAPayer(resultatPrevisionnel);
        TransportFixePrevuDto transportFixePrevu = buildTransportFixePrevu(debut, fin, scopedAgenceId);
        BigDecimal resultatPrevisionnelApresChargesFixes = resultatPrevisionnel.subtract(safeAmount(transportFixePrevu.getTotalTransportRestant()));
        SyntheseComparaisonAgencesDto comparaisonAgences = buildComparaisonAgences(
            debut,
            fin,
            scopedAgenceId,
            details,
            chargeDetails,
            masseSalariale,
            transportFixePrevu,
            carnetAccumulator,
            carnetCost
        );
        TresorerieDisponibleDto tresorerieDisponible = buildTresorerieDisponible(scopedAgenceId, fondsMembresProteges, masseSalariale, transportFixePrevu);
        CapaciteRetraitProprietaireDto capaciteRetraitProprietaire = buildCapaciteRetraitProprietaire(apportsFinancements, tresorerieDisponible, positionCredit, beneficeNetEstime);
        List<ControleCoherenceDto> controlesPaie = buildPaieControls(debut, fin, scopedAgenceId, masseSalariale);
        controles.addAll(controlesPaie);
        controles.addAll(buildTransportControls(scopedAgenceId, transportFixePrevu));

        return RapportRevenusResponse.builder()
                .dateDebut(debut)
                .dateFin(fin)
                .totalRevenus(total)
            .totalMouvementsNonRevenus(totalMouvementsNonRevenus)
            .totalCharges(totalCharges)
            .beneficeNetEstime(beneficeNetEstime)
            .resultatPrevisionnelApresSalairesAPayer(resultatPrevisionnel)
            .resultatPrevisionnelApresChargesFixes(resultatPrevisionnelApresChargesFixes)
            .chargesGlobalesSiege(comparaisonAgences.getChargesGlobalesSiege())
            .resultatApresChargesGlobalesSiege(comparaisonAgences.getResultatGlobalApresChargesSiege())
            .nombreCarnetsVendus(carnetAccumulator.nombreCarnets)
            .montantVentesCarnets(carnetAccumulator.montantVentes)
            .coutCarnetDisponible(carnetCost.disponible())
            .coutEstimeCarnets(carnetCost.coutTotal())
            .margeCarnets(carnetCost.disponible() ? carnetAccumulator.montantVentes.subtract(carnetCost.coutTotal()) : BigDecimal.ZERO)
            .messageMargeCarnets(carnetCost.disponible() ? null : "Non calculable, coût carnet non défini")
                .carnetMarge(CarnetMargeDto.builder()
                    .nombreCarnetsVendus(carnetAccumulator.nombreCarnets)
                    .montantVentesCarnets(carnetAccumulator.montantVentes)
                    .coutAchatUnitaireCarnet(carnetCost.coutUnitaire())
                    .coutTotalCarnets(carnetCost.coutTotal())
                    .margeCarnets(carnetAccumulator.montantVentes.subtract(carnetCost.coutTotal()))
                    .build())
                .positionCredit(positionCredit)
                .apportsFinancements(apportsFinancements)
                .capaciteRetraitProprietaire(capaciteRetraitProprietaire)
                .tresorerieDisponible(tresorerieDisponible)
                .masseSalariale(masseSalariale)
                .transportFixePrevu(transportFixePrevu)
                .fondsMembresProteges(fondsMembresProteges)
                .comparaisonAgences(comparaisonAgences)
                .apportsFinancementsDetails(apportsDetails)
                .kpis(kpis)
                .parAntenne(parAntenne.values().stream().map(AntenneAccumulator::toDto).toList())
                .parCategorie(parCategorie.entrySet().stream()
                        .map(entry -> RevenuParCategorieDto.builder()
                        .categorie(entry.getKey().categorie())
                        .sousCategorie(entry.getKey().sousCategorie())
                                .montant(entry.getValue())
                                .build())
                        .toList())
                        .mouvementsNonRevenus(toBreakdownList("MOUVEMENT_NON_REVENU", mouvementsNonRevenus))
                        .charges(toBreakdownList("CHARGE", charges))
                        .chargesParCategorie(chargesParCategorie)
                        .chargeDetails(chargeDetails)
                        .mouvementsNonRevenusParAntenne(mouvementsParAntenne)
                        .controlesCoherence(controles)
                .details(details)
                .build();
    }

    private void addOperationCaisseRevenus(
            LocalDate dateDebut,
            LocalDate dateFin,
            Long agenceId,
            String categorieFilter,
            List<RevenuDetailDto> details,
            RevenuKpiDto kpis,
            Map<Long, AntenneAccumulator> parAntenne,
                Map<CategoryBreakdownKey, BigDecimal> parCategorie
    ) {
        List<CategorieOperationCaisse> categories = List.of(
                CategorieOperationCaisse.FRAIS_DEMANDE_CREDIT,
                CategorieOperationCaisse.FRAIS_DEMANDE,
                CategorieOperationCaisse.FRAIS_RETRAIT_EPARGNE,
                CategorieOperationCaisse.ENTREE_DIVERSE
        );

        List<OperationCaisse> operations = operationCaisseRepository.findRevenueOperations(
                dateDebut.atStartOfDay(),
                dateFin.atTime(LocalTime.MAX),
                agenceId,
                categories
        );
        Map<Long, RecetteJournaliereTerrain> recettesById = loadRecettesById(operations);

        for (OperationCaisse operation : operations) {
            RevenueClassification classification = classifyOperation(operation, recettesById.get(operation.getRecetteId()));
            if (classification == null || !matchesCategory(categorieFilter, classification.categorie())) {
                continue;
            }

            BigDecimal montant = safeAmount(operation.getMontant());
            if (montant.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            AntenneInfo antenne = resolveAntenne(operation);
        addAmount(kpis, classification.categorie(), montant);
        addCategory(parCategorie, classification, montant);
        addAntenne(parAntenne, antenne, classification.categorie(), montant);

            details.add(RevenuDetailDto.builder()
                    .date(operation.getDateOperation())
                    .antenneId(antenne.id())
                    .antenne(antenne.nom())
            .categorie(classification.categorie())
            .nature(classification.nature())
            .sousCategorie(classification.sousCategorie())
                    .source(SOURCE_CAISSE)
                    .reference(firstNonBlank(operation.getReferenceMetier(), operation.getReferenceExterne(), operation.getNumeroPiece()))
                    .membre(operation.getMembre() != null ? operation.getMembre().getNomComplet() : null)
                    .montant(montant)
                    .utilisateur(resolveUtilisateur(operation))
                    .observation(firstNonBlank(operation.getObservation(), operation.getCommentaire(), operation.getDescription()))
                    .build());
        }
    }

    private void addCreditRevenus(
            LocalDate dateDebut,
            LocalDate dateFin,
            Long agenceId,
            String categorieFilter,
            List<RevenuDetailDto> details,
            RevenuKpiDto kpis,
            Map<Long, AntenneAccumulator> parAntenne,
            Map<CategoryBreakdownKey, BigDecimal> parCategorie
    ) {
        List<RemboursementCredit> remboursements = remboursementCreditRepository.findRevenueRemboursements(
                dateDebut.atStartOfDay(),
                dateFin.atTime(LocalTime.MAX),
                agenceId
        );

        for (RemboursementCredit remboursement : remboursements) {
            AntenneInfo antenne = resolveAntenne(remboursement);
            addCreditLine(remboursement, antenne, CAT_INTERETS_CREDIT, safeAmount(remboursement.getMontantInteret()), categorieFilter, details, kpis, parAntenne, parCategorie);
            addCreditLine(remboursement, antenne, CAT_PENALITES_CREDIT, safeAmount(remboursement.getMontantPenalite()), categorieFilter, details, kpis, parAntenne, parCategorie);
        }
    }

    private void addCreditLine(
            RemboursementCredit remboursement,
            AntenneInfo antenne,
            String categorie,
            BigDecimal montant,
            String categorieFilter,
            List<RevenuDetailDto> details,
            RevenuKpiDto kpis,
            Map<Long, AntenneAccumulator> parAntenne,
                Map<CategoryBreakdownKey, BigDecimal> parCategorie
    ) {
        if (!matchesCategory(categorieFilter, categorie) || montant.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        addAmount(kpis, categorie, montant);
        RevenueClassification classification = fixedClassification(categorie);
        addCategory(parCategorie, classification, montant);
        addAntenne(parAntenne, antenne, categorie, montant);

        details.add(RevenuDetailDto.builder()
                .date(remboursement.getDatePaiement())
                .antenneId(antenne.id())
                .antenne(antenne.nom())
                .categorie(categorie)
            .nature(classification.nature())
            .sousCategorie(classification.sousCategorie())
                .source(SOURCE_CREDIT)
                .reference(remboursement.getNumeroRecu())
                .membre(remboursement.getMembre() != null ? remboursement.getMembre().getNomComplet() : null)
                .montant(montant)
                .utilisateur(remboursement.getCreatedBy() != null ? remboursement.getCreatedBy().getNomComplet() : null)
                .observation(remboursement.getObservation())
                .build());
    }

    private void addCollecteLineRevenus(
            LocalDate dateDebut,
            LocalDate dateFin,
            Long agenceId,
            String categorieFilter,
            List<RevenuDetailDto> details,
            RevenuKpiDto kpis,
            Map<Long, AntenneAccumulator> parAntenne,
            Map<CategoryBreakdownKey, BigDecimal> parCategorie,
            CarnetAccumulator carnetAccumulator
    ) {
        if (matchesCategory(categorieFilter, CAT_CARNETS_VENDUS)) {
            List<CollecteMembreLigne> lignesCarnets = collecteMembreLigneRepository.findPositiveLinesByTypeAndPeriod(
                    TypeLigneCollecte.CARNET,
                    dateDebut,
                    dateFin,
                    agenceId
            );
            for (CollecteMembreLigne ligne : lignesCarnets) {
                AntenneInfo antenne = addCollecteLine(ligne, fixedClassification(CAT_CARNETS_VENDUS), details, kpis, parAntenne, parCategorie);
                carnetAccumulator.add(ligne, antenne.id());
            }
        }

        if (matchesCategory(categorieFilter, CAT_FRAIS_ANALYSE_CREDIT)) {
            List<CollecteMembreLigne> lignesFraisAnalyse = collecteMembreLigneRepository.findPositiveLinesByTypeAndPeriod(
                    TypeLigneCollecte.FRAIS_ANALYSE,
                    dateDebut,
                    dateFin,
                    agenceId
            );
            for (CollecteMembreLigne ligne : lignesFraisAnalyse) {
                addCollecteLine(ligne, new RevenueClassification(CAT_FRAIS_ANALYSE_CREDIT, NATURE_FRAIS_ANALYSE_TERRAIN), details, kpis, parAntenne, parCategorie);
            }
        }
    }

    private AntenneInfo addCollecteLine(
            CollecteMembreLigne ligne,
            RevenueClassification classification,
            List<RevenuDetailDto> details,
            RevenuKpiDto kpis,
            Map<Long, AntenneAccumulator> parAntenne,
            Map<CategoryBreakdownKey, BigDecimal> parCategorie
    ) {
        BigDecimal montant = safeAmount(ligne.getMontant());
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            return new AntenneInfo(null, "Non renseigné");
        }

        AntenneInfo antenne = resolveAntenne(ligne);
        addAmount(kpis, classification.categorie(), montant);
        addCategory(parCategorie, classification, montant);
        addAntenne(parAntenne, antenne, classification.categorie(), montant);

        LocalDate dateCollecte = ligne.getCollecte() != null ? ligne.getCollecte().getDateCollecte() : null;
        details.add(RevenuDetailDto.builder()
                .date(dateCollecte != null ? dateCollecte.atStartOfDay() : null)
                .antenneId(antenne.id())
                .antenne(antenne.nom())
                .categorie(classification.categorie())
                .nature(classification.nature())
                .sousCategorie(classification.sousCategorie())
                .source(SOURCE_COLLECTE)
                .reference(firstNonBlank(ligne.getReference(), ligne.getCollecte() != null ? "COLLECTE-" + ligne.getCollecte().getId() : null))
                .membre(ligne.getMembre() != null ? ligne.getMembre().getNomComplet() : null)
                .montant(montant)
                .utilisateur(null)
                .observation(buildCollecteLineObservation(ligne))
                .build());
            return antenne;
    }

    private Map<Long, RecetteJournaliereTerrain> loadRecettesById(List<OperationCaisse> operations) {
        Set<Long> recetteIds = operations.stream()
                .map(OperationCaisse::getRecetteId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (recetteIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return recetteJournaliereTerrainRepository.findAllById(recetteIds).stream()
                .collect(Collectors.toMap(RecetteJournaliereTerrain::getId, Function.identity()));
    }

    private RevenueClassification classifyOperation(OperationCaisse operation, RecetteJournaliereTerrain recette) {
        if (operation.getCategorieOperation() == CategorieOperationCaisse.FRAIS_DEMANDE_CREDIT
                || operation.getCategorieOperation() == CategorieOperationCaisse.FRAIS_DEMANDE) {
            return fixedClassification(CAT_FRAIS_ANALYSE_CREDIT);
        }
        if (operation.getCategorieOperation() == CategorieOperationCaisse.FRAIS_RETRAIT_EPARGNE) {
            return fixedClassification(CAT_FRAIS_RETRAIT_EPARGNE);
        }
        if (operation.getCategorieOperation() == CategorieOperationCaisse.ENTREE_DIVERSE
                && isRevenuDiversEligible(operation)) {
            return classifyEntreeDiverse(operation, recette);
        }
        return null;
    }

    private RevenueClassification classifyEntreeDiverse(OperationCaisse operation, RecetteJournaliereTerrain recette) {
        String nature = resolveRevenuDiversNature(operation, recette);
        if (nature == null) {
            return null;
        }
        if (NATURE_FRAIS_ANALYSE_TERRAIN.equals(nature)) {
            return new RevenueClassification(CAT_FRAIS_ANALYSE_CREDIT, nature);
        }
        return new RevenueClassification(CAT_REVENUS_DIVERS, nature);
    }

    private RevenueClassification fixedClassification(String categorie) {
        return switch (categorie) {
            case CAT_FRAIS_ANALYSE_CREDIT -> new RevenueClassification(categorie, NATURE_FRAIS_ANALYSE_CREDIT);
            case CAT_FRAIS_RETRAIT_EPARGNE -> new RevenueClassification(categorie, NATURE_FRAIS_RETRAIT_EPARGNE);
            case CAT_INTERETS_CREDIT -> new RevenueClassification(categorie, NATURE_INTERET_CREDIT);
            case CAT_PENALITES_CREDIT -> new RevenueClassification(categorie, NATURE_PENALITE_CREDIT);
            case CAT_CARNETS_VENDUS -> new RevenueClassification(categorie, NATURE_VENTE_CARNET);
            default -> new RevenueClassification(categorie, NATURE_NON_CATEGORISE);
        };
    }

    private String resolveRevenuDiversNature(OperationCaisse operation, RecetteJournaliereTerrain recette) {
        String text = normalizeText(firstNonBlank(
                operation.getObservation(),
                operation.getCommentaire(),
                operation.getDescription(),
                operation.getReferenceMetier(),
                operation.getReferenceExterne(),
                recette != null ? recette.getObservation() : null,
                recette != null ? recette.getReferencePapier() : null
        ));

        if (containsAny(text, "carnet")) {
            return null;
        }
        if (containsAny(text, "analyse", "frais analyse", "demande credit", "demande crédit")) {
            return NATURE_FRAIS_ANALYSE_TERRAIN;
        }
        if (containsAny(text, "excedent", "excédent", "surplus")) {
            return NATURE_EXCEDENT_CAISSE;
        }
        if ((operation.getSource() == SourceOperationCaisse.MANUEL || operation.getSource() == SourceOperationCaisse.AUTRE)
                && containsAny(text, "revenu", "recette", "service", "commission", "frais")) {
            return NATURE_AUTRE_REVENU;
        }
        return null;
    }

    private boolean isRevenuDiversEligible(OperationCaisse operation) {
        SourceOperationCaisse source = operation.getSource();
        return source == SourceOperationCaisse.MANUEL
            || source == SourceOperationCaisse.AUTRE
            || source == SourceOperationCaisse.RECETTE_JOURNALIERE;
    }

    private Long resolveScopedAgenceId(Long requestedAgenceId) {
        Utilisateur currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null || currentUser.getRole() == null || currentUser.getRole().getCode() == null) {
            throw new BusinessException("Utilisateur non authentifié");
        }

        RoleCode role = currentUser.getRole().getCode();
        if (isGlobalRole(role)) {
            return requestedAgenceId;
        }

        Long currentAgenceId = resolveCurrentUserAgenceId(currentUser);
        if (currentAgenceId == null) {
            return -1L;
        }
        if (requestedAgenceId != null && !requestedAgenceId.equals(currentAgenceId)) {
            throw new BusinessException("Accès refusé à cette antenne");
        }
        return currentAgenceId;
    }

    private boolean isGlobalRole(RoleCode role) {
        return EnumSet.of(RoleCode.ADMIN, RoleCode.GERANT_GENERAL, RoleCode.COO, RoleCode.RCI).contains(role);
    }

    private Long resolveCurrentUserAgenceId(Utilisateur currentUser) {
        if (currentUser.getEmploye() != null
                && currentUser.getEmploye().getAgence() != null
                && currentUser.getEmploye().getAgence().getId() != null) {
            return currentUser.getEmploye().getAgence().getId();
        }
        if (currentUser.getSite() != null
                && currentUser.getSite().getAgence() != null
                && currentUser.getSite().getAgence().getId() != null) {
            return currentUser.getSite().getAgence().getId();
        }
        if (currentUser.getEmploye() != null
                && currentUser.getEmploye().getSite() != null
                && currentUser.getEmploye().getSite().getAgence() != null
                && currentUser.getEmploye().getSite().getAgence().getId() != null) {
            return currentUser.getEmploye().getSite().getAgence().getId();
        }
        return null;
    }

    private Agence resolveEmployeAgence(Employe employe) {
        if (employe == null) {
            return null;
        }
        if (employe.getSite() != null && employe.getSite().getAgence() != null) {
            return employe.getSite().getAgence();
        }
        return employe.getAgence();
    }

    private boolean isSiegePoste(PosteEmploye poste) {
        return poste == PosteEmploye.COO || poste == PosteEmploye.RCI;
    }

    private AntenneInfo resolveAntenne(OperationCaisse operation) {
        Agence agence = null;
        if (operation.getCaisse() != null && operation.getCaisse().getAgence() != null) {
            agence = operation.getCaisse().getAgence();
        } else if (operation.getSite() != null && operation.getSite().getAgence() != null) {
            agence = operation.getSite().getAgence();
        } else if (operation.getCaisse() != null && operation.getCaisse().getSite() != null) {
            agence = operation.getCaisse().getSite().getAgence();
        }
        return toAntenneInfo(agence);
    }

    private AntenneInfo resolveAntenne(RemboursementCredit remboursement) {
        Agence agence = null;
        if (remboursement.getCredit() != null && remboursement.getCredit().getSite() != null) {
            agence = remboursement.getCredit().getSite().getAgence();
        }
        if (agence == null && remboursement.getMembre() != null && remboursement.getMembre().getSite() != null) {
            agence = remboursement.getMembre().getSite().getAgence();
        }
        return toAntenneInfo(agence);
    }

    private AntenneInfo resolveAntenne(Credit credit) {
        Agence agence = null;
        if (credit.getSite() != null && credit.getSite().getAgence() != null) {
            agence = credit.getSite().getAgence();
        }
        if (agence == null && credit.getMembre() != null && credit.getMembre().getSite() != null) {
            agence = credit.getMembre().getSite().getAgence();
        }
        return toAntenneInfo(agence);
    }

    private AntenneInfo resolveAntenne(CollecteMembreLigne ligne) {
        if (ligne.getCollecte() == null) {
            return new AntenneInfo(null, "Non renseigné");
        }
        Site site = ligne.getCollecte().getSite();
        if (site != null && site.getAgence() != null) {
            return toAntenneInfo(site.getAgence());
        }
        Long antenneId = ligne.getCollecte().getAntenneId();
        return new AntenneInfo(antenneId, antenneId != null ? "Antenne " + antenneId : "Non renseigné");
    }

    private AntenneInfo resolveAntenne(DepenseCaisse depense) {
        Agence agence = null;
        if (depense.getCaisse() != null && depense.getCaisse().getAgence() != null) {
            agence = depense.getCaisse().getAgence();
        } else if (depense.getSite() != null && depense.getSite().getAgence() != null) {
            agence = depense.getSite().getAgence();
        } else if (depense.getCaisse() != null && depense.getCaisse().getSite() != null) {
            agence = depense.getCaisse().getSite().getAgence();
        }
        return toAntenneInfo(agence);
    }

    private AntenneInfo resolveAntenne(DemandeCredit demande) {
        Agence agence = null;
        if (demande.getSite() != null && demande.getSite().getAgence() != null) {
            agence = demande.getSite().getAgence();
        }
        if (agence == null && demande.getMembre() != null && demande.getMembre().getSite() != null) {
            agence = demande.getMembre().getSite().getAgence();
        }
        return toAntenneInfo(agence);
    }

    private AntenneInfo resolveAntenne(CollecteJournaliereTerrain collecte) {
        if (collecte == null) {
            return new AntenneInfo(null, "Non renseigné");
        }
        if (collecte.getSite() != null && collecte.getSite().getAgence() != null) {
            return toAntenneInfo(collecte.getSite().getAgence());
        }
        Long antenneId = collecte.getAntenneId();
        return new AntenneInfo(antenneId, antenneId != null ? "Antenne " + antenneId : "Non renseigné");
    }

    private AntenneInfo toAntenneInfo(Agence agence) {
        if (agence == null) {
            return new AntenneInfo(null, "Non renseigné");
        }
        return new AntenneInfo(agence.getId(), firstNonBlank(agence.getNomAgence(), agence.getCodeAgence(), "Antenne " + agence.getId()));
    }

    private void addAmount(RevenuKpiDto kpis, String categorie, BigDecimal montant) {
        switch (categorie) {
            case CAT_FRAIS_ANALYSE_CREDIT -> kpis.setFraisAnalyseCredit(kpis.getFraisAnalyseCredit().add(montant));
            case CAT_FRAIS_RETRAIT_EPARGNE -> kpis.setFraisRetraitEpargne(kpis.getFraisRetraitEpargne().add(montant));
            case CAT_INTERETS_CREDIT -> kpis.setInteretsCredit(kpis.getInteretsCredit().add(montant));
            case CAT_PENALITES_CREDIT -> kpis.setPenalitesCredit(kpis.getPenalitesCredit().add(montant));
            case CAT_CARNETS_VENDUS -> kpis.setCarnetsVendus(kpis.getCarnetsVendus().add(montant));
            case CAT_REVENUS_DIVERS -> kpis.setRevenusDivers(kpis.getRevenusDivers().add(montant));
            default -> { }
        }
    }

    private void addCategory(Map<CategoryBreakdownKey, BigDecimal> parCategorie, RevenueClassification classification, BigDecimal montant) {
        parCategorie.merge(new CategoryBreakdownKey(classification.categorie(), classification.sousCategorie()), montant, BigDecimal::add);
    }

    private Map<String, BigDecimal> buildMouvementsNonRevenus(List<MouvementNonRevenuParAntenneDto> mouvementsParAntenne) {
        Map<String, BigDecimal> mouvements = new LinkedHashMap<>();
        addIfPositive(mouvements, "Entrées non revenus - Épargne collectée", sumMovement(mouvementsParAntenne, MouvementNonRevenuParAntenneDto::getEpargneCollectee));
        addIfPositive(mouvements, "Entrées non revenus - Principal crédit remboursé", sumMovement(mouvementsParAntenne, MouvementNonRevenuParAntenneDto::getPrincipalCreditRembourse));
        addIfPositive(mouvements, "Entrées non revenus - Garanties / dépôts garantie", sumMovement(mouvementsParAntenne, MouvementNonRevenuParAntenneDto::getGarantiesDepotGarantie));
        addIfPositive(mouvements, "Entrées non revenus - Approvisionnements caisse", sumMovement(mouvementsParAntenne, MouvementNonRevenuParAntenneDto::getApprovisionnementsCaisse));
        addIfPositive(mouvements, "Sorties non charges - Retraits épargne", sumMovement(mouvementsParAntenne, MouvementNonRevenuParAntenneDto::getRetraitsEpargne));
        addIfPositive(mouvements, "Sorties non charges - Décaissements crédit", sumMovement(mouvementsParAntenne, MouvementNonRevenuParAntenneDto::getDecaissementsCredit));
        addIfPositive(mouvements, "Autres mouvements exclus", sumMovement(mouvementsParAntenne, MouvementNonRevenuParAntenneDto::getAutresMouvementsNonRevenus));
        return mouvements;
    }

    private BigDecimal sumMovement(List<MouvementNonRevenuParAntenneDto> mouvementsParAntenne, Function<MouvementNonRevenuParAntenneDto, BigDecimal> extractor) {
        return mouvementsParAntenne.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, BigDecimal> buildCharges(LocalDate dateDebut, LocalDate dateFin, Long agenceId, CarnetCostResult carnetCost) {
        Map<String, BigDecimal> charges = new LinkedHashMap<>();
        addIfPositive(charges, "Dépenses caisse payées", depenseCaisseRepository.sumDepensesPayeesByPeriodAndAgence(dateDebut.atStartOfDay(), dateFin.atTime(LocalTime.MAX), agenceId));
        if (carnetCost.disponible()) {
            addIfPositive(charges, "Achat carnets", carnetCost.coutTotal());
        }
        return charges;
    }

    private List<ChargeDetailDto> buildChargeDetails(LocalDate dateDebut, LocalDate dateFin, Long agenceId) {
        return depenseCaisseRepository.findDepensesPayeesWithContext(dateDebut.atStartOfDay(), dateFin.atTime(LocalTime.MAX), agenceId)
                .stream()
                .map(depense -> {
                    AntenneInfo antenne = resolveAntenne(depense);
                    boolean canRattacherPaie = depense.getCategorie() == DepenseCaisseCategorie.SALAIRE
                        && (depense.getStatut() == DepenseCaisseStatus.PAYEE || depense.getStatut() == DepenseCaisseStatus.VALIDEE)
                        && (depense.getEmploye() == null
                            || depense.getPeriodePaie() == null
                            || depense.getPeriodePaie().isBlank()
                            || depense.getTypePaiementPersonnel() == null);
                    boolean canRattacherTransport = depense.getCategorie() == DepenseCaisseCategorie.TRANSPORT
                        && (depense.getStatut() == DepenseCaisseStatus.PAYEE || depense.getStatut() == DepenseCaisseStatus.VALIDEE)
                        && (depense.getEmploye() == null
                            || depense.getPeriodeCharge() == null
                            || depense.getPeriodeCharge().isBlank()
                            || depense.getSiteCharge() == null
                            || depense.getTypeChargeFixe() == null);
                    return ChargeDetailDto.builder()
                        .depenseId(depense.getId())
                            .caisseId(depense.getCaisse() != null ? depense.getCaisse().getId() : null)
                            .date(depense.getDatePaiement())
                            .antenneId(antenne.id())
                            .antenne(antenne.nom())
                            .categorie(chargeCategoryLabel(depense.getCategorie()))
                        .categorieTechnique(depense.getCategorie() != null ? depense.getCategorie().name() : null)
                            .reference(depense.getOperationCaisse() != null ? depense.getOperationCaisse().getNumeroPiece() : "DEPENSE-" + depense.getId())
                            .beneficiaire(firstNonBlank(depense.getBeneficiaireNom(), depense.getBeneficiaire(), depense.getBeneficiaireUtilisateur() != null ? depense.getBeneficiaireUtilisateur().getNomComplet() : null))
                            .montant(safeAmount(depense.getMontant()))
                            .employeId(depense.getEmploye() != null ? depense.getEmploye().getId() : null)
                            .employeMatricule(depense.getEmploye() != null ? depense.getEmploye().getMatricule() : null)
                            .employeNomComplet(depense.getEmploye() != null ? depense.getEmploye().getNomComplet() : null)
                            .employePoste(depense.getEmploye() != null && depense.getEmploye().getFonction() != null ? depense.getEmploye().getFonction().name() : null)
                            .periodePaie(depense.getPeriodePaie())
                            .typePaiementPersonnel(depense.getTypePaiementPersonnel() != null ? depense.getTypePaiementPersonnel().name() : null)
                            .montantRemunerationReference(depense.getMontantRemunerationReference())
                            .montantEcartRemuneration(depense.getMontantEcartRemuneration())
                            .motifEcartRemuneration(depense.getMotifEcartRemuneration())
                            .naturePaiementPaie(depense.getNaturePaiementPaie())
                            .montantSalaireDu(depense.getMontantSalaireDu())
                            .montantDejaPaye(depense.getMontantDejaPaye())
                            .montantRestantApresPaiement(depense.getMontantRestantApresPaiement())
                            .montantRetenue(depense.getMontantRetenue())
                            .motifRetenue(depense.getMotifRetenue())
                            .motifPaiementPartiel(depense.getMotifPaiementPartiel())
                            .commentairePaie(depense.getCommentairePaie())
                            .periodeCharge(depense.getPeriodeCharge())
                            .typeChargeFixe(depense.getTypeChargeFixe() != null ? depense.getTypeChargeFixe().name() : null)
                            .siteChargeId(depense.getSiteCharge() != null ? depense.getSiteCharge().getId() : null)
                            .siteChargeNom(depense.getSiteCharge() != null ? depense.getSiteCharge().getNomSite() : null)
                            .montantChargeFixeReference(depense.getMontantChargeFixeReference())
                            .montantEcartChargeFixe(depense.getMontantEcartChargeFixe())
                            .commentaireRapprochement(depense.getCommentaireRapprochement())
                            .salaireBase(depense.getEmploye() != null ? depense.getEmploye().getSalaireBase() : null)
                            .epargneCollecteeReference(depense.getEpargneCollecteeReference())
                            .remboursementCollecteReference(depense.getRemboursementCollecteReference())
                            .nombreCarnetsVendus(depense.getNombreCarnetsVendus())
                            .primeMobilisationEpargne(depense.getPrimeMobilisationEpargne())
                            .primeMobilisationRemboursement(depense.getPrimeMobilisationRemboursement())
                            .bonusCarnets(depense.getBonusCarnets())
                            .primeMotivationManuelle(depense.getPrimeMotivationManuelle())
                            .modeCalculPaie(depense.getModeCalculPaie() != null ? depense.getModeCalculPaie().name() : null)
                            .statut(depense.getStatut() != null ? depense.getStatut().name() : null)
                            .canRattacherPaie(canRattacherPaie)
                            .canRattacherTransport(canRattacherTransport)
                            .observation(firstNonBlank(depense.getMotif(), depense.getCommentaireValidation()))
                            .build();
                })
                .toList();
    }

    private List<ChargeParCategorieDto> buildChargesParCategorie(List<ChargeDetailDto> chargeDetails, CarnetCostResult carnetCost) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (ChargeDetailDto detail : chargeDetails) {
            totals.merge(detail.getCategorie(), safeAmount(detail.getMontant()), BigDecimal::add);
        }
        if (carnetCost.disponible() && carnetCost.coutTotal().compareTo(BigDecimal.ZERO) > 0) {
            totals.merge("Achat carnets", carnetCost.coutTotal(), BigDecimal::add);
        }
        return totals.entrySet().stream()
                .map(entry -> ChargeParCategorieDto.builder()
                        .categorie(entry.getKey())
                        .montant(entry.getValue())
                        .build())
                .toList();
    }

    private String chargeCategoryLabel(DepenseCaisseCategorie categorie) {
        if (categorie == null) {
            return "Autre dépense";
        }
        return switch (categorie) {
            case FOURNITURE_BUREAU, MATERIEL, LOYER, ENTRETIEN -> "Fonctionnement";
            case TRANSPORT -> "Transport";
            case SALAIRE -> "Salaire / prime / commission";
            case COMMUNICATION -> "Communication";
            case AUTRE -> "Autre dépense";
        };
    }

    private SyntheseComparaisonAgencesDto buildComparaisonAgences(
            LocalDate dateDebut,
            LocalDate dateFin,
            Long agenceId,
            List<RevenuDetailDto> revenus,
            List<ChargeDetailDto> chargeDetails,
            MasseSalarialeDto masseSalariale,
            TransportFixePrevuDto transportFixePrevu,
            CarnetAccumulator carnetAccumulator,
            CarnetCostResult carnetCost
    ) {
        Map<Long, ComparaisonAgenceDto> agences = buildComparaisonAgenceRows(agenceId);
        BigDecimal chargesSiege = BigDecimal.ZERO;

        for (RevenuDetailDto revenu : revenus) {
            ComparaisonAgenceDto row = agences.get(revenu.getAntenneId());
            if (row != null) {
                addRevenuComparaison(row, revenu.getCategorie(), safeAmount(revenu.getMontant()));
            }
        }

        for (ChargeDetailDto charge : chargeDetails) {
            if (isSalaryCharge(charge) || isFixedTransportCharge(charge)) {
                continue;
            }
            BigDecimal montant = safeAmount(charge.getMontant());
            ComparaisonAgenceDto row = agences.get(charge.getAntenneId());
            if (row != null) {
                addDepenseComparaison(row, charge.getCategorieTechnique(), montant);
            } else {
                chargesSiege = chargesSiege.add(montant);
            }
        }

        if (carnetCost.disponible()) {
            for (Map.Entry<Long, Integer> entry : carnetAccumulator.nombreCarnetsParAgence.entrySet()) {
                BigDecimal montant = carnetCost.coutUnitaire().multiply(BigDecimal.valueOf(entry.getValue()));
                ComparaisonAgenceDto row = agences.get(entry.getKey());
                if (row != null) {
                    row.setAchatCarnets(row.getAchatCarnets().add(montant));
                    row.setChargesConnues(row.getChargesConnues().add(montant));
                } else {
                    chargesSiege = chargesSiege.add(montant);
                }
            }
        }

        for (DetailMasseSalarialeDto detail : masseSalariale.getDetailsEmployes()) {
            BigDecimal chargePaie = safeAmount(detail.getMontantPaye()).add(safeAmount(detail.getResteAPayer()));
            ComparaisonAgenceDto row = Boolean.TRUE.equals(detail.getChargeSiege()) ? null : agences.get(detail.getAgenceId());
            if (row != null) {
                row.setSalairesPayes(row.getSalairesPayes().add(safeAmount(detail.getMontantPaye())));
                row.setSalairesRestantAPayer(row.getSalairesRestantAPayer().add(safeAmount(detail.getResteAPayer())));
                row.setPrimes(row.getPrimes().add(safeAmount(detail.getPrimesBonusJustifies())));
                row.setChargesConnues(row.getChargesConnues().add(chargePaie));
            } else {
                chargesSiege = chargesSiege.add(chargePaie);
            }
        }

        for (TransportFixeDetailDto detail : transportFixePrevu.getDetails()) {
            BigDecimal chargeTransport = safeAmount(detail.getMontantPaye()).add(safeAmount(detail.getResteAPayer()));
            ComparaisonAgenceDto row = agences.get(detail.getAgenceId());
            if (row != null) {
                row.setTransportTerrain(row.getTransportTerrain().add(safeAmount(detail.getMontantPaye())));
                row.setTransportRestantAPayer(row.getTransportRestantAPayer().add(safeAmount(detail.getResteAPayer())));
                row.setChargesConnues(row.getChargesConnues().add(chargeTransport));
            } else {
                chargesSiege = chargesSiege.add(chargeTransport);
            }
        }

        for (ComparaisonAgenceDto row : agences.values()) {
            row.setCollectesValidees(collecteJournaliereTerrainRepository.countValideesByAgenceAndPeriod(dateDebut, dateFin, row.getAgenceId()));
            row.setMembresActifs(membreRepository.countActiveByAgence(row.getAgenceId()));
            row.setCreditsActifs(creditRepository.countByStatutInAndAgence(activeCreditStatuses(), row.getAgenceId()));
            row.setResultatNetEstime(row.getRevenusReels().subtract(row.getChargesConnues()));
            row.setMargePourcentage(calculateMargin(row.getResultatNetEstime(), row.getRevenusReels()));
            row.setAlerteDeficit(row.getResultatNetEstime().compareTo(BigDecimal.ZERO) < 0);
            row.setAlerteChargesElevees(row.getChargesConnues().compareTo(BigDecimal.ZERO) > 0
                    && row.getChargesConnues().compareTo(row.getRevenusReels()) > 0);
        }

        BigDecimal totalRevenusAgences = agences.values().stream()
                .map(ComparaisonAgenceDto::getRevenusReels)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalChargesAgences = agences.values().stream()
                .map(ComparaisonAgenceDto::getChargesConnues)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalResultatAgences = totalRevenusAgences.subtract(totalChargesAgences);
        BigDecimal resultatGlobalApresSiege = totalResultatAgences.subtract(chargesSiege);
        List<ComparaisonAgenceDto> rows = new ArrayList<>(agences.values());

        return SyntheseComparaisonAgencesDto.builder()
                .agences(rows)
                .totalRevenusAgences(totalRevenusAgences)
                .totalChargesAgences(totalChargesAgences)
                .totalResultatAgences(totalResultatAgences)
                .chargesGlobalesSiege(chargesSiege)
                .resultatGlobalApresChargesSiege(resultatGlobalApresSiege)
                .agencePlusRevenus(maxBy(rows, ComparaisonAgenceDto::getRevenusReels))
                .agencePlusRentable(maxBy(rows, ComparaisonAgenceDto::getResultatNetEstime))
                .agencePlusCharges(maxBy(rows, ComparaisonAgenceDto::getChargesConnues))
                .agencesDeficitaires(rows.stream()
                        .filter(row -> Boolean.TRUE.equals(row.getAlerteDeficit()))
                        .toList())
                .build();
    }

    private Map<Long, ComparaisonAgenceDto> buildComparaisonAgenceRows(Long agenceId) {
        List<Agence> agences = agenceId != null
                ? agenceRepository.findById(agenceId).stream().toList()
                : agenceRepository.findByActifTrue();
        return agences.stream()
                .filter(agence -> agence.getId() != null)
                .sorted(Comparator.comparing(Agence::getNomAgence, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toMap(
                        Agence::getId,
                        agence -> ComparaisonAgenceDto.builder()
                                .agenceId(agence.getId())
                                .agenceNom(firstNonBlank(agence.getNomAgence(), agence.getCodeAgence(), "Antenne " + agence.getId()))
                                .build(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
    }

    private void addRevenuComparaison(ComparaisonAgenceDto row, String categorie, BigDecimal montant) {
        row.setRevenusReels(row.getRevenusReels().add(montant));
        switch (categorie) {
            case CAT_FRAIS_RETRAIT_EPARGNE -> row.setCommissionsRetrait(row.getCommissionsRetrait().add(montant));
            case CAT_FRAIS_ANALYSE_CREDIT -> row.setFraisCredit(row.getFraisCredit().add(montant));
            case CAT_INTERETS_CREDIT -> row.setInteretsCredit(row.getInteretsCredit().add(montant));
            case CAT_PENALITES_CREDIT -> row.setPenalitesCredit(row.getPenalitesCredit().add(montant));
            case CAT_CARNETS_VENDUS -> row.setCarnetsVendus(row.getCarnetsVendus().add(montant));
            default -> row.setAutresRevenus(row.getAutresRevenus().add(montant));
        }
    }

    private void addDepenseComparaison(ComparaisonAgenceDto row, String categorieTechnique, BigDecimal montant) {
        row.setChargesConnues(row.getChargesConnues().add(montant));
        DepenseCaisseCategorie categorie = parseDepenseCategorie(categorieTechnique);
        if (categorie == DepenseCaisseCategorie.FOURNITURE_BUREAU
                || categorie == DepenseCaisseCategorie.MATERIEL
                || categorie == DepenseCaisseCategorie.LOYER
                || categorie == DepenseCaisseCategorie.ENTRETIEN
                || categorie == DepenseCaisseCategorie.COMMUNICATION) {
            row.setFonctionnement(row.getFonctionnement().add(montant));
        } else {
            row.setAutresCharges(row.getAutresCharges().add(montant));
        }
    }

    private boolean isSalaryCharge(ChargeDetailDto charge) {
        return parseDepenseCategorie(charge.getCategorieTechnique()) == DepenseCaisseCategorie.SALAIRE;
    }

    private boolean isFixedTransportCharge(ChargeDetailDto charge) {
        return parseDepenseCategorie(charge.getCategorieTechnique()) == DepenseCaisseCategorie.TRANSPORT
                && (charge.getEmployeId() != null || firstNonBlank(charge.getPeriodeCharge(), charge.getTypeChargeFixe()) != null);
    }

    private DepenseCaisseCategorie parseDepenseCategorie(String categorieTechnique) {
        if (categorieTechnique == null || categorieTechnique.isBlank()) {
            return null;
        }
        try {
            return DepenseCaisseCategorie.valueOf(categorieTechnique);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private BigDecimal calculateMargin(BigDecimal resultat, BigDecimal revenus) {
        BigDecimal safeRevenus = safeAmount(revenus);
        if (safeRevenus.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return safeAmount(resultat)
                .multiply(BigDecimal.valueOf(100))
                .divide(safeRevenus, 2, RoundingMode.HALF_UP);
    }

    private ComparaisonAgenceDto maxBy(List<ComparaisonAgenceDto> rows, Function<ComparaisonAgenceDto, BigDecimal> extractor) {
        return rows.stream()
                .max(Comparator.comparing(row -> safeAmount(extractor.apply(row))))
                .orElse(null);
    }

    private List<ApportFinancementDetailDto> buildApportsFinancementsDetails(LocalDate dateDebut, LocalDate dateFin, Long agenceId) {
        return operationCaisseRepository.findAccountingMovementOperations(
                        dateDebut.atStartOfDay(),
                        dateFin.atTime(LocalTime.MAX),
                        agenceId,
                        List.of(CategorieOperationCaisse.APPROVISIONNEMENT)
                )
                .stream()
                .map(operation -> {
                    AntenneInfo antenne = resolveAntenne(operation);
                    return ApportFinancementDetailDto.builder()
                            .operationId(operation.getId())
                            .date(operation.getDateOperation())
                            .antenneId(antenne.id())
                            .antenne(antenne.nom())
                            .categorie("APPROVISIONNEMENT")
                            .natureFinancement(natureFinancementLabel(operation.getNatureFinancement()))
                            .reference(firstNonBlank(operation.getReferenceMetier(), operation.getReferenceExterne(), operation.getNumeroPiece()))
                            .utilisateur(resolveUtilisateur(operation))
                            .montant(safeAmount(operation.getMontant()))
                            .observation(firstNonBlank(operation.getObservation(), operation.getCommentaire(), operation.getDescription()))
                            .build();
                })
                .toList();
    }

    private ApportFinancementDto buildApportsFinancements(List<ApportFinancementDetailDto> details, LocalDate dateFin, Long agenceId) {
        BigDecimal transfertsInternes = sumApportsByNature(details, NatureFinancementApprovisionnement.TRANSFERT_INTERNE);
        BigDecimal apportsProprietaire = sumApportsByNature(details, NatureFinancementApprovisionnement.APPORT_PROPRIETAIRE);
        BigDecimal pretsRecus = sumApportsByNature(details, NatureFinancementApprovisionnement.PRET_RECU);
        BigDecimal remboursementsAvance = sumApportsByNature(details, NatureFinancementApprovisionnement.REMBOURSEMENT_AVANCE);
        BigDecimal autresFinancements = sumApportsByNature(details, NatureFinancementApprovisionnement.AUTRE_FINANCEMENT);
        BigDecimal nonQualifies = details.stream()
                .filter(detail -> "Approvisionnement non qualifié".equals(detail.getNatureFinancement()))
                .map(ApportFinancementDetailDto::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = details.stream()
                .map(ApportFinancementDetailDto::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal capitalInjecte = safeAmount(operationCaisseRepository.sumApprovisionnementsByNatureUntil(dateFin.atTime(LocalTime.MAX), agenceId, NatureFinancementApprovisionnement.APPORT_PROPRIETAIRE));
        BigDecimal remboursementsPayes = safeAmount(remboursementApportRepository.sumRemboursementsPayesByAgence(agenceId));
        BigDecimal capitalRestant = capitalInjecte.subtract(remboursementsPayes).max(BigDecimal.ZERO);

        return ApportFinancementDto.builder()
                .totalApprovisionnements(total)
                .apportsProprietaire(apportsProprietaire)
                .transfertsInternes(transfertsInternes)
                .pretsRecus(pretsRecus)
                .remboursementsAvance(remboursementsAvance)
                .autresFinancements(autresFinancements)
                .approvisionnementsNonQualifies(nonQualifies)
                .capitalInjecteARecuperer(capitalInjecte)
                .remboursementsApportPayes(remboursementsPayes)
                .capitalInjecteRestantARecuperer(capitalRestant)
                .capitalInjecteIndicatif(true)
                .commentairePedagogique("Un apport propriétaire augmente la caisse mais n'est pas un revenu. Les remboursements d'apport payés réduisent le capital restant à récupérer et ne sont pas comptés comme charges.")
                .build();
    }

        private TresorerieDisponibleDto buildTresorerieDisponible(Long agenceId, FondsMembresProtegesDto fondsMembresProteges, MasseSalarialeDto masseSalariale, TransportFixePrevuDto transportFixePrevu) {
        BigDecimal soldeCaisseActif = safeAmount(sessionCaisseRepository.sumSoldeTheoriqueByStatutsAndAgence(
                List.of(StatutSessionCaisse.OUVERTE, StatutSessionCaisse.PRE_CLOTUREE, StatutSessionCaisse.VALIDEE_CONTROLE),
                agenceId
        ));
        BigDecimal retraitsValides = safeAmount(demandeRetraitEpargneRepository.sumRetraitsValidesNonPayesByAgence(agenceId));
        BigDecimal creditsApprouves = safeAmount(creditRepository.sumCreditsApprouvesNonDecaissesByAgence(agenceId));
        BigDecimal depensesValidees = safeAmount(depenseCaisseRepository.sumDepensesValideesNonPayeesByAgence(agenceId));
        BigDecimal fondsMinimum = getOptionalDecimalParam("FONDS_MINIMUM_CAISSE");
        BigDecimal margePrudence = getOptionalDecimalParam("MARGE_PRUDENCE_TRESORERIE");
        BigDecimal fondsMembres = safeAmount(fondsMembresProteges.getTotalFondsMembres());
        BigDecimal salairesRestantAPayer = safeAmount(masseSalariale != null ? masseSalariale.getSalairesRestantAPayer() : null);
        BigDecimal transportRestantAPayer = safeAmount(transportFixePrevu != null ? transportFixePrevu.getTotalTransportRestant() : null);
        BigDecimal tresorerieApresProtectionMembres = soldeCaisseActif.subtract(fondsMembres);
        BigDecimal engagements = retraitsValides.add(creditsApprouves).add(depensesValidees).add(salairesRestantAPayer).add(transportRestantAPayer);
        BigDecimal tresoreriePrudente = tresorerieApresProtectionMembres
            .subtract(engagements)
            .subtract(fondsMinimum)
            .subtract(margePrudence);

        return TresorerieDisponibleDto.builder()
                .soldeCaisseTheoriqueActif(soldeCaisseActif)
                .retraitsEpargneValidesNonPayes(retraitsValides)
            .fondsMembresAProteger(fondsMembres)
            .tresorerieApresProtectionMembres(tresorerieApresProtectionMembres)
                .creditsApprouvesNonDecaisses(creditsApprouves)
                .depensesValideesNonPayees(depensesValidees)
                .salairesRestantAPayer(salairesRestantAPayer)
                .transportRestantAPayer(transportRestantAPayer)
                .fondsMinimumSecurite(fondsMinimum)
            .margePrudence(margePrudence)
                .totalEngagementsCourtTerme(engagements)
            .tresorerieDisponibleApresEngagements(tresoreriePrudente)
            .tresorerieRecuperablePrudente(tresoreriePrudente)
                .soldeCaisseIndicatif(true)
            .commentairePedagogique("Trésorerie récupérable prudente : solde théorique des sessions de caisse actives moins fonds membres à protéger, retraits épargne validés non payés, crédits approuvés non décaissés, dépenses validées non payées, salaires et transport terrain restant à payer, fonds minimum de sécurité et marge de prudence éventuelle.")
                .build();
    }

    private FondsMembresProtegesDto buildFondsMembresProteges(Long agenceId) {
        BigDecimal disponible = safeAmount(compteEpargneRepository.sumSoldeDisponibleActifByAgence(agenceId));
        BigDecimal bloque = safeAmount(compteEpargneRepository.sumSoldeBloqueActifByAgence(agenceId));
        BigDecimal retraitsValides = safeAmount(demandeRetraitEpargneRepository.sumRetraitsValidesNonPayesByAgence(agenceId));
        BigDecimal retraitsNonInclus = retraitsValides.subtract(disponible).max(BigDecimal.ZERO);
        return FondsMembresProtegesDto.builder()
                .epargneDisponibleMembres(disponible)
                .epargneBloqueeGaranties(bloque)
            .retraitsEpargneValidesNonPayesNonInclus(retraitsNonInclus)
            .totalFondsMembres(disponible.add(bloque).add(retraitsNonInclus))
            .commentairePedagogique("Les fonds membres ne sont pas récupérables par le propriétaire. Les retraits validés non payés ne sont ajoutés ici que si leur montant n'est pas déjà couvert par l'épargne disponible.")
                .build();
    }

    private MasseSalarialeDto buildMasseSalariale(LocalDate dateDebut, LocalDate dateFin, Long agenceId, BigDecimal beneficeNetEstime) {
        YearMonth periode = YearMonth.from(dateDebut);
        String periodePaie = periode.toString();
        List<Employe> employesActifs = agenceId != null
                ? employeRepository.findByAgenceIdAndActifTrue(agenceId)
                : employeRepository.findByActifTrue();
        List<DepenseCaisse> salairesPayes = depenseCaisseRepository.findSalaryPaymentsForPayrollPeriod(
                periodePaie,
                dateDebut.atStartOfDay(),
                dateFin.atTime(LocalTime.MAX),
                agenceId
        );

        Map<Long, List<DepenseCaisse>> paiementsParEmploye = salairesPayes.stream()
                .filter(depense -> depense.getEmploye() != null && depense.getEmploye().getId() != null)
                .collect(Collectors.groupingBy(depense -> depense.getEmploye().getId(), LinkedHashMap::new, Collectors.toList()));

        List<DetailMasseSalarialeDto> details = new ArrayList<>();
        BigDecimal massePrevue = BigDecimal.ZERO;
        BigDecimal totalPaye = BigDecimal.ZERO;

        for (Employe employe : employesActifs) {
            BigDecimal salaireBase = safeAmount(employe.getSalaireBase());
            BigDecimal primeFixe = safeAmount(employe.getPrimeFixe());
            BigDecimal bonusVariable = safeAmount(employe.getBonusVariable());
            BigDecimal remunerationFixe = salaireFixePrevu(employe);
            AgentTerrainPrimeBreakdown primesAgentTerrain = buildAgentTerrainPrimeBreakdown(employe, dateDebut, dateFin);
            List<DepenseCaisse> paiements = paiementsParEmploye.getOrDefault(employe.getId(), List.of());
            BigDecimal montantPaye = paiements.stream()
                    .map(DepenseCaisse::getMontant)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            PayrollBreakdown breakdown = buildPayrollBreakdown(paiements, remunerationFixe);
            BigDecimal primesBonusJustifies = primesAgentTerrain.totalPrimesAgentTerrain()
                    .add(resolveComplementaryPrimesBonusJustifies(paiements, breakdown));
            BigDecimal remunerationReferenceStockee = paiements.stream()
                    .map(this::resolveRemunerationReferenceFromPayment)
                    .reduce(BigDecimal.ZERO, BigDecimal::max);
            BigDecimal remunerationAttendue = remunerationFixe
                    .add(primesBonusJustifies)
                    .max(remunerationReferenceStockee)
                    .max(remunerationFixe);
            BigDecimal ecartRemuneration = montantPaye.subtract(remunerationAttendue);
            BigDecimal reste = remunerationAttendue.subtract(montantPaye).max(BigDecimal.ZERO);
            String statutPaie = resolveStatutPaie(remunerationAttendue, montantPaye, breakdown, paiements);
            if ("PAYE".equals(statutPaie) && primesBonusJustifies.compareTo(BigDecimal.ZERO) > 0) {
                statutPaie = "PAYE_AVEC_PRIME_BONUS";
            }
            String motifRemuneration = resolveMotifRemuneration(paiements);
            String references = paiements.stream()
                    .map(depense -> depense.getOperationCaisse() != null && depense.getOperationCaisse().getNumeroPiece() != null
                            ? depense.getOperationCaisse().getNumeroPiece()
                            : "DEPENSE-" + depense.getId())
                    .collect(Collectors.joining(", "));
                Agence agenceEmploye = resolveEmployeAgence(employe);
                boolean chargeSiege = isSiegePoste(employe.getFonction()) || agenceEmploye == null;

            massePrevue = massePrevue.add(remunerationAttendue);
            totalPaye = totalPaye.add(montantPaye);
            details.add(DetailMasseSalarialeDto.builder()
                    .employeId(employe.getId())
                    .matricule(employe.getMatricule())
                    .nomComplet(employe.getNomComplet())
                    .poste(employe.getFonction() != null ? employe.getFonction().name() : null)
                    .agenceId(chargeSiege ? null : agenceEmploye.getId())
                    .agenceNom(chargeSiege ? "Siège / Global" : firstNonBlank(agenceEmploye.getNomAgence(), agenceEmploye.getCodeAgence(), "Antenne " + agenceEmploye.getId()))
                    .chargeSiege(chargeSiege)
                    .salaireBase(salaireBase)
                    .primeFixe(primeFixe)
                    .bonusVariable(bonusVariable)
                    .totalEpargneCollecteeValidee(primesAgentTerrain.totalEpargneCollecteeValidee())
                    .primeEpargne(primesAgentTerrain.primeEpargne())
                    .totalRemboursementCollecteValide(primesAgentTerrain.totalRemboursementCollecteValide())
                    .primeRemboursement(primesAgentTerrain.primeRemboursement())
                    .nombreCarnetsVendus(primesAgentTerrain.nombreCarnetsVendus())
                    .bonusCarnets(primesAgentTerrain.bonusCarnets())
                    .totalPrimesAgentTerrain(primesAgentTerrain.totalPrimesAgentTerrain())
                    .primesBonusJustifies(primesBonusJustifies)
                    .remunerationAttendueTotale(remunerationAttendue)
                    .ecartRemuneration(ecartRemuneration)
                    .motifRemuneration(motifRemuneration)
                    .salairePrevu(remunerationAttendue)
                    .montantPaye(montantPaye)
                    .salairesPartielsPayes(breakdown.salairesPartielsPayes())
                    .avancesPayees(breakdown.avancesPayees())
                    .retenues(breakdown.retenues())
                    .primes(breakdown.primes())
                    .commissions(breakdown.commissions())
                    .regularisations(breakdown.regularisations())
                    .resteAPayer(reste)
                    .statutPaie(statutPaie)
                    .referenceDepenseCaisse(references.isBlank() ? null : references)
                    .build());
        }

        BigDecimal resteGlobal = massePrevue.subtract(totalPaye).max(BigDecimal.ZERO);
        boolean surpayeGlobal = totalPaye.compareTo(massePrevue) > 0;

        return MasseSalarialeDto.builder()
                .masseSalarialeMensuellePrevue(massePrevue)
                .salairesPayes(totalPaye)
                .salairesRestantAPayer(resteGlobal)
                .resultatPrevisionnelApresSalairesAPayer(safeAmount(beneficeNetEstime).subtract(resteGlobal))
                .nombreEmployesActifs(employesActifs.size())
                .periodePaie(periodePaie)
                .paiementSuperieurAuPrevu(surpayeGlobal)
                .alerte(surpayeGlobal ? "Les salaires payés dépassent la masse salariale mensuelle prévue." : null)
                .detailsEmployes(details)
                .build();
    }

    private BigDecimal salaireFixePrevu(Employe employe) {
        return safeAmount(employe.getSalaireBase())
                .add(safeAmount(employe.getPrimeFixe()))
                .add(safeAmount(employe.getBonusVariable()));
    }

    private AgentTerrainPrimeBreakdown buildAgentTerrainPrimeBreakdown(Employe employe, LocalDate dateDebut, LocalDate dateFin) {
        if (employe == null || employe.getFonction() != PosteEmploye.AGENT_TERRAIN
            || employe.getUtilisateur() == null || employe.getUtilisateur().getId() == null) {
            return AgentTerrainPrimeBreakdown.empty();
    }

        return agentTerrainRepository.findByUtilisateurId(employe.getUtilisateur().getId())
            .map(agent -> {
                BigDecimal totalEpargne = scaleCurrency(collecteMembreLigneRepository.sumValidatedAmountByAgentAndTypeAndPeriod(
                    agent.getId(),
                    TypeLigneCollecte.EPARGNE,
                    dateDebut,
                    dateFin
                ));
                BigDecimal totalRemboursement = scaleCurrency(collecteMembreLigneRepository.sumValidatedAmountByAgentAndTypeAndPeriod(
                    agent.getId(),
                    TypeLigneCollecte.REMBOURSEMENT_CREDIT,
                    dateDebut,
                    dateFin
                ));
                Long carnets = collecteMembreLigneRepository.sumValidatedCarnetsByAgentAndPeriod(agent.getId(), dateDebut, dateFin);
                int nombreCarnets = carnets != null ? Math.toIntExact(carnets) : 0;
                BigDecimal primeEpargne = scaleCurrency(totalEpargne.multiply(PaiePersonnelConstants.TAUX_PRIME_EPARGNE_AGENT_TERRAIN));
                BigDecimal primeRemboursement = scaleCurrency(totalRemboursement.multiply(PaiePersonnelConstants.TAUX_PRIME_REMBOURSEMENT_AGENT_TERRAIN));
                BigDecimal bonusCarnets = scaleCurrency(PaiePersonnelConstants.BONUS_CARNET_AGENT_TERRAIN.multiply(BigDecimal.valueOf(nombreCarnets)));
                return new AgentTerrainPrimeBreakdown(
                    totalEpargne,
                    primeEpargne,
                    totalRemboursement,
                    primeRemboursement,
                    nombreCarnets,
                    bonusCarnets,
                    primeEpargne.add(primeRemboursement).add(bonusCarnets)
                );
            })
            .orElseGet(AgentTerrainPrimeBreakdown::empty);
        }

    private TransportFixePrevuDto buildTransportFixePrevu(LocalDate dateDebut, LocalDate dateFin, Long agenceId) {
        YearMonth periode = YearMonth.from(dateDebut);
        String periodeCharge = periode.toString();
        LocalDate referenceDate = periode.atEndOfMonth();
        Map<Long, TransportSiteParametre> parametresParSite = transportSiteParametreRepository.findActifsAtDate(referenceDate).stream()
                .filter(parametre -> parametre.getSite() != null && parametre.getSite().getId() != null)
                .filter(parametre -> agenceId == null || (parametre.getSite().getAgence() != null && Objects.equals(parametre.getSite().getAgence().getId(), agenceId)))
                .collect(Collectors.toMap(parametre -> parametre.getSite().getId(), Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        List<DepenseCaisse> transportsPayes = depenseCaisseRepository.findTransportPaymentsForPeriod(
                periodeCharge,
                dateDebut.atStartOfDay(),
                dateFin.atTime(LocalTime.MAX),
                agenceId
        );
        Map<Long, List<DepenseCaisse>> paiementsParEmploye = transportsPayes.stream()
                .filter(depense -> depense.getEmploye() != null && depense.getEmploye().getId() != null)
                .collect(Collectors.groupingBy(depense -> depense.getEmploye().getId(), LinkedHashMap::new, Collectors.toList()));

        List<TransportFixeDetailDto> details = new ArrayList<>();
        BigDecimal totalPrevu = BigDecimal.ZERO;
        BigDecimal totalPaye = BigDecimal.ZERO;
        int nombreJoursPeriode = Math.toIntExact(ChronoUnit.DAYS.between(dateDebut, dateFin) + 1);
        Set<Long> sitesSansMontant = new java.util.LinkedHashSet<>();
        Map<Long, SiteTransportAccumulator> sites = new LinkedHashMap<>();
        for (Employe employe : employeRepository.findByFonctionAndActifTrue(PosteEmploye.AGENT_TERRAIN)) {
            Site site = employe.getSite();
            if (site == null || site.getId() == null) {
                continue;
            }
            if (agenceId != null && (site.getAgence() == null || !Objects.equals(site.getAgence().getId(), agenceId))) {
                continue;
            }
            TransportSiteParametre parametre = parametresParSite.get(site.getId());
            BigDecimal montantJournalier = parametre != null ? safeAmount(parametre.getMontantTransportJournalierParAgent()) : BigDecimal.ZERO;
            BigDecimal montantPrevu = montantJournalier.multiply(BigDecimal.valueOf(nombreJoursPeriode));
            if (parametre == null) {
                sitesSansMontant.add(site.getId());
            }
            List<DepenseCaisse> paiements = paiementsParEmploye.getOrDefault(employe.getId(), List.of());
            BigDecimal montantPaye = paiements.stream()
                    .map(DepenseCaisse::getMontant)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal reste = montantPrevu.subtract(montantPaye).max(BigDecimal.ZERO);
            String references = paiements.stream()
                    .map(depense -> depense.getOperationCaisse() != null && depense.getOperationCaisse().getNumeroPiece() != null
                            ? depense.getOperationCaisse().getNumeroPiece()
                            : "DEPENSE-" + depense.getId())
                    .collect(Collectors.joining(", "));
            totalPrevu = totalPrevu.add(montantPrevu);
            totalPaye = totalPaye.add(montantPaye);
                sites.computeIfAbsent(site.getId(), ignored -> new SiteTransportAccumulator(site, montantJournalier, nombreJoursPeriode))
                    .addAgent(montantPrevu, montantPaye);
            details.add(TransportFixeDetailDto.builder()
                    .employeId(employe.getId())
                    .matricule(employe.getMatricule())
                    .nomComplet(employe.getNomComplet())
                    .siteId(site.getId())
                    .siteNom(site.getNomSite())
                    .agenceId(site.getAgence() != null ? site.getAgence().getId() : null)
                    .agenceNom(site.getAgence() != null ? firstNonBlank(site.getAgence().getNomAgence(), site.getAgence().getCodeAgence(), "Antenne " + site.getAgence().getId()) : null)
                    .montantJournalierParAgent(montantJournalier)
                    .nombreJoursPeriode(nombreJoursPeriode)
                    .montantPrevu(montantPrevu)
                    .montantPaye(montantPaye)
                    .resteAPayer(reste)
                    .statut(resolveStatutTransport(montantPrevu, montantPaye, parametre != null))
                    .referencesPaiement(references.isBlank() ? null : references)
                    .build());
        }
        return TransportFixePrevuDto.builder()
                .totalTransportPrevu(totalPrevu)
                .totalTransportPaye(totalPaye)
                .totalTransportRestant(totalPrevu.subtract(totalPaye).max(BigDecimal.ZERO))
                .nombreAgentsTerrain(details.size())
                .nombreSitesConfigures(parametresParSite.size())
                .nombreSitesSansMontant(sitesSansMontant.size())
                .nombreJoursPeriode(nombreJoursPeriode)
                .periodeCharge(periodeCharge)
                .commentaireCalcul("Calcul basé sur les jours calendaires de la période : montant journalier par Agent Terrain × nombre d'Agents Terrain actifs affectés au site × nombre de jours.")
                .detailsParSite(sites.values().stream().map(SiteTransportAccumulator::toDto).toList())
                .details(details)
                .build();
    }

    private String resolveStatutTransport(BigDecimal montantPrevu, BigDecimal montantPaye, boolean configured) {
        BigDecimal prevu = safeAmount(montantPrevu);
        BigDecimal paye = safeAmount(montantPaye);
        if (!configured) {
            return paye.compareTo(BigDecimal.ZERO) > 0 ? "A_VERIFIER" : "NON_CONFIGURE";
        }
        if (paye.compareTo(prevu) > 0) {
            return "SURPAYE";
        }
        if (paye.compareTo(BigDecimal.ZERO) <= 0) {
            return "NON_PAYE";
        }
        if (paye.compareTo(prevu) < 0) {
            return "PARTIEL";
        }
        return "PAYE";
    }

    private PayrollBreakdown buildPayrollBreakdown(List<DepenseCaisse> paiements, BigDecimal salairePrevu) {
        BigDecimal salairesPartiels = BigDecimal.ZERO;
        BigDecimal avances = BigDecimal.ZERO;
        BigDecimal retenues = BigDecimal.ZERO;
        BigDecimal primes = BigDecimal.ZERO;
        BigDecimal commissions = BigDecimal.ZERO;
        BigDecimal regularisations = BigDecimal.ZERO;
        BigDecimal salaireComplet = BigDecimal.ZERO;

        for (DepenseCaisse paiement : paiements) {
            TypePaiementPersonnel type = normalizeTypePaiementPaie(paiement.getTypePaiementPersonnel());
            BigDecimal montant = safeAmount(paiement.getMontant());
            if (type == TypePaiementPersonnel.SALAIRE_COMPLET) {
                salaireComplet = salaireComplet.add(montant);
            } else if (type == TypePaiementPersonnel.SALAIRE_PARTIEL) {
                salairesPartiels = salairesPartiels.add(montant);
            } else if (type == TypePaiementPersonnel.AVANCE_SALAIRE) {
                avances = avances.add(montant);
            } else if (type == TypePaiementPersonnel.RETENUE_SALAIRE) {
                retenues = retenues.add(safeAmount(paiement.getMontantRetenue()).compareTo(BigDecimal.ZERO) > 0
                        ? safeAmount(paiement.getMontantRetenue())
                        : safeAmount(salairePrevu).subtract(montant).max(BigDecimal.ZERO));
                salairesPartiels = salairesPartiels.add(montant);
            } else if (type == TypePaiementPersonnel.PRIME) {
                primes = primes.add(montant);
            } else if (type == TypePaiementPersonnel.COMMISSION) {
                commissions = commissions.add(montant);
            } else if (type == TypePaiementPersonnel.REGULARISATION || type == TypePaiementPersonnel.AUTRE) {
                regularisations = regularisations.add(montant);
            }
        }
        BigDecimal reductionSalaire = salaireComplet.add(salairesPartiels).add(avances).add(retenues);
        return new PayrollBreakdown(salaireComplet, salairesPartiels, avances, retenues, primes, commissions, regularisations, reductionSalaire);
    }

    private TypePaiementPersonnel normalizeTypePaiementPaie(TypePaiementPersonnel type) {
        if (type == null || type == TypePaiementPersonnel.SALAIRE) {
            return TypePaiementPersonnel.SALAIRE_COMPLET;
        }
        if (type == TypePaiementPersonnel.AVANCE) {
            return TypePaiementPersonnel.AVANCE_SALAIRE;
        }
        return type;
    }

    private String resolveStatutPaie(BigDecimal remunerationAttendue, BigDecimal montantPaye, PayrollBreakdown breakdown, List<DepenseCaisse> paiements) {
        BigDecimal prevu = safeAmount(remunerationAttendue);
        BigDecimal paye = safeAmount(montantPaye);
        if (paiements.stream().anyMatch(depense -> depense.getTypePaiementPersonnel() == null && depense.getEmploye() != null)) {
            return "A_VERIFIER";
        }
        if (paye.compareTo(prevu) > 0) {
            return "SURPAYE";
        }
        if (paye.compareTo(BigDecimal.ZERO) <= 0) {
            return "NON_PAYE";
        }
        if (breakdown.avancesPayees().compareTo(BigDecimal.ZERO) > 0 && paye.compareTo(prevu) < 0) {
            return "AVANCE_A_REGULARISER";
        }
        if (breakdown.retenues().compareTo(BigDecimal.ZERO) > 0 && paye.compareTo(prevu) >= 0) {
            return "PAYE_AVEC_RETENUE";
        }
        if (paye.compareTo(prevu) < 0) {
            return "PARTIEL";
        }
        if (safeAmount(detailPrimesEtBonus(breakdown)).compareTo(BigDecimal.ZERO) > 0) {
            return "PAYE_AVEC_PRIME_BONUS";
        }
        return "PAYE";
    }

        private BigDecimal resolveComplementaryPrimesBonusJustifies(List<DepenseCaisse> paiements, PayrollBreakdown breakdown) {
        BigDecimal primeMotivationManuelle = paiements.stream()
            .map(depense -> safeAmount(depense.getPrimeMotivationManuelle()))
            .reduce(BigDecimal.ZERO, BigDecimal::max);
        return primeMotivationManuelle.add(detailPrimesEtBonus(breakdown));
    }

    private BigDecimal detailPrimesEtBonus(PayrollBreakdown breakdown) {
        if (breakdown == null) {
            return BigDecimal.ZERO;
        }
        return safeAmount(breakdown.primes())
                .add(safeAmount(breakdown.commissions()))
                .add(safeAmount(breakdown.regularisations()));
    }

    private BigDecimal resolveRemunerationReferenceFromPayment(DepenseCaisse depense) {
        return safeAmount(depense.getMontantRemunerationReference())
                .max(safeAmount(depense.getMontantSalaireDu()))
                .max(safeAmount(depense.getEmploye() != null ? depense.getEmploye().getTotalRemuneration() : null));
    }

    private BigDecimal scaleCurrency(BigDecimal value) {
        return safeAmount(value).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String resolveMotifRemuneration(List<DepenseCaisse> paiements) {
        return paiements.stream()
                .map(depense -> firstNonBlank(
                        depense.getMotifPrimeMotivationManuelle(),
                        depense.getMotifEcartRemuneration(),
                        depense.getMotifPaiementPartiel(),
                        depense.getMotifRetenue(),
                        depense.getCommentairePaie(),
                        depense.getMotif(),
                        depense.getCommentaireValidation()))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(" | "));
    }

    private List<ControleCoherenceDto> buildPaieControls(LocalDate dateDebut, LocalDate dateFin, Long agenceId, MasseSalarialeDto masseSalariale) {
        List<ControleCoherenceDto> controles = new ArrayList<>();
        String periodePaie = masseSalariale.getPeriodePaie();
        for (DetailMasseSalarialeDto detail : masseSalariale.getDetailsEmployes()) {
            AntenneInfo antenne = new AntenneInfo(agenceId, agenceId != null ? "Antenne " + agenceId : "Toutes accessibles");
            if (safeAmount(detail.getSalairePrevu()).compareTo(BigDecimal.ZERO) <= 0) {
                addControle(controles, "ROUGE", "MASSE_SALARIALE", detail.getMatricule(), "Employé actif sans salaire fixe prévu exploitable.", antenne);
            }
            if ("PARTIEL".equals(detail.getStatutPaie())) {
                addControle(controles, "ORANGE", "MASSE_SALARIALE", detail.getMatricule(), "Salaire payé inférieur à la rémunération prévue totale. " + buildPaieControlDetail(detail, periodePaie), antenne);
            }
            if ("AVANCE_A_REGULARISER".equals(detail.getStatutPaie())) {
                addControle(controles, "ORANGE", "MASSE_SALARIALE", detail.getMatricule(), "Avance salaire à régulariser pour la période " + periodePaie + ".", antenne);
            }
            if ("PAYE_AVEC_RETENUE".equals(detail.getStatutPaie())) {
                addControle(controles, "ORANGE", "MASSE_SALARIALE", detail.getMatricule(), "Retenue salaire appliquée. " + buildPaieControlDetail(detail, periodePaie), antenne);
            }
            if ("PAYE_AVEC_PRIME_BONUS".equals(detail.getStatutPaie())) {
                addControle(controles, "INFO", "MASSE_SALARIALE", detail.getMatricule(), "Paiement supérieur au salaire de base, justifié par prime/bonus. " + buildPaieControlDetail(detail, periodePaie), antenne);
            }
            if ("SURPAYE".equals(detail.getStatutPaie())) {
                addControle(controles, "ROUGE", "MASSE_SALARIALE", detail.getMatricule(), "Salaire payé supérieur à la rémunération prévue totale. " + buildPaieControlDetail(detail, periodePaie), antenne);
            }
            if ("A_VERIFIER".equals(detail.getStatutPaie())) {
                addControle(controles, "ROUGE", "MASSE_SALARIALE", detail.getMatricule(), "Paiement salaire rattaché sans qualification paie explicite.", antenne);
            }
        }

        List<DepenseCaisse> salairesPayes = depenseCaisseRepository.findSalaryPaymentsForPayrollPeriod(
                periodePaie,
                dateDebut.atStartOfDay(),
                dateFin.atTime(LocalTime.MAX),
                agenceId
        );
        for (DepenseCaisse depense : salairesPayes) {
            AntenneInfo antenne = resolveAntenne(depense);
            String reference = "DEPENSE-" + depense.getId();
            if (depense.getEmploye() == null) {
                addControle(controles, "ROUGE", "MASSE_SALARIALE", reference, "Salaire payé sans employé lié.", antenne);
            }
            if (depense.getPeriodePaie() == null || depense.getPeriodePaie().isBlank()) {
                addControle(controles, "ORANGE", "MASSE_SALARIALE", reference, "Dépense salaire payée sans périodePaie ; rapprochement effectué par date de paiement.", antenne);
            }
        }
        Map<String, Long> salairesCompletsParEmployePeriode = salairesPayes.stream()
                .filter(depense -> depense.getEmploye() != null && depense.getEmploye().getId() != null)
            .filter(depense -> normalizeTypePaiementPaie(depense.getTypePaiementPersonnel()) == TypePaiementPersonnel.SALAIRE_COMPLET)
                .collect(Collectors.groupingBy(depense -> depense.getEmploye().getId() + "|" + firstNonBlank(depense.getPeriodePaie(), periodePaie), Collectors.counting()));
        salairesCompletsParEmployePeriode.forEach((key, count) -> {
            if (count > 1) {
            addControle(controles, "ROUGE", "MASSE_SALARIALE", key, "Double salaire complet détecté pour le même employé et la même période.", new AntenneInfo(agenceId, agenceId != null ? "Antenne " + agenceId : "Toutes accessibles"));
            }
        });

        return controles;
    }

    private String buildPaieControlDetail(DetailMasseSalarialeDto detail, String periodePaie) {
        BigDecimal salaireBase = safeAmount(detail.getSalaireBase());
        BigDecimal primeFixe = safeAmount(detail.getPrimeFixe());
        BigDecimal bonusVariable = safeAmount(detail.getBonusVariable());
        BigDecimal primesBonus = safeAmount(detail.getPrimesBonusJustifies());
        BigDecimal attendu = safeAmount(detail.getRemunerationAttendueTotale());
        BigDecimal paye = safeAmount(detail.getMontantPaye());
        BigDecimal ecart = paye.subtract(attendu);
        String motif = firstNonBlank(detail.getMotifRemuneration(), "N/A");
        return "Employé=" + firstNonBlank(detail.getNomComplet(), detail.getMatricule(), "N/A")
                + ", période=" + firstNonBlank(periodePaie, "N/A")
                + ", salaire base=" + salaireBase
                + ", prime fixe=" + primeFixe
                + ", bonus fixe/variable=" + bonusVariable
                + ", primes/bonus validés=" + primesBonus
                + ", rémunération attendue totale=" + attendu
                + ", total payé=" + paye
                + ", écart=" + ecart
                + ", motif=" + motif
                + ".";
    }

    private List<ControleCoherenceDto> buildTransportControls(Long agenceId, TransportFixePrevuDto transportFixePrevu) {
        List<ControleCoherenceDto> controles = new ArrayList<>();
        AntenneInfo antenne = new AntenneInfo(agenceId, agenceId != null ? "Antenne " + agenceId : "Toutes accessibles");
        if (transportFixePrevu == null) {
            return controles;
        }
        for (Employe employe : employeRepository.findByFonctionAndActifTrue(PosteEmploye.AGENT_TERRAIN)) {
            if (employe.getSite() == null) {
                addControle(controles, "ROUGE", "TRANSPORT_TERRAIN", employe.getMatricule(), "Agent Terrain sans site : transport non calculable.", antenne);
            }
        }
        for (TransportFixeDetailDto detail : transportFixePrevu.getDetails()) {
            if ("NON_CONFIGURE".equals(detail.getStatut())) {
                addControle(controles, "ORANGE", "TRANSPORT_TERRAIN", detail.getMatricule(), "Transport Agent Terrain non configuré pour le site : " + firstNonBlank(detail.getSiteNom(), "N/A") + ".", antenne);
            }
            if ("PARTIEL".equals(detail.getStatut())) {
                addControle(controles, "ORANGE", "TRANSPORT_TERRAIN", detail.getMatricule(), "Transport payé inférieur au transport prévu sur la période " + transportFixePrevu.getPeriodeCharge() + ".", antenne);
            }
            if ("SURPAYE".equals(detail.getStatut())) {
                addControle(controles, "ROUGE", "TRANSPORT_TERRAIN", detail.getMatricule(), "Transport payé supérieur au transport prévu sur la période " + transportFixePrevu.getPeriodeCharge() + ".", antenne);
            }
            if ("A_VERIFIER".equals(detail.getStatut())) {
                addControle(controles, "ROUGE", "TRANSPORT_TERRAIN", detail.getMatricule(), "Montant transport terrain à vérifier pour la période " + transportFixePrevu.getPeriodeCharge() + ".", antenne);
            }
        }
        for (DepenseCaisse depense : depenseCaisseRepository.findTransportPaymentsForPeriod(
                transportFixePrevu.getPeriodeCharge(),
                YearMonth.parse(transportFixePrevu.getPeriodeCharge()).atDay(1).atStartOfDay(),
                YearMonth.parse(transportFixePrevu.getPeriodeCharge()).atEndOfMonth().atTime(LocalTime.MAX),
                agenceId
        )) {
            AntenneInfo depenseAntenne = resolveAntenne(depense);
            String reference = "DEPENSE-" + depense.getId();
            if (depense.getEmploye() == null) {
                addControle(controles, "ROUGE", "TRANSPORT_TERRAIN", reference, "Transport payé sans agent lié.", depenseAntenne);
            } else if (depense.getEmploye().getFonction() != PosteEmploye.AGENT_TERRAIN) {
                addControle(controles, "ROUGE", "TRANSPORT_TERRAIN", reference, "Transport payé pour un employé non Agent Terrain.", depenseAntenne);
            }
            if (depense.getPeriodeCharge() == null || depense.getPeriodeCharge().isBlank()) {
                addControle(controles, "ORANGE", "TRANSPORT_TERRAIN", reference, "Transport payé sans période de charge ; rapprochement par date de paiement.", depenseAntenne);
            }
            if (depense.getEmploye() != null && depense.getSiteCharge() != null && depense.getEmploye().getSite() != null
                    && !Objects.equals(depense.getSiteCharge().getId(), depense.getEmploye().getSite().getId())) {
                addControle(controles, "ORANGE", "TRANSPORT_TERRAIN", reference, "Transport payé sur un site différent du site d'affectation de l'Agent Terrain.", depenseAntenne);
            }
        }
        return controles;
    }

    private record PayrollBreakdown(
            BigDecimal salairesComplets,
            BigDecimal salairesPartielsPayes,
            BigDecimal avancesPayees,
            BigDecimal retenues,
            BigDecimal primes,
            BigDecimal commissions,
            BigDecimal regularisations,
            BigDecimal reductionSalaire
    ) {}

        private record AgentTerrainPrimeBreakdown(
            BigDecimal totalEpargneCollecteeValidee,
            BigDecimal primeEpargne,
            BigDecimal totalRemboursementCollecteValide,
            BigDecimal primeRemboursement,
            Integer nombreCarnetsVendus,
            BigDecimal bonusCarnets,
            BigDecimal totalPrimesAgentTerrain
        ) {
        private static AgentTerrainPrimeBreakdown empty() {
            return new AgentTerrainPrimeBreakdown(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO
            );
        }
        }

    private class SiteTransportAccumulator {
        private final Site site;
        private final BigDecimal montantJournalierParAgent;
        private final int nombreJoursPeriode;
        private int nombreAgentsTerrainActifs;
        private BigDecimal transportPrevuSite = BigDecimal.ZERO;
        private BigDecimal transportPayeSite = BigDecimal.ZERO;

        SiteTransportAccumulator(Site site, BigDecimal montantJournalierParAgent, int nombreJoursPeriode) {
            this.site = site;
            this.montantJournalierParAgent = montantJournalierParAgent;
            this.nombreJoursPeriode = nombreJoursPeriode;
        }

        void addAgent(BigDecimal prevu, BigDecimal paye) {
            nombreAgentsTerrainActifs++;
            transportPrevuSite = transportPrevuSite.add(prevu);
            transportPayeSite = transportPayeSite.add(paye);
        }

        TransportFixeSiteDetailDto toDto() {
            return TransportFixeSiteDetailDto.builder()
                    .siteId(site != null ? site.getId() : null)
                    .siteNom(site != null ? site.getNomSite() : null)
                    .agenceId(site != null && site.getAgence() != null ? site.getAgence().getId() : null)
                    .agenceNom(site != null && site.getAgence() != null ? firstNonBlank(site.getAgence().getNomAgence(), site.getAgence().getCodeAgence(), "Antenne " + site.getAgence().getId()) : null)
                    .montantJournalierParAgent(montantJournalierParAgent)
                    .nombreAgentsTerrainActifs(nombreAgentsTerrainActifs)
                    .nombreJoursPeriode(nombreJoursPeriode)
                    .transportPrevuSite(transportPrevuSite)
                    .transportPayeSite(transportPayeSite)
                    .transportRestantSite(transportPrevuSite.subtract(transportPayeSite).max(BigDecimal.ZERO))
                    .build();
        }
    }

        private CapaciteRetraitProprietaireDto buildCapaciteRetraitProprietaire(ApportFinancementDto apportsFinancements, TresorerieDisponibleDto tresorerieDisponible, PositionCreditDto positionCredit, BigDecimal beneficeNetEstime) {
        BigDecimal capitalRestant = safeAmount(apportsFinancements.getCapitalInjecteRestantARecuperer());
        BigDecimal tresoreriePrudente = safeAmount(tresorerieDisponible.getTresorerieRecuperablePrudente());
        BigDecimal tresorerieRecuperable = tresoreriePrudente.max(BigDecimal.ZERO);
        BigDecimal montantConseille = capitalRestant.min(tresorerieRecuperable);
        boolean retraitDeconseille = beneficeNetEstime.compareTo(BigDecimal.ZERO) < 0 || montantConseille.compareTo(BigDecimal.ZERO) <= 0;
        String alerte = beneficeNetEstime.compareTo(BigDecimal.ZERO) < 0
                ? "Résultat net négatif : récupération d'apport déconseillée sauf validation exceptionnelle."
                : null;
        String alerteTresorerie = montantConseille.compareTo(safeAmount(tresorerieDisponible.getTresorerieApresProtectionMembres())) > 0
            ? "Alerte critique : le montant conseillé dépasse la trésorerie après protection des fonds membres."
            : null;
        String alerteCredit = safeAmount(positionCredit.getCapitalRestantDehors()).compareTo(BigDecimal.ZERO) > 0
            ? "Prudence : du capital crédit reste dehors et n'est pas encore revenu en caisse."
            : null;

        return CapaciteRetraitProprietaireDto.builder()
                .capitalInjecteCumule(safeAmount(apportsFinancements.getCapitalInjecteARecuperer()))
                .remboursementsApportPayes(safeAmount(apportsFinancements.getRemboursementsApportPayes()))
                .capitalInjecteRestantARecuperer(capitalRestant)
            .soldeCaisseTheoriqueActif(safeAmount(tresorerieDisponible.getSoldeCaisseTheoriqueActif()))
            .fondsMembresAProteger(safeAmount(tresorerieDisponible.getFondsMembresAProteger()))
            .tresorerieApresProtectionMembres(safeAmount(tresorerieDisponible.getTresorerieApresProtectionMembres()))
            .engagementsCourtTerme(safeAmount(tresorerieDisponible.getTotalEngagementsCourtTerme()))
            .salairesRestantAPayer(safeAmount(tresorerieDisponible.getSalairesRestantAPayer()))
            .transportRestantAPayer(safeAmount(tresorerieDisponible.getTransportRestantAPayer()))
            .fondsMinimumSecurite(safeAmount(tresorerieDisponible.getFondsMinimumSecurite()))
            .margePrudence(safeAmount(tresorerieDisponible.getMargePrudence()))
            .tresorerieRecuperablePrudente(tresoreriePrudente)
                .tresoreriePotentiellementRecuperable(tresorerieRecuperable)
                .montantRecuperableConseille(montantConseille)
                .retraitDeconseille(retraitDeconseille)
                .alerte(alerte)
            .alerteTresorerie(alerteTresorerie)
            .alerteCredit(alerteCredit)
            .commentairePedagogique("Montant conseillé = minimum entre capital propriétaire restant à récupérer et trésorerie récupérable prudente positive. Les fonds membres, les salaires et le transport terrain restant à payer doivent être réservés avant remboursement d'apport propriétaire. Cette lecture est une aide de gestion, pas une règle métier 3N officielle.")
                .build();
    }

    private BigDecimal getOptionalDecimalParam(String key) {
        try {
            return parametreMetierService.existsKey(key) ? safeAmount(parametreMetierService.getDecimal(key)) : BigDecimal.ZERO;
        } catch (RuntimeException ignored) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal sumApportsByNature(List<ApportFinancementDetailDto> details, NatureFinancementApprovisionnement nature) {
        String label = natureFinancementLabel(nature);
        return details.stream()
                .filter(detail -> label.equals(detail.getNatureFinancement()))
                .map(ApportFinancementDetailDto::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String natureFinancementLabel(NatureFinancementApprovisionnement nature) {
        if (nature == null) {
            return "Approvisionnement non qualifié";
        }
        return switch (nature) {
            case TRANSFERT_INTERNE -> "Transfert interne";
            case APPORT_PROPRIETAIRE -> "Apport propriétaire / capital injecté";
            case PRET_RECU -> "Prêt reçu par l'institution";
            case REMBOURSEMENT_AVANCE -> "Remboursement d'avance";
            case AUTRE_FINANCEMENT -> "Autre financement";
        };
    }

    private List<MouvementNonRevenuParAntenneDto> buildMouvementsNonRevenusParAntenne(LocalDate dateDebut, LocalDate dateFin, Long agenceId) {
        Map<Long, MouvementAntenneAccumulator> accumulators = new LinkedHashMap<>();

        for (CollecteMembreLigne ligne : collecteMembreLigneRepository.findPositiveLinesByTypeAndPeriod(TypeLigneCollecte.EPARGNE, dateDebut, dateFin, agenceId)) {
            movementAccumulator(accumulators, resolveAntenne(ligne)).epargneCollectee = movementAccumulator(accumulators, resolveAntenne(ligne)).epargneCollectee.add(safeAmount(ligne.getMontant()));
        }

        for (RemboursementCredit remboursement : remboursementCreditRepository.findPrincipalRemboursements(dateDebut.atStartOfDay(), dateFin.atTime(LocalTime.MAX), agenceId)) {
            MouvementAntenneAccumulator accumulator = movementAccumulator(accumulators, resolveAntenne(remboursement));
            accumulator.principalCreditRembourse = accumulator.principalCreditRembourse.add(safeAmount(remboursement.getMontantPrincipal()));
        }

        List<OperationCaisse> operations = operationCaisseRepository.findAccountingMovementOperations(
                dateDebut.atStartOfDay(),
                dateFin.atTime(LocalTime.MAX),
                agenceId,
                List.of(
                        CategorieOperationCaisse.DEPOT_GARANTIE,
                        CategorieOperationCaisse.DEPOT_GARANTIE_CREDIT,
                        CategorieOperationCaisse.APPROVISIONNEMENT,
                        CategorieOperationCaisse.RETRAIT_EPARGNE,
                        CategorieOperationCaisse.DECAISSEMENT_CREDIT,
                        CategorieOperationCaisse.REMBOURSEMENT_APPORT_PROPRIETAIRE,
                        CategorieOperationCaisse.EPARGNE,
                        CategorieOperationCaisse.REMBOURSEMENT_CREDIT
                )
        );
        for (OperationCaisse operation : operations) {
            MouvementAntenneAccumulator accumulator = movementAccumulator(accumulators, resolveAntenne(operation));
            BigDecimal montant = safeAmount(operation.getMontant());
            if (operation.getCategorieOperation() == CategorieOperationCaisse.DEPOT_GARANTIE
                    || operation.getCategorieOperation() == CategorieOperationCaisse.DEPOT_GARANTIE_CREDIT) {
                accumulator.garantiesDepotGarantie = accumulator.garantiesDepotGarantie.add(montant);
            } else if (operation.getCategorieOperation() == CategorieOperationCaisse.APPROVISIONNEMENT) {
                accumulator.approvisionnementsCaisse = accumulator.approvisionnementsCaisse.add(montant);
            } else if (operation.getCategorieOperation() == CategorieOperationCaisse.RETRAIT_EPARGNE) {
                accumulator.retraitsEpargne = accumulator.retraitsEpargne.add(montant);
            } else if (operation.getCategorieOperation() == CategorieOperationCaisse.DECAISSEMENT_CREDIT) {
                accumulator.decaissementsCredit = accumulator.decaissementsCredit.add(montant);
            } else if (operation.getCategorieOperation() != CategorieOperationCaisse.REMBOURSEMENT_CREDIT
                    && operation.getCategorieOperation() != CategorieOperationCaisse.EPARGNE) {
                accumulator.autresMouvementsNonRevenus = accumulator.autresMouvementsNonRevenus.add(montant);
            }
        }

        return accumulators.values().stream().map(MouvementAntenneAccumulator::toDto).toList();
    }

    private MouvementAntenneAccumulator movementAccumulator(Map<Long, MouvementAntenneAccumulator> accumulators, AntenneInfo antenne) {
        Long key = antenne.id() != null ? antenne.id() : -1L;
        return accumulators.computeIfAbsent(key, ignored -> new MouvementAntenneAccumulator(antenne.id(), antenne.nom()));
    }

    private PositionCreditDto buildPositionCredit(LocalDate dateDebut, LocalDate dateFin, Long agenceId) {
        BigDecimal capitalDecaisse = safeAmount(creditRepository.sumCapitalDecaisseByPeriodAndAgence(dateDebut, dateFin, agenceId, StatutCredit.ANNULE));
        BigDecimal principalRecupere = safeAmount(remboursementCreditRepository.sumPrincipalRembourseByPeriodAndAgence(dateDebut.atStartOfDay(), dateFin.atTime(LocalTime.MAX), agenceId));
        BigDecimal interetsEncaisses = safeAmount(remboursementCreditRepository.sumInteretsByPeriodAndAgence(dateDebut.atStartOfDay(), dateFin.atTime(LocalTime.MAX), agenceId));
        BigDecimal penalitesEncaisses = safeAmount(remboursementCreditRepository.sumPenalitesByPeriodAndAgence(dateDebut.atStartOfDay(), dateFin.atTime(LocalTime.MAX), agenceId));
        BigDecimal capitalRestantDehors = capitalDecaisse.subtract(principalRecupere);
        List<StatutCredit> statutsActifs = activeCreditStatuses();

        return PositionCreditDto.builder()
                .capitalDecaisse(capitalDecaisse)
                .principalRecupere(principalRecupere)
                .capitalRestantDehors(capitalRestantDehors)
                .capitalRestantEstime(true)
                .interetsEncaisses(interetsEncaisses)
                .penalitesEncaisses(penalitesEncaisses)
                .nombreCreditsActifs(creditRepository.countByStatutInAndAgence(statutsActifs, agenceId))
                .nombreCreditsRembourses(creditRepository.countRemboursesByPeriodAndAgence(dateDebut.atStartOfDay(), dateFin.atTime(LocalTime.MAX), agenceId, reimbursedCreditStatuses()))
                .commentairePedagogique("Capital restant dehors estimé sur la période : capital décaissé - principal récupéré. Il ne correspond pas au reste à payer total et n'inclut pas les intérêts ni les pénalités.")
                .build();
    }

    private List<StatutCredit> reimbursedCreditStatuses() {
        return List.of(StatutCredit.REMBOURSE);
    }

    private List<StatutCredit> activeCreditStatuses() {
        return List.of(StatutCredit.DECAISSE, StatutCredit.EN_COURS, StatutCredit.EN_RETARD, StatutCredit.CONTENTIEUX);
    }

    private List<ControleCoherenceDto> buildControlesCoherence(LocalDate dateDebut, LocalDate dateFin, Long agenceId, PositionCreditDto positionCredit) {
        List<ControleCoherenceDto> controles = new ArrayList<>();
        addFraisAnalyseControls(controles, dateDebut, dateFin, agenceId);
        addCarnetControls(controles, dateDebut, dateFin, agenceId);
        addPositionCreditControls(controles, dateDebut, dateFin, agenceId, positionCredit);
        return controles;
    }

    private void addPositionCreditControls(List<ControleCoherenceDto> controles, LocalDate dateDebut, LocalDate dateFin, Long agenceId, PositionCreditDto positionCredit) {
        if (positionCredit.getPrincipalRecupere().compareTo(positionCredit.getCapitalDecaisse()) > 0) {
            addControle(controles, "ORANGE", "POSITION_CREDIT", "PERIODE", "Principal récupéré supérieur au capital décaissé sur la période. Cela peut arriver lorsque des crédits décaissés avant la période sont remboursés pendant la période.", new AntenneInfo(agenceId, agenceId != null ? "Antenne " + agenceId : "Toutes accessibles"));
        }
        if (positionCredit.getCapitalRestantDehors().compareTo(BigDecimal.ZERO) < 0) {
            addControle(controles, "ORANGE", "POSITION_CREDIT", "PERIODE", "Capital restant dehors estimé négatif. Le calcul de période est capital décaissé - principal récupéré et peut être négatif si des crédits plus anciens sont remboursés.", new AntenneInfo(agenceId, agenceId != null ? "Antenne " + agenceId : "Toutes accessibles"));
        }

        for (RemboursementCredit remboursement : remboursementCreditRepository.findRemboursementsSansVentilation(dateDebut.atStartOfDay(), dateFin.atTime(LocalTime.MAX), agenceId)) {
            addControle(controles, "ROUGE", "POSITION_CREDIT", remboursement.getNumeroRecu(), "Remboursement crédit sans ventilation principal/intérêt/pénalité malgré un montant total positif.", resolveAntenne(remboursement));
        }
        for (Credit credit : creditRepository.findDecaissesWithoutMontantAccorde(dateDebut, dateFin, agenceId, StatutCredit.ANNULE)) {
            addControle(controles, "ROUGE", "POSITION_CREDIT", credit.getNumeroCredit(), "Crédit décaissé sans montant accordé exploitable.", resolveAntenne(credit));
        }
        for (Credit credit : creditRepository.findActiveCreditsWithInconsistentEncours(activeCreditStatuses(), agenceId)) {
            addControle(controles, "ORANGE", "POSITION_CREDIT", credit.getNumeroCredit(), "Crédit actif avec encours principal incohérent par rapport au principal total.", resolveAntenne(credit));
        }
    }

    private void addFraisAnalyseControls(List<ControleCoherenceDto> controles, LocalDate dateDebut, LocalDate dateFin, Long agenceId) {
        List<DemandeCredit> demandes = demandeCreditRepository.findForRevenueControls(dateDebut, dateFin, agenceId);
        for (DemandeCredit demande : demandes) {
            BigDecimal fraisDemande = safeAmount(demande.getFraisDemande());
            BigDecimal fraisPayes = safeAmount(demande.getFraisDemandePayes());
            if (fraisDemande.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            boolean statutAvance = demande.getStatut() != StatutDemandeCredit.BROUILLON
                    && demande.getStatut() != StatutDemandeCredit.SOUMISE
                    && demande.getStatut() != StatutDemandeCredit.ANNULEE
                    && demande.getStatut() != StatutDemandeCredit.REJETEE;
            if (fraisPayes.compareTo(BigDecimal.ZERO) == 0 && statutAvance) {
                addControle(controles, "ROUGE", "FRAIS_ANALYSE_CREDIT", demande.getNumeroDemande(), "Demande crédit avancée avec frais d'analyse non payés", resolveAntenne(demande));
            } else if (fraisPayes.compareTo(fraisDemande) < 0 && statutAvance) {
                addControle(controles, "ORANGE", "FRAIS_ANALYSE_CREDIT", demande.getNumeroDemande(), "Demande crédit avancée avec frais d'analyse incomplets", resolveAntenne(demande));
            }
        }

        List<OperationCaisse> operationsFrais = operationCaisseRepository.findRevenueOperations(
                dateDebut.atStartOfDay(),
                dateFin.atTime(LocalTime.MAX),
                agenceId,
                List.of(CategorieOperationCaisse.FRAIS_DEMANDE_CREDIT, CategorieOperationCaisse.FRAIS_DEMANDE)
        );
        for (OperationCaisse operation : operationsFrais) {
            if (firstNonBlank(operation.getReferenceMetier(), operation.getReferenceExterne()) == null) {
                addControle(controles, "ORANGE", "FRAIS_ANALYSE_CREDIT", operation.getNumeroPiece(), "Opération caisse frais analyse sans référence métier/externe", resolveAntenne(operation));
            }
        }
    }

    private void addCarnetControls(List<ControleCoherenceDto> controles, LocalDate dateDebut, LocalDate dateFin, Long agenceId) {
        List<CollecteJournaliereTerrain> collectes = collecteJournaliereTerrainRepository.findForCarnetControls(dateDebut, dateFin, agenceId);
        for (CollecteJournaliereTerrain collecte : collectes) {
            long lignesCarnet = collecte.getLignes().stream()
                    .filter(ligne -> ligne.getTypeLigne() == TypeLigneCollecte.CARNET)
                    .count();
            int totalCarnetsCalcule = collecte.getTotalCarnetsCalcule() != null ? collecte.getTotalCarnetsCalcule() : 0;
            if (totalCarnetsCalcule != lignesCarnet) {
                addControle(controles, "ORANGE", "CARNET", "COLLECTE-" + collecte.getId(), "Total carnets calculé différent du nombre de lignes CARNET", resolveAntenne(collecte));
            }
            for (CollecteMembreLigne ligne : collecte.getLignes()) {
                if (ligne.getTypeLigne() == TypeLigneCollecte.CARNET && safeAmount(ligne.getMontant()).compareTo(BigDecimal.ZERO) <= 0) {
                    addControle(controles, "ROUGE", "CARNET", "COLLECTE-" + collecte.getId() + "-LIGNE-" + ligne.getId(), "Ligne collecte CARNET sans montant", resolveAntenne(collecte));
                }
                if (ligne.getTypeLigne() == TypeLigneCollecte.CARNET && firstNonBlank(ligne.getReference()) == null) {
                    addControle(controles, "ORANGE", "CARNET", "COLLECTE-" + collecte.getId() + "-LIGNE-" + ligne.getId(), "Vente carnet sans numéro/référence carnet individuel. La vente est comptée, mais la traçabilité du stock carnet est incomplète.", resolveAntenne(collecte));
                }
            }
        }
    }

    private void addControle(List<ControleCoherenceDto> controles, String severite, String type, String reference, String message, AntenneInfo antenne) {
        controles.add(ControleCoherenceDto.builder()
                .severite(severite)
                .type(type)
                .reference(reference)
                .message(message)
                .antenneId(antenne.id())
                .antenne(antenne.nom())
                .build());
    }

    private CarnetCostResult resolveCarnetCost(int nombreCarnets) {
        String[] keys = {"COUT_ACHAT_CARNET", "COUT_CARNET", "PRIX_ACHAT_CARNET", "COUT_UNITAIRE_CARNET"};
        for (String key : keys) {
            if (parametreMetierService.existsKey(key)) {
                BigDecimal coutUnitaire = safeAmount(parametreMetierService.getDecimal(key));
                if (coutUnitaire.compareTo(BigDecimal.ZERO) > 0) {
                    return new CarnetCostResult(true, coutUnitaire, coutUnitaire.multiply(BigDecimal.valueOf(nombreCarnets)));
                }
            }
        }
        return new CarnetCostResult(false, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private void addIfPositive(Map<String, BigDecimal> target, String label, BigDecimal montant) {
        BigDecimal safe = safeAmount(montant);
        if (safe.compareTo(BigDecimal.ZERO) > 0) {
            target.put(label, safe);
        }
    }

    private BigDecimal sumValues(Map<String, BigDecimal> values) {
        return values.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<RevenuParCategorieDto> toBreakdownList(String categorie, Map<String, BigDecimal> values) {
        return values.entrySet().stream()
                .map(entry -> RevenuParCategorieDto.builder()
                        .categorie(categorie)
                        .sousCategorie(entry.getKey())
                        .montant(entry.getValue())
                        .build())
                .toList();
    }

    private void addAntenne(Map<Long, AntenneAccumulator> parAntenne, AntenneInfo antenne, String categorie, BigDecimal montant) {
        Long key = antenne.id() != null ? antenne.id() : -1L;
        AntenneAccumulator accumulator = parAntenne.computeIfAbsent(key, ignored -> new AntenneAccumulator(antenne.id(), antenne.nom()));
        accumulator.add(categorie, montant);
    }

    private boolean matchesCategory(String filter, String categorie) {
        return filter == null || filter.equals(categorie);
    }

    private boolean matchesSource(String filter, String source) {
        return filter == null || filter.equals(source);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... tokens) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String resolveUtilisateur(OperationCaisse operation) {
        if (operation.getUtilisateur() != null) {
            return operation.getUtilisateur().getNomComplet();
        }
        if (operation.getCreatedBy() != null) {
            return operation.getCreatedBy().getNomComplet();
        }
        return null;
    }

    private String buildCollecteLineObservation(CollecteMembreLigne ligne) {
        String quantite = ligne.getQuantite() != null && ligne.getQuantite() > 0 ? "Quantité: " + ligne.getQuantite() : null;
        return firstNonBlank(ligne.getCommentaire(), quantite);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private record AntenneInfo(Long id, String nom) { }

    private record RevenueClassification(String categorie, String sousCategorie) {
        private String nature() {
            return sousCategorie;
        }
    }

    private record CategoryBreakdownKey(String categorie, String sousCategorie) { }

    private record CarnetCostResult(boolean disponible, BigDecimal coutUnitaire, BigDecimal coutTotal) { }

    private static final class MouvementAntenneAccumulator {
        private final Long antenneId;
        private final String antenneNom;
        private BigDecimal epargneCollectee = BigDecimal.ZERO;
        private BigDecimal principalCreditRembourse = BigDecimal.ZERO;
        private BigDecimal garantiesDepotGarantie = BigDecimal.ZERO;
        private BigDecimal approvisionnementsCaisse = BigDecimal.ZERO;
        private BigDecimal retraitsEpargne = BigDecimal.ZERO;
        private BigDecimal decaissementsCredit = BigDecimal.ZERO;
        private BigDecimal autresMouvementsNonRevenus = BigDecimal.ZERO;

        private MouvementAntenneAccumulator(Long antenneId, String antenneNom) {
            this.antenneId = antenneId;
            this.antenneNom = antenneNom;
        }

        private MouvementNonRevenuParAntenneDto toDto() {
            BigDecimal total = epargneCollectee
                    .add(principalCreditRembourse)
                    .add(garantiesDepotGarantie)
                    .add(approvisionnementsCaisse)
                    .add(retraitsEpargne)
                    .add(decaissementsCredit)
                    .add(autresMouvementsNonRevenus);
            return MouvementNonRevenuParAntenneDto.builder()
                    .antenneId(antenneId)
                    .antenneNom(antenneNom)
                    .epargneCollectee(epargneCollectee)
                    .principalCreditRembourse(principalCreditRembourse)
                    .garantiesDepotGarantie(garantiesDepotGarantie)
                    .approvisionnementsCaisse(approvisionnementsCaisse)
                    .retraitsEpargne(retraitsEpargne)
                    .decaissementsCredit(decaissementsCredit)
                    .autresMouvementsNonRevenus(autresMouvementsNonRevenus)
                    .totalHorsRevenus(total)
                    .build();
        }
    }

    private static final class CarnetAccumulator {
        private int nombreCarnets = 0;
        private BigDecimal montantVentes = BigDecimal.ZERO;
        private final Map<Long, Integer> nombreCarnetsParAgence = new LinkedHashMap<>();

        private void add(CollecteMembreLigne ligne, Long agenceId) {
            int quantite = ligne.getQuantite() != null && ligne.getQuantite() > 0 ? ligne.getQuantite() : 1;
            nombreCarnets += quantite;
            nombreCarnetsParAgence.merge(agenceId, quantite, Integer::sum);
            montantVentes = montantVentes.add(ligne.getMontant() != null ? ligne.getMontant() : BigDecimal.ZERO);
        }
    }

    private static final class AntenneAccumulator {
        private final Long antenneId;
        private final String antenneNom;
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal fraisAnalyseCredit = BigDecimal.ZERO;
        private BigDecimal fraisRetraitEpargne = BigDecimal.ZERO;
        private BigDecimal interetsCredit = BigDecimal.ZERO;
        private BigDecimal penalitesCredit = BigDecimal.ZERO;
        private BigDecimal carnetsVendus = BigDecimal.ZERO;
        private BigDecimal revenusDivers = BigDecimal.ZERO;

        private AntenneAccumulator(Long antenneId, String antenneNom) {
            this.antenneId = antenneId;
            this.antenneNom = antenneNom;
        }

        private void add(String categorie, BigDecimal montant) {
            total = total.add(montant);
            switch (categorie) {
                case CAT_FRAIS_ANALYSE_CREDIT -> fraisAnalyseCredit = fraisAnalyseCredit.add(montant);
                case CAT_FRAIS_RETRAIT_EPARGNE -> fraisRetraitEpargne = fraisRetraitEpargne.add(montant);
                case CAT_INTERETS_CREDIT -> interetsCredit = interetsCredit.add(montant);
                case CAT_PENALITES_CREDIT -> penalitesCredit = penalitesCredit.add(montant);
                case CAT_CARNETS_VENDUS -> carnetsVendus = carnetsVendus.add(montant);
                case CAT_REVENUS_DIVERS -> revenusDivers = revenusDivers.add(montant);
                default -> { }
            }
        }

        private RevenuParAntenneDto toDto() {
            return RevenuParAntenneDto.builder()
                    .antenneId(antenneId)
                    .antenneNom(antenneNom)
                    .totalRevenus(total)
                    .fraisAnalyseCredit(fraisAnalyseCredit)
                    .fraisRetraitEpargne(fraisRetraitEpargne)
                    .interetsCredit(interetsCredit)
                    .penalitesCredit(penalitesCredit)
                    .carnetsVendus(carnetsVendus)
                    .revenusDivers(revenusDivers)
                    .build();
        }
    }
}
