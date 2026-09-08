package com.mini.credit.service.impl;

import com.mini.credit.dto.caisse.rapport.RapportCaisseDepenseLigneDto;
import com.mini.credit.dto.caisse.rapport.RapportCaisseDepensesDto;
import com.mini.credit.dto.caisse.rapport.RapportCaisseEcartLigneDto;
import com.mini.credit.dto.caisse.rapport.RapportCaisseEcartsDto;
import com.mini.credit.dto.caisse.rapport.RapportCaisseJournalierDto;
import com.mini.credit.dto.caisse.rapport.RapportCaissePeriodeDto;
import com.mini.credit.dto.caisse.rapport.RapportCaisseSessionDto;
import com.mini.credit.dto.caisse.rapport.RapportCaisseSyntheseDto;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.DepenseCaisse;
import com.mini.credit.entity.caisse.EcartCaisse;
import com.mini.credit.entity.caisse.OperationCaisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.DepenseCaisseStatus;
import com.mini.credit.enums.StatutEcartCaisse;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.enums.security.AuditSeverity;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.exception.ResourceNotFoundException;
import com.mini.credit.repository.caisse.DepenseCaisseRepository;
import com.mini.credit.repository.caisse.EcartCaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.service.RapportCaisseService;
import com.mini.credit.service.audit.AuditService;
import com.mini.credit.service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RapportCaisseServiceImpl implements RapportCaisseService {

    private final SessionCaisseRepository sessionCaisseRepository;
    private final OperationCaisseRepository operationCaisseRepository;
    private final DepenseCaisseRepository depenseCaisseRepository;
    private final EcartCaisseRepository ecartCaisseRepository;
    private final AuditService auditService;

    @Override
    public RapportCaisseSessionDto getRapportSession(Long sessionId) {
        SessionCaisse session = sessionCaisseRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session caisse introuvable: " + sessionId));
        validateSessionScope(session);

        List<OperationCaisse> operations = operationCaisseRepository.findBySessionCaisseIdOrderByDateOperationDesc(sessionId);
        List<DepenseCaisse> depenses = depenseCaisseRepository.findBySessionCaisseIdOrderByDateDemandeDesc(sessionId);
        BigDecimal totalDepensesPayees = depenses.stream()
                .filter(d -> d.getStatut() == DepenseCaisseStatus.PAYEE)
                .map(DepenseCaisse::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        RapportCaisseSessionDto result = RapportCaisseSessionDto.builder()
                .sessionId(session.getId())
                .caisseId(session.getCaisse() != null ? session.getCaisse().getId() : null)
                .caisseCode(session.getCaisse() != null ? session.getCaisse().getCodeCaisse() : null)
                .caisseLibelle(session.getCaisse() != null ? session.getCaisse().getLibelle() : null)
                .siteId(extractSiteId(session.getCaisse()))
                .siteNom(session.getCaisse() != null && session.getCaisse().getSite() != null ? session.getCaisse().getSite().getNomSite() : null)
                .dateComptable(session.getDateComptable())
                .statut(session.getStatut() != null ? session.getStatut().name() : null)
                .utilisateur(session.getUtilisateur() != null ? session.getUtilisateur().getNomComplet() : null)
                .dateOuverture(session.getDateOuverture())
                .dateCloture(session.getDateCloture())
                .soldeOuverture(session.getSoldeOuverture())
                .totalEntrees(session.getTotalEntrees())
                .totalSorties(session.getTotalSorties())
                .soldeTheorique(session.getSoldeTheorique())
                .soldePhysique(session.getSoldePhysique())
                .ecartCaisse(session.getEcartCaisse())
                .nombreOperations(operations.size())
                .totalDepensesPayees(totalDepensesPayees)
                .build();

            safeLogRapportConsultation(
                AuditModule.RAPPORT_CAISSE,
                "RapportCaisseSession",
                sessionId,
                "Consultation rapport session caisse",
                "SESSION-" + sessionId
            );

            return result;
    }

    @Override
    public RapportCaisseJournalierDto getRapportJournalier(LocalDate date, Long caisseId, Long siteId) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        AggregateData aggregate = computeJournalierAggregate(targetDate, caisseId, siteId);
        RapportCaisseJournalierDto result = RapportCaisseJournalierDto.builder()
                .date(targetDate)
                .nombreSessions(aggregate.sessions.size())
            .nombreOperations(aggregate.operations.size())
                .nombreSessionsNonCloturees(aggregate.nombreSessionsNonCloturees)
                .totalEntrees(aggregate.totalEntrees)
                .totalSorties(aggregate.totalSorties)
                .soldeTheoriqueTotal(aggregate.soldeTheoriqueTotal)
                .soldePhysiqueTotal(aggregate.soldePhysiqueTotal)
                .ecartTotal(aggregate.ecartTotal)
                .nombreDepenses(aggregate.depenses.size())
                .montantDepenses(aggregate.montantDepenses)
                .nombreEcarts(aggregate.ecarts.size())
                .montantEcarts(aggregate.montantEcarts)
                .build();

            safeLogRapportConsultation(
                AuditModule.RAPPORT_CAISSE,
                "RapportCaisseJournalier",
                null,
                "Consultation rapport journalier caisse",
                "JOURNALIER-" + targetDate
            );

            return result;
    }

    @Override
    public RapportCaissePeriodeDto getRapportPeriode(LocalDate dateDebut, LocalDate dateFin, Long caisseId, Long siteId) {
        DateRange range = validateRange(dateDebut, dateFin);
        AggregateData aggregate = computeAggregate(range.dateDebut, range.dateFin, caisseId, siteId);
        RapportCaissePeriodeDto result = RapportCaissePeriodeDto.builder()
                .dateDebut(range.dateDebut)
                .dateFin(range.dateFin)
                .nombreSessions(aggregate.sessions.size())
                .nombreSessionsNonCloturees(aggregate.nombreSessionsNonCloturees)
                .totalEntrees(aggregate.totalEntrees)
                .totalSorties(aggregate.totalSorties)
                .soldeTheoriqueTotal(aggregate.soldeTheoriqueTotal)
                .soldePhysiqueTotal(aggregate.soldePhysiqueTotal)
                .ecartTotal(aggregate.ecartTotal)
                .nombreOperations(aggregate.operations.size())
                .nombreDepenses(aggregate.depenses.size())
                .montantDepenses(aggregate.montantDepenses)
                .nombreEcarts(aggregate.ecarts.size())
                .montantEcarts(aggregate.montantEcarts)
                .build();

            safeLogRapportConsultation(
                AuditModule.RAPPORT_CAISSE,
                "RapportCaissePeriode",
                null,
                "Consultation rapport caisse période",
                "PERIODE-" + range.dateDebut + "-" + range.dateFin
            );

            return result;
    }

    @Override
    public RapportCaisseDepensesDto getRapportDepenses(LocalDate dateDebut, LocalDate dateFin, Long caisseId, Long siteId) {
        DateRange range = validateRange(dateDebut, dateFin);
        List<DepenseCaisse> depenses = filterDepenses(
                depenseCaisseRepository.findByDateDemandeBetweenOrderByDateDemandeDesc(
                        range.dateDebut.atStartOfDay(),
                        range.dateFin.atTime(LocalTime.MAX)
                ),
                caisseId,
                siteId
        );

        BigDecimal total = depenses.stream()
                .map(DepenseCaisse::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RapportCaisseDepenseLigneDto> lignes = depenses.stream().map(d -> RapportCaisseDepenseLigneDto.builder()
                .depenseId(d.getId())
                .sessionId(d.getSessionCaisse() != null ? d.getSessionCaisse().getId() : null)
                .caisseId(d.getCaisse() != null ? d.getCaisse().getId() : null)
                .caisseLibelle(d.getCaisse() != null ? d.getCaisse().getLibelle() : null)
                .siteId(d.getSite() != null ? d.getSite().getId() : null)
                .siteNom(d.getSite() != null ? d.getSite().getNomSite() : null)
                .categorie(d.getCategorie() != null ? d.getCategorie().name() : null)
                .montant(d.getMontant())
                .devise(d.getDevise())
                .statut(d.getStatut() != null ? d.getStatut().name() : null)
                .demandePar(d.getDemandePar() != null ? d.getDemandePar().getNomComplet() : null)
                .validePar(d.getValidePar() != null ? d.getValidePar().getNomComplet() : null)
                .payePar(d.getPayePar() != null ? d.getPayePar().getNomComplet() : null)
                .dateDemande(d.getDateDemande())
                .dateValidation(d.getDateValidation())
                .datePaiement(d.getDatePaiement())
                .motif(d.getMotif())
                .build()).toList();

        RapportCaisseDepensesDto result = RapportCaisseDepensesDto.builder()
                .dateDebut(range.dateDebut)
                .dateFin(range.dateFin)
                .nombreDepenses(depenses.size())
                .montantTotal(total)
                .lignes(lignes)
                .build();

            safeLogRapportConsultation(
                AuditModule.RAPPORT_CAISSE,
                "RapportCaisseDepenses",
                null,
                "Consultation rapport dépenses caisse",
                "DEPENSES-" + range.dateDebut + "-" + range.dateFin
            );

            return result;
    }

    @Override
    public RapportCaisseEcartsDto getRapportEcarts(LocalDate dateDebut, LocalDate dateFin, Long caisseId, Long siteId) {
        DateRange range = validateRange(dateDebut, dateFin);
        List<EcartCaisse> ecarts = filterEcarts(
                ecartCaisseRepository.findByDateJourBetweenOrderByDateJourDesc(range.dateDebut, range.dateFin),
                caisseId,
                siteId
        );

        BigDecimal total = ecarts.stream().map(EcartCaisse::getMontantEcart).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        long ouverts = ecarts.stream().filter(e -> e.getStatut() == StatutEcartCaisse.DETECTE || e.getStatut() == StatutEcartCaisse.EN_INVESTIGATION).count();

        List<RapportCaisseEcartLigneDto> lignes = ecarts.stream().map(e -> {
            SessionCaisse s = e.getSessionCaisse();
            Caisse c = s != null ? s.getCaisse() : null;
            return RapportCaisseEcartLigneDto.builder()
                    .ecartId(e.getId())
                    .sessionId(s != null ? s.getId() : null)
                    .caisseId(c != null ? c.getId() : null)
                    .caisseLibelle(c != null ? c.getLibelle() : null)
                    .siteId(extractSiteId(c))
                    .siteNom(c != null && c.getSite() != null ? c.getSite().getNomSite() : null)
                    .dateJour(e.getDateJour())
                    .typeEcart(e.getTypeEcart() != null ? e.getTypeEcart().name() : null)
                    .statut(e.getStatut() != null ? e.getStatut().name() : null)
                    .montantEcart(e.getMontantEcart())
                    .seuilDepasse(e.getSeuilDepassé())
                    .enquetePar(e.getEnquetePar() != null ? e.getEnquetePar().getNomComplet() : null)
                    .validePar(e.getValidePar() != null ? e.getValidePar().getNomComplet() : null)
                    .dateEnquete(e.getDateEnquete())
                    .dateValidation(e.getDateValidation())
                    .description(e.getDescription())
                    .build();
        }).toList();

        RapportCaisseEcartsDto result = RapportCaisseEcartsDto.builder()
                .dateDebut(range.dateDebut)
                .dateFin(range.dateFin)
                .nombreEcarts(ecarts.size())
                .montantTotal(total)
                .nombreEcartsOuverts(ouverts)
                .lignes(lignes)
                .build();

            safeLogRapportConsultation(
                AuditModule.CONTROLE_INTERNE,
                "RapportCaisseEcarts",
                null,
                "Consultation rapport écarts caisse",
                "ECARTS-" + range.dateDebut + "-" + range.dateFin
            );

            return result;
    }

    @Override
    public RapportCaisseSyntheseDto getRapportSynthese(LocalDate dateDebut, LocalDate dateFin, Long caisseId, Long siteId) {
        DateRange range = validateRange(dateDebut, dateFin);
        AggregateData aggregate = computeAggregate(range.dateDebut, range.dateFin, caisseId, siteId);
        long sessionsOuvertes = aggregate.sessions.stream()
            .filter(s -> s.getStatut() != StatutSessionCaisse.CLOTUREE)
                .count();

        return RapportCaisseSyntheseDto.builder()
                .dateDebut(range.dateDebut)
                .dateFin(range.dateFin)
                .nombreSessions(aggregate.sessions.size())
                .nombreSessionsOuvertes(sessionsOuvertes)
                .totalEntrees(aggregate.totalEntrees)
                .totalSorties(aggregate.totalSorties)
                .ecartTotal(aggregate.ecartTotal)
                .montantDepenses(aggregate.montantDepenses)
                .nombreEcarts(aggregate.ecarts.size())
                .build();
    }

    @Override
    public byte[] exportRapportSessionCsv(Long sessionId) {
        RapportCaisseSessionDto dto = getRapportSession(sessionId);
        String head = "sessionId,caisseId,caisseCode,caisseLibelle,siteId,siteNom,dateComptable,statut,utilisateur,dateOuverture,dateCloture,soldeOuverture,totalEntrees,totalSorties,soldeTheorique,soldePhysique,ecartCaisse,nombreOperations,totalDepensesPayees\n";
        String row = csv(dto.getSessionId()) + "," + csv(dto.getCaisseId()) + "," + csv(dto.getCaisseCode()) + "," + csv(dto.getCaisseLibelle()) + "," +
                csv(dto.getSiteId()) + "," + csv(dto.getSiteNom()) + "," + csv(dto.getDateComptable()) + "," + csv(dto.getStatut()) + "," +
                csv(dto.getUtilisateur()) + "," + csv(dto.getDateOuverture()) + "," + csv(dto.getDateCloture()) + "," + csv(dto.getSoldeOuverture()) + "," +
                csv(dto.getTotalEntrees()) + "," + csv(dto.getTotalSorties()) + "," + csv(dto.getSoldeTheorique()) + "," + csv(dto.getSoldePhysique()) + "," +
                csv(dto.getEcartCaisse()) + "," + csv(dto.getNombreOperations()) + "," + csv(dto.getTotalDepensesPayees()) + "\n";
        logExport("SESSION", sessionId);
        return (head + row).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportRapportJournalierCsv(LocalDate date, Long caisseId, Long siteId) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        AggregateData aggregate = computeJournalierAggregate(targetDate, caisseId, siteId);
        RapportCaisseJournalierDto dto = RapportCaisseJournalierDto.builder()
            .date(targetDate)
            .nombreSessions(aggregate.sessions.size())
            .nombreOperations(aggregate.operations.size())
            .nombreSessionsNonCloturees(aggregate.nombreSessionsNonCloturees)
            .totalEntrees(aggregate.totalEntrees)
            .totalSorties(aggregate.totalSorties)
            .soldeTheoriqueTotal(aggregate.soldeTheoriqueTotal)
            .soldePhysiqueTotal(aggregate.soldePhysiqueTotal)
            .ecartTotal(aggregate.ecartTotal)
            .nombreDepenses(aggregate.depenses.size())
            .montantDepenses(aggregate.montantDepenses)
            .nombreEcarts(aggregate.ecarts.size())
            .montantEcarts(aggregate.montantEcarts)
            .build();

        StringBuilder sb = new StringBuilder();
        sb.append("date,nombreSessions,nombreOperations,nombreSessionsNonCloturees,totalEntrees,totalSorties,soldeTheoriqueTotal,soldePhysiqueTotal,ecartTotal,nombreDepenses,montantDepenses,nombreEcarts,montantEcarts\n");
        sb.append(csv(dto.getDate())).append(',')
            .append(csv(dto.getNombreSessions())).append(',')
            .append(csv(dto.getNombreOperations())).append(',')
            .append(csv(dto.getNombreSessionsNonCloturees())).append(',')
            .append(csv(dto.getTotalEntrees())).append(',')
            .append(csv(dto.getTotalSorties())).append(',')
            .append(csv(dto.getSoldeTheoriqueTotal())).append(',')
            .append(csv(dto.getSoldePhysiqueTotal())).append(',')
            .append(csv(dto.getEcartTotal())).append(',')
            .append(csv(dto.getNombreDepenses())).append(',')
            .append(csv(dto.getMontantDepenses())).append(',')
            .append(csv(dto.getNombreEcarts())).append(',')
            .append(csv(dto.getMontantEcarts())).append('\n');

        sb.append("\n");
        sb.append("Détail des transactions\n");
        sb.append("dateOperation,heureOperation,caisseId,caisseLibelle,sessionId,typeOperation,categorie,source,montant,soldeApresOperation,utilisateurId,utilisateurNom,roleUtilisateur,siteId,siteNom,referenceMetier,commentaire\n");
        for (OperationCaisse op : aggregate.operations) {
            LocalDateTime dt = op.getDateOperation();
            sb.append(csv(dt != null ? dt.toLocalDate() : null)).append(',')
                .append(csv(dt != null ? dt.toLocalTime() : null)).append(',')
                .append(csv(op.getCaisse() != null ? op.getCaisse().getId() : null)).append(',')
                .append(csv(op.getCaisse() != null ? op.getCaisse().getLibelle() : null)).append(',')
                .append(csv(op.getSessionCaisse() != null ? op.getSessionCaisse().getId() : null)).append(',')
                .append(csv(op.getTypeOperation())).append(',')
                .append(csv(op.getCategorieOperation())).append(',')
                .append(csv(op.getSource())).append(',')
                .append(csv(op.getMontant())).append(',')
                .append(csv(op.getSoldeApresOperation())).append(',')
                .append(csv(op.getUtilisateur() != null ? op.getUtilisateur().getId() : null)).append(',')
                .append(csv(op.getUtilisateur() != null ? op.getUtilisateur().getNomComplet() : null)).append(',')
                .append(csv(op.getRoleUtilisateur())).append(',')
                .append(csv(op.getSite() != null ? op.getSite().getId() : extractSiteId(op.getCaisse()))).append(',')
                .append(csv(op.getSite() != null ? op.getSite().getNomSite() : (op.getCaisse() != null && op.getCaisse().getSite() != null ? op.getCaisse().getSite().getNomSite() : null))).append(',')
                .append(csv(op.getReferenceMetier())).append(',')
                .append(csv(op.getCommentaire()))
                .append('\n');
        }

        logExport("JOURNALIER", null);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

        private AggregateData computeJournalierAggregate(LocalDate date, Long caisseId, Long siteId) {
            Utilisateur user = requireCurrentUser();
        LocalDate targetDate = date != null ? date : LocalDate.now();
            Long agenceId = resolveCurrentUserAgenceId(user);

            Page<OperationCaisse> operationPage = operationCaisseRepository.findJournal(
                    null,
                    caisseId,
                    agenceId,
                    siteId,
                    null,
                    null,
                    null,
                null,
                    targetDate.atStartOfDay(),
                    targetDate.atTime(LocalTime.MAX),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Pageable.unpaged()
            );

            List<OperationCaisse> operations = operationPage.getContent();

        Map<Long, SessionCaisse> sessionMap = new LinkedHashMap<>();
        for (OperationCaisse op : operations) {
            SessionCaisse s = op.getSessionCaisse();
            if (s != null && s.getId() != null) {
            sessionMap.putIfAbsent(s.getId(), s);
            }
        }
        List<SessionCaisse> sessions = new ArrayList<>(sessionMap.values());

        List<DepenseCaisse> depenses = operations.stream()
            .filter(op -> op.getTypeOperation() == TypeOperationCaisse.SORTIE)
            .filter(op -> op.getCategorieOperation() == CategorieOperationCaisse.DEPENSE)
            .map(op -> DepenseCaisse.builder().montant(op.getMontant()).build())
            .toList();

        List<EcartCaisse> ecarts = new ArrayList<>();
        for (SessionCaisse session : sessions) {
            if (session.getId() != null) {
            ecarts.addAll(ecartCaisseRepository.findBySessionCaisseIdOrderByDateCreationAsc(session.getId()));
            }
        }

        BigDecimal totalEntrees = operations.stream()
            .filter(op -> op.getTypeOperation() == TypeOperationCaisse.ENTREE)
            .map(OperationCaisse::getMontant)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSorties = operations.stream()
            .filter(op -> op.getTypeOperation() == TypeOperationCaisse.SORTIE)
            .map(OperationCaisse::getMontant)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montantDepenses = depenses.stream()
            .map(DepenseCaisse::getMontant)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, OperationCaisse> lastOperationBySession = new LinkedHashMap<>();
        for (OperationCaisse op : operations) {
            Long sessionId = op.getSessionCaisse() != null ? op.getSessionCaisse().getId() : -1L;
            lastOperationBySession.put(sessionId, op);
        }

        BigDecimal soldeTheoriqueTotal = lastOperationBySession.values().stream()
            .map(OperationCaisse::getSoldeApresOperation)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (soldeTheoriqueTotal.compareTo(BigDecimal.ZERO) == 0) {
            soldeTheoriqueTotal = sessions.stream()
                .map(SessionCaisse::getSoldeTheorique)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal soldePhysiqueTotal = sessions.stream()
            .filter(s -> s.getStatut() == StatutSessionCaisse.CLOTUREE)
            .map(SessionCaisse::getSoldePhysique)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (soldePhysiqueTotal.compareTo(BigDecimal.ZERO) == 0) {
            soldePhysiqueTotal = sessions.stream()
                .map(SessionCaisse::getSoldePhysique)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal ecartTotal = sessions.stream()
            .map(SessionCaisse::getEcartCaisse)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (ecartTotal.compareTo(BigDecimal.ZERO) == 0 && soldePhysiqueTotal.compareTo(BigDecimal.ZERO) != 0) {
            ecartTotal = soldePhysiqueTotal.subtract(soldeTheoriqueTotal);
        }

        BigDecimal montantEcarts = ecarts.stream()
            .map(EcartCaisse::getMontantEcart)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long nonCloturees = sessions.stream()
            .filter(s -> s.getStatut() != StatutSessionCaisse.CLOTUREE)
            .count();

        return new AggregateData(
            sessions,
            operations,
            depenses,
            ecarts,
            nonCloturees,
            totalEntrees,
            totalSorties,
            soldeTheoriqueTotal,
            soldePhysiqueTotal,
            ecartTotal,
            montantDepenses,
            montantEcarts
        );
        }

    private Long resolveCurrentUserAgenceId(Utilisateur currentUser) {
        if (currentUser == null) {
            return null;
        }

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

    @Override
    public byte[] exportRapportPeriodeCsv(LocalDate dateDebut, LocalDate dateFin, Long caisseId, Long siteId) {
        RapportCaissePeriodeDto dto = getRapportPeriode(dateDebut, dateFin, caisseId, siteId);
        String head = "dateDebut,dateFin,nombreSessions,nombreSessionsNonCloturees,totalEntrees,totalSorties,soldeTheoriqueTotal,soldePhysiqueTotal,ecartTotal,nombreOperations,nombreDepenses,montantDepenses,nombreEcarts,montantEcarts\n";
        String row = csv(dto.getDateDebut()) + "," + csv(dto.getDateFin()) + "," + csv(dto.getNombreSessions()) + "," + csv(dto.getNombreSessionsNonCloturees()) + "," +
                csv(dto.getTotalEntrees()) + "," + csv(dto.getTotalSorties()) + "," + csv(dto.getSoldeTheoriqueTotal()) + "," + csv(dto.getSoldePhysiqueTotal()) + "," +
                csv(dto.getEcartTotal()) + "," + csv(dto.getNombreOperations()) + "," + csv(dto.getNombreDepenses()) + "," + csv(dto.getMontantDepenses()) + "," +
                csv(dto.getNombreEcarts()) + "," + csv(dto.getMontantEcarts()) + "\n";
        logExport("PERIODE", null);
        return (head + row).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportRapportDepensesCsv(LocalDate dateDebut, LocalDate dateFin, Long caisseId, Long siteId) {
        RapportCaisseDepensesDto dto = getRapportDepenses(dateDebut, dateFin, caisseId, siteId);
        StringBuilder sb = new StringBuilder();
        sb.append("dateDebut,dateFin,nombreDepenses,montantTotal\n");
        sb.append(csv(dto.getDateDebut())).append(',').append(csv(dto.getDateFin())).append(',')
                .append(csv(dto.getNombreDepenses())).append(',').append(csv(dto.getMontantTotal())).append('\n');
        sb.append("\n");
        sb.append("depenseId,sessionId,caisseId,caisseLibelle,siteId,siteNom,categorie,montant,devise,statut,demandePar,validePar,payePar,dateDemande,dateValidation,datePaiement,motif\n");
        for (RapportCaisseDepenseLigneDto l : dto.getLignes()) {
            sb.append(csv(l.getDepenseId())).append(',').append(csv(l.getSessionId())).append(',').append(csv(l.getCaisseId())).append(',')
                    .append(csv(l.getCaisseLibelle())).append(',').append(csv(l.getSiteId())).append(',').append(csv(l.getSiteNom())).append(',')
                    .append(csv(l.getCategorie())).append(',').append(csv(l.getMontant())).append(',').append(csv(l.getDevise())).append(',')
                    .append(csv(l.getStatut())).append(',').append(csv(l.getDemandePar())).append(',').append(csv(l.getValidePar())).append(',')
                    .append(csv(l.getPayePar())).append(',').append(csv(l.getDateDemande())).append(',').append(csv(l.getDateValidation())).append(',')
                    .append(csv(l.getDatePaiement())).append(',').append(csv(l.getMotif())).append('\n');
        }
        logExport("DEPENSES", null);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportRapportEcartsCsv(LocalDate dateDebut, LocalDate dateFin, Long caisseId, Long siteId) {
        RapportCaisseEcartsDto dto = getRapportEcarts(dateDebut, dateFin, caisseId, siteId);
        StringBuilder sb = new StringBuilder();
        sb.append("dateDebut,dateFin,nombreEcarts,montantTotal,nombreEcartsOuverts\n");
        sb.append(csv(dto.getDateDebut())).append(',').append(csv(dto.getDateFin())).append(',')
                .append(csv(dto.getNombreEcarts())).append(',').append(csv(dto.getMontantTotal())).append(',')
                .append(csv(dto.getNombreEcartsOuverts())).append('\n');
        sb.append("\n");
        sb.append("ecartId,sessionId,caisseId,caisseLibelle,siteId,siteNom,dateJour,typeEcart,statut,montantEcart,seuilDepasse,enquetePar,validePar,dateEnquete,dateValidation,description\n");
        for (RapportCaisseEcartLigneDto l : dto.getLignes()) {
            sb.append(csv(l.getEcartId())).append(',').append(csv(l.getSessionId())).append(',').append(csv(l.getCaisseId())).append(',')
                    .append(csv(l.getCaisseLibelle())).append(',').append(csv(l.getSiteId())).append(',').append(csv(l.getSiteNom())).append(',')
                    .append(csv(l.getDateJour())).append(',').append(csv(l.getTypeEcart())).append(',').append(csv(l.getStatut())).append(',')
                    .append(csv(l.getMontantEcart())).append(',').append(csv(l.getSeuilDepasse())).append(',').append(csv(l.getEnquetePar())).append(',')
                    .append(csv(l.getValidePar())).append(',').append(csv(l.getDateEnquete())).append(',').append(csv(l.getDateValidation())).append(',')
                    .append(csv(l.getDescription())).append('\n');
        }
        logExport("ECARTS", null);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private AggregateData computeAggregate(LocalDate dateDebut, LocalDate dateFin, Long caisseId, Long siteId) {
        List<SessionCaisse> sessions = filterSessions(
                sessionCaisseRepository.findByDateComptableBetweenOrderByDateComptableDesc(dateDebut, dateFin),
                caisseId,
                siteId
        );

        List<OperationCaisse> operations = new ArrayList<>();
        List<DepenseCaisse> depenses = new ArrayList<>();
        List<EcartCaisse> ecarts = new ArrayList<>();

        for (SessionCaisse session : sessions) {
            Long sessionId = session.getId();
            operations.addAll(operationCaisseRepository.findBySessionCaisseIdOrderByDateOperationDesc(sessionId));
            depenses.addAll(depenseCaisseRepository.findBySessionCaisseIdOrderByDateDemandeDesc(sessionId));
            ecarts.addAll(ecartCaisseRepository.findBySessionCaisseIdOrderByDateCreationAsc(sessionId));
        }

        depenses.sort(Comparator.comparing(DepenseCaisse::getDateDemande, Comparator.nullsLast(Comparator.reverseOrder())));
        ecarts.sort(Comparator.comparing(EcartCaisse::getDateJour, Comparator.nullsLast(Comparator.reverseOrder())));

        BigDecimal totalEntrees = sessions.stream().map(SessionCaisse::getTotalEntrees).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSorties = sessions.stream().map(SessionCaisse::getTotalSorties).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal soldeTheoriqueTotal = sessions.stream().map(SessionCaisse::getSoldeTheorique).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal soldePhysiqueTotal = sessions.stream().map(SessionCaisse::getSoldePhysique).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ecartTotal = sessions.stream().map(SessionCaisse::getEcartCaisse).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montantDepenses = depenses.stream().map(DepenseCaisse::getMontant).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montantEcarts = ecarts.stream().map(EcartCaisse::getMontantEcart).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        long nonCloturees = sessions.stream()
            .filter(s -> s.getStatut() != StatutSessionCaisse.CLOTUREE)
                .count();

        return new AggregateData(sessions, operations, depenses, ecarts, nonCloturees,
                totalEntrees, totalSorties, soldeTheoriqueTotal, soldePhysiqueTotal, ecartTotal, montantDepenses, montantEcarts);
    }

    private List<SessionCaisse> filterSessions(List<SessionCaisse> sessions, Long caisseId, Long siteId) {
        Utilisateur user = requireCurrentUser();
        return sessions.stream()
                .filter(s -> s.getCaisse() != null)
                .filter(s -> caisseId == null || caisseId.equals(s.getCaisse().getId()))
                .filter(s -> siteId == null || siteId.equals(extractSiteId(s.getCaisse())))
                .filter(s -> canAccessCaisse(user, s.getCaisse()))
                .toList();
    }

    private List<DepenseCaisse> filterDepenses(List<DepenseCaisse> depenses, Long caisseId, Long siteId) {
        Utilisateur user = requireCurrentUser();
        return depenses.stream()
                .filter(d -> d.getCaisse() != null)
                .filter(d -> caisseId == null || caisseId.equals(d.getCaisse().getId()))
                .filter(d -> siteId == null || siteId.equals(extractSiteId(d.getCaisse())))
                .filter(d -> canAccessCaisse(user, d.getCaisse()))
                .toList();
    }

    private List<EcartCaisse> filterEcarts(List<EcartCaisse> ecarts, Long caisseId, Long siteId) {
        Utilisateur user = requireCurrentUser();
        return ecarts.stream()
                .filter(e -> e.getSessionCaisse() != null && e.getSessionCaisse().getCaisse() != null)
                .filter(e -> caisseId == null || caisseId.equals(e.getSessionCaisse().getCaisse().getId()))
                .filter(e -> siteId == null || siteId.equals(extractSiteId(e.getSessionCaisse().getCaisse())))
                .filter(e -> canAccessCaisse(user, e.getSessionCaisse().getCaisse()))
                .toList();
    }

    private void validateSessionScope(SessionCaisse session) {
        Utilisateur user = requireCurrentUser();
        if (!canAccessCaisse(user, session.getCaisse())) {
            throw new BusinessException("Accès refusé à cette session caisse");
        }
    }

    private Utilisateur requireCurrentUser() {
        Utilisateur user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException("Utilisateur non authentifié");
        }
        return user;
    }

    private boolean canAccessCaisse(Utilisateur user, Caisse caisse) {
        if (user.getRole() == null || user.getRole().getCode() == null || caisse == null) {
            return false;
        }

        RoleCode role = user.getRole().getCode();
        if (role == RoleCode.ADMIN) {
            return true;
        }

        Long caisseSiteId = extractSiteId(caisse);

        if (role == RoleCode.CAISSIER) {
            if (user.getSite() != null && user.getSite().getId() != null && caisseSiteId != null) {
                return user.getSite().getId().equals(caisseSiteId);
            }
            return caisse.getCaissierResponsable() != null
                    && caisse.getCaissierResponsable().getId() != null
                    && caisse.getCaissierResponsable().getId().equals(user.getId());
        }

        if (role == RoleCode.CHEF_BUREAU || role == RoleCode.CONTROLEUR || role == RoleCode.RCI) {
            Long userAntenneId = null;
            if (user.getEmploye() != null && user.getEmploye().getAgence() != null) {
                userAntenneId = user.getEmploye().getAgence().getId();
            } else if (user.getSite() != null && user.getSite().getAgence() != null) {
                userAntenneId = user.getSite().getAgence().getId();
            } else if (user.getEmploye() != null
                    && user.getEmploye().getSite() != null
                    && user.getEmploye().getSite().getAgence() != null) {
                userAntenneId = user.getEmploye().getSite().getAgence().getId();
            }

            return userAntenneId != null
                    && caisse.getSite() != null
                    && caisse.getSite().getAgence() != null
                    && caisse.getSite().getAgence().getId() != null
                    && caisse.getSite().getAgence().getId().equals(userAntenneId);
        }

        return false;
    }

    private Long extractSiteId(Caisse caisse) {
        return caisse != null && caisse.getSite() != null ? caisse.getSite().getId() : null;
    }

    private DateRange validateRange(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null) {
            throw new BusinessException("dateDebut et dateFin sont obligatoires");
        }
        if (dateFin.isBefore(dateDebut)) {
            throw new BusinessException("dateFin doit être >= dateDebut");
        }
        return new DateRange(dateDebut, dateFin);
    }

    private void logExport(String type, Long entityId) {
        Utilisateur currentUser = SecurityUtils.getCurrentUser();
        String actor = currentUser != null ? currentUser.getUsername() : "unknown";
        String reason = String.format(Locale.ROOT, "Export CSV rapport caisse (type=%s, actor=%s)", type, actor);
        safeLogRapportExport(
                "ECARTS".equalsIgnoreCase(type) ? AuditModule.CONTROLE_INTERNE : AuditModule.RAPPORT_CAISSE,
                "RapportCaisse",
                entityId,
                reason,
                type
        );
    }

    private void safeLogRapportConsultation(AuditModule module, String entityType, Long entityId, String commentaire, String referenceMetier) {
        try {
            auditService.logActionRequiresNew(
                    AuditAction.RAPPORT_GENERE,
                    module,
                    entityType,
                    entityId,
                    true,
                    AuditSeverity.INFO,
                    commentaire,
                    referenceMetier,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        } catch (Exception ignored) {
            // Non bloquant pour la génération de rapport
        }
    }

    private void safeLogRapportExport(AuditModule module, String entityType, Long entityId, String commentaire, String referenceMetier) {
        try {
            auditService.logActionRequiresNew(
                    AuditAction.DATA_EXPORT,
                    module,
                    entityType,
                    entityId,
                    true,
                    AuditSeverity.INFO,
                    commentaire,
                    referenceMetier,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        } catch (Exception ignored) {
            // Non bloquant pour l'export de rapport
        }
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        boolean needQuotes = text.contains(",") || text.contains("\n") || text.contains("\r") || text.contains("\"");
        String escaped = text.replace("\"", "\"\"");
        return needQuotes ? "\"" + escaped + "\"" : escaped;
    }

    private static final class DateRange {
        private final LocalDate dateDebut;
        private final LocalDate dateFin;

        private DateRange(LocalDate dateDebut, LocalDate dateFin) {
            this.dateDebut = dateDebut;
            this.dateFin = dateFin;
        }
    }

    private static final class AggregateData {
        private final List<SessionCaisse> sessions;
        private final List<OperationCaisse> operations;
        private final List<DepenseCaisse> depenses;
        private final List<EcartCaisse> ecarts;
        private final long nombreSessionsNonCloturees;
        private final BigDecimal totalEntrees;
        private final BigDecimal totalSorties;
        private final BigDecimal soldeTheoriqueTotal;
        private final BigDecimal soldePhysiqueTotal;
        private final BigDecimal ecartTotal;
        private final BigDecimal montantDepenses;
        private final BigDecimal montantEcarts;

        private AggregateData(
                List<SessionCaisse> sessions,
                List<OperationCaisse> operations,
                List<DepenseCaisse> depenses,
                List<EcartCaisse> ecarts,
                long nombreSessionsNonCloturees,
                BigDecimal totalEntrees,
                BigDecimal totalSorties,
                BigDecimal soldeTheoriqueTotal,
                BigDecimal soldePhysiqueTotal,
                BigDecimal ecartTotal,
                BigDecimal montantDepenses,
                BigDecimal montantEcarts
        ) {
            this.sessions = sessions;
            this.operations = operations;
            this.depenses = depenses;
            this.ecarts = ecarts;
            this.nombreSessionsNonCloturees = nombreSessionsNonCloturees;
            this.totalEntrees = totalEntrees;
            this.totalSorties = totalSorties;
            this.soldeTheoriqueTotal = soldeTheoriqueTotal;
            this.soldePhysiqueTotal = soldePhysiqueTotal;
            this.ecartTotal = ecartTotal;
            this.montantDepenses = montantDepenses;
            this.montantEcarts = montantEcarts;
        }
    }
}
