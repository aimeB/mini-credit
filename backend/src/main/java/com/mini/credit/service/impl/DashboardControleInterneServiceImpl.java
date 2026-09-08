package com.mini.credit.service.impl;

import com.mini.credit.dto.dashboard.controleinterne.ActionDashboardResponse;
import com.mini.credit.dto.dashboard.controleinterne.AlerteDashboardResponse;
import com.mini.credit.dto.dashboard.controleinterne.DashboardControleInterneResponse;
import com.mini.credit.dto.dashboard.controleinterne.ResumeAudit;
import com.mini.credit.dto.dashboard.controleinterne.ResumeCaisse;
import com.mini.credit.dto.dashboard.controleinterne.ResumeCredits;
import com.mini.credit.dto.dashboard.controleinterne.ResumeDepenses;
import com.mini.credit.dto.dashboard.controleinterne.ResumeRecettes;
import com.mini.credit.dto.dashboard.controleinterne.ResumeRetraits;
import com.mini.credit.entity.audit.AuditLog;
import com.mini.credit.entity.caisse.DepenseCaisse;
import com.mini.credit.entity.caisse.EcartCaisse;
import com.mini.credit.entity.caisse.OperationCaisse;
import com.mini.credit.entity.caisse.RecetteJournaliereTerrain;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.entity.credit.Credit;
import com.mini.credit.entity.epargne.DemandeRetraitEpargne;
import com.mini.credit.enums.CategorieOperationCaisse;
import com.mini.credit.enums.DepenseCaisseStatus;
import com.mini.credit.enums.SourceOperationCaisse;
import com.mini.credit.enums.StatutCredit;
import com.mini.credit.enums.StatutDemandeRetrait;
import com.mini.credit.enums.StatutEcartCaisse;
import com.mini.credit.enums.StatutRecetteJournaliere;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditSeverity;
import com.mini.credit.repository.audit.AuditLogRepository;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.caisse.DepenseCaisseRepository;
import com.mini.credit.repository.caisse.EcartCaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.RecetteJournaliereTerrainRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.credit.CreditRepository;
import com.mini.credit.repository.epargne.DemandeRetraitEpargneRepository;
import com.mini.credit.service.DashboardControleInterneService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardControleInterneServiceImpl implements DashboardControleInterneService {

    private final SessionCaisseRepository sessionCaisseRepository;
    private final EcartCaisseRepository ecartCaisseRepository;
    private final DepenseCaisseRepository depenseCaisseRepository;
    private final RecetteJournaliereTerrainRepository recetteJournaliereTerrainRepository;
    private final DemandeRetraitEpargneRepository demandeRetraitEpargneRepository;
    private final CreditRepository creditRepository;
    private final AuditLogRepository auditLogRepository;
    private final OperationCaisseRepository operationCaisseRepository;
    private final CaisseRepository caisseRepository;

    @Override
    public DashboardControleInterneResponse getDashboardControleInterne(
            LocalDate date,
            LocalDate dateDebut,
            LocalDate dateFin,
            Long siteId,
            Long caisseId
    ) {
        LocalDate debut = resolveDateDebut(date, dateDebut);
        LocalDate fin = resolveDateFin(date, dateFin);

        if (fin.isBefore(debut)) {
            throw new IllegalArgumentException("La date de fin doit etre superieure ou egale a la date de debut");
        }

        if (caisseId != null && caisseRepository.findById(caisseId).isEmpty()) {
            throw new IllegalArgumentException("Caisse introuvable: " + caisseId);
        }

        List<SessionCaisse> sessions = sessionCaisseRepository.findByDateComptableBetweenOrderByDateComptableDesc(debut, fin)
                .stream()
                .filter(s -> matchesSite(s.getCaisse() != null ? s.getCaisse().getSite() != null ? s.getCaisse().getSite().getId() : null : null, siteId))
                .filter(s -> matchesCaisse(s.getCaisse() != null ? s.getCaisse().getId() : null, caisseId))
                .toList();

        List<EcartCaisse> ecarts = ecartCaisseRepository.findByDateJourBetweenOrderByDateJourDesc(debut, fin)
                .stream()
                .filter(e -> matchesSite(e.getSessionCaisse() != null && e.getSessionCaisse().getCaisse() != null && e.getSessionCaisse().getCaisse().getSite() != null ? e.getSessionCaisse().getCaisse().getSite().getId() : null, siteId))
                .filter(e -> matchesCaisse(e.getSessionCaisse() != null && e.getSessionCaisse().getCaisse() != null ? e.getSessionCaisse().getCaisse().getId() : null, caisseId))
                .toList();

        List<DepenseCaisse> depenses = depenseCaisseRepository.findByDateDemandeBetweenOrderByDateDemandeDesc(debut.atStartOfDay(), fin.plusDays(1).atStartOfDay().minusNanos(1))
                .stream()
                .filter(d -> matchesSite(d.getSite() != null ? d.getSite().getId() : null, siteId))
                .filter(d -> matchesCaisse(d.getCaisse() != null ? d.getCaisse().getId() : null, caisseId))
                .toList();

        List<RecetteJournaliereTerrain> recettes = recetteJournaliereTerrainRepository.findByDateJourBetween(debut, fin)
                .stream()
                .filter(r -> matchesSite(r.getAgent() != null && r.getAgent().getSite() != null ? r.getAgent().getSite().getId() : null, siteId))
                .toList();

        List<DemandeRetraitEpargne> retraits = demandeRetraitEpargneRepository.findAll()
                .stream()
                .filter(r -> r.getDateDemande() != null && !r.getDateDemande().toLocalDate().isBefore(debut) && !r.getDateDemande().toLocalDate().isAfter(fin))
                .filter(r -> matchesSite(r.getMembre() != null && r.getMembre().getSite() != null ? r.getMembre().getSite().getId() : null, siteId))
                .toList();

        List<Credit> credits = creditRepository.findAll()
                .stream()
                .filter(c -> matchesSite(c.getSite() != null ? c.getSite().getId() : null, siteId))
                .filter(c -> matchesDateRange(c.getDateCreation() != null ? c.getDateCreation().toLocalDate() : c.getDateApprobation(), debut, fin))
                .toList();

        List<AuditLog> audits = auditLogRepository.findAll()
                .stream()
                .filter(a -> matchesDateRange(a.getDateAction() != null ? a.getDateAction().toLocalDate() : null, debut, fin))
                .filter(a -> matchesSite(a.getSiteId(), siteId))
                .filter(a -> matchesCaisse(a.getCaisseId(), caisseId))
                .toList();

        List<OperationCaisse> operationsJour = operationCaisseRepository.findJournal(
                null,
                caisseId,
            null,
                siteId,
                null,
                null,
                null,
                null,
                fin.atStartOfDay(),
                fin.plusDays(1).atStartOfDay().minusNanos(1),
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 1000)
            )
            .getContent();

        ResumeCaisse resumeCaisse = ResumeCaisse.builder()
                .sessionsOuvertes(countSessionsByStatus(sessions, StatutSessionCaisse.OUVERTE))
                .sessionsFermeesNonValidees(countSessionsByStatus(sessions, StatutSessionCaisse.CLOTUREE, StatutSessionCaisse.PRE_CLOTUREE))
                .ecartsDetectes(countEcartsByStatus(ecarts, StatutEcartCaisse.DETECTE))
                .ecartsEnInvestigation(countEcartsByStatus(ecarts, StatutEcartCaisse.EN_INVESTIGATION))
                .ecartsNonResolus(ecarts.stream().filter(e -> e.getStatut() != StatutEcartCaisse.RESOLU && e.getStatut() != StatutEcartCaisse.ACCEPTE).count())
                .build();

        ResumeDepenses resumeDepenses = ResumeDepenses.builder()
                .brouillon(countDepensesByStatus(depenses, DepenseCaisseStatus.BROUILLON))
                .enAttenteValidation(countDepensesByStatus(depenses, DepenseCaisseStatus.EN_ATTENTE_VALIDATION))
                .valideesNonPayees(countDepensesByStatus(depenses, DepenseCaisseStatus.VALIDEE))
                .payees(countDepensesByStatus(depenses, DepenseCaisseStatus.PAYEE))
                .rejetees(countDepensesByStatus(depenses, DepenseCaisseStatus.REJETEE))
                .build();

        ResumeRecettes resumeRecettes = ResumeRecettes.builder()
                .brouillon(countRecettesByStatus(recettes, StatutRecetteJournaliere.CREEE))
                .soumises(countRecettesByStatus(recettes, StatutRecetteJournaliere.EN_ATTENTE_VALIDATION))
                .validees(countRecettesByStatus(recettes, StatutRecetteJournaliere.VALIDEE))
                .rejetees(countRecettesByStatus(recettes, StatutRecetteJournaliere.REJETEE))
                .build();

        ResumeRetraits resumeRetraits = ResumeRetraits.builder()
                .creees(countRetraitsByStatus(retraits, StatutDemandeRetrait.CREEE, StatutDemandeRetrait.EN_ATTENTE_VALIDATION))
                .valides(countRetraitsByStatus(retraits, StatutDemandeRetrait.VALIDEE))
                .rejetees(countRetraitsByStatus(retraits, StatutDemandeRetrait.REJETEE))
                .decaissees(countRetraitsByStatus(retraits, StatutDemandeRetrait.DECAISSEE))
                .build();

        ResumeCredits resumeCredits = ResumeCredits.builder()
                .approuves(countCreditsByStatus(credits, StatutCredit.APPROUVE, StatutCredit.DECAISSE))
                .enCours(countCreditsByStatus(credits, StatutCredit.EN_COURS))
                .enRetard(countCreditsByStatus(credits, StatutCredit.EN_RETARD))
                .contentieux(countCreditsByStatus(credits, StatutCredit.CONTENTIEUX))
                .clotures(countCreditsByStatus(credits, StatutCredit.REMBOURSE))
                .build();

        ResumeAudit resumeAudit = ResumeAudit.builder()
                .totalEvenements((long) audits.size())
                .infos(audits.stream().filter(a -> a.getSeverity() == AuditSeverity.INFO).count())
                .warnings(audits.stream().filter(a -> a.getSeverity() == AuditSeverity.WARNING).count())
                .critiques(audits.stream().filter(a -> a.getSeverity() == AuditSeverity.CRITICAL).count())
                .build();

        return DashboardControleInterneResponse.builder()
                .dateDebut(debut)
                .dateFin(fin)
                .siteId(siteId)
                .caisseId(caisseId)
                .caisse(resumeCaisse)
                .depenses(resumeDepenses)
                .recettes(resumeRecettes)
                .retraits(resumeRetraits)
                .credits(resumeCredits)
                .audit(resumeAudit)
                .alertes(buildAlertes(fin, sessions, ecarts, depenses, recettes, retraits, credits, audits, operationsJour,
                    resumeCaisse, resumeDepenses, resumeRecettes, resumeRetraits, resumeCredits, resumeAudit))
                .build();
    }

    private List<AlerteDashboardResponse> buildAlertes(
            LocalDate dateReference,
            List<SessionCaisse> sessions,
            List<EcartCaisse> ecarts,
            List<DepenseCaisse> depensesList,
            List<RecetteJournaliereTerrain> recettesList,
            List<DemandeRetraitEpargne> retraitsList,
            List<Credit> creditsList,
            List<AuditLog> audits,
            List<OperationCaisse> operationsJour,
            ResumeCaisse resumeCaisse,
            ResumeDepenses resumeDepenses,
            ResumeRecettes resumeRecettes,
            ResumeRetraits resumeRetraits,
            ResumeCredits resumeCredits,
            ResumeAudit resumeAudit
    ) {
        List<AlerteDashboardResponse> alertes = new ArrayList<>();

            SessionCaisse sessionAncienneOuverte = sessions.stream()
                .filter(s -> s.getStatut() == StatutSessionCaisse.OUVERTE)
                .filter(s -> s.getDateComptable() != null && s.getDateComptable().isBefore(dateReference))
                .findFirst()
                .orElse(null);
            long sessionAnciennesOuvertesCount = sessions.stream()
                .filter(s -> s.getStatut() == StatutSessionCaisse.OUVERTE)
                .filter(s -> s.getDateComptable() != null && s.getDateComptable().isBefore(dateReference))
                .count();
            if (sessionAnciennesOuvertesCount > 0) {
                Long referenceId = sessionAncienneOuverte != null ? sessionAncienneOuverte.getId() : null;
                String route = referenceId != null ? "/caisses/sessions/" + referenceId : "/caisses/controle?statut=OUVERTE";
                alertes.add(alert("CAISSE_SESSION_ANCIENNE_OUVERTE", "CAISSE", "WARNING", "Session ancienne encore ouverte",
                    "Une ou plusieurs sessions d'une date antérieure sont encore ouvertes.",
                    sessionAnciennesOuvertesCount, "SESSION_ANCIENNE_OUVERTE", "Voir la session", route,
                    referenceId, "SessionCaisse", dateReference));
            }

            long preCloturees = sessions.stream().filter(s -> s.getStatut() == StatutSessionCaisse.PRE_CLOTUREE).count();
            if (preCloturees > 0) {
                alertes.add(alert("CAISSE_PRE_CLOTUREE", "CAISSE", "WARNING", "Sessions en pré-clôture",
                    "Des sessions attendent encore la validation de contrôle.",
                    preCloturees, "SESSIONS_PRE_CLOTUREE", "Contrôle caisse", "/caisses/controle?statut=PRE_CLOTUREE",
                    null, "SessionCaisse", dateReference));
            }

            long valideeControle = sessions.stream().filter(s -> s.getStatut() == StatutSessionCaisse.VALIDEE_CONTROLE).count();
            if (valideeControle > 0) {
                SessionCaisse session = sessions.stream().filter(s -> s.getStatut() == StatutSessionCaisse.VALIDEE_CONTROLE).findFirst().orElse(null);
                Long referenceId = session != null ? session.getId() : null;
                String route = referenceId != null ? "/caisses/sessions/" + referenceId : "/caisses/controle?statut=VALIDEE_CONTROLE";
                alertes.add(alert("CAISSE_VALIDEE_NON_CLOTUREE", "CAISSE", "WARNING", "Sessions validées non clôturées",
                    "Des sessions validées contrôle n'ont pas encore été clôturées définitivement.",
                    valideeControle, "SESSIONS_VALIDEE_CONTROLE", "Voir les sessions", route,
                    referenceId, "SessionCaisse", dateReference));
            }

                if (resumeCaisse.getEcartsNonResolus() > 0) {
                alertes.add(alert("ECART_CAISSE", "CAISSE", "CRITIQUE", "Ecarts caisse non résolus",
                    "Des écarts de caisse restent ouverts et nécessitent une action de contrôle.",
                    resumeCaisse.getEcartsNonResolus(), "OUVRIR_ECARTS", "Consulter les écarts", "/caisses/rapports/ecarts?dateDebut=" + dateReference + "&dateFin=" + dateReference,
                    null, "EcartCaisse", dateReference));
            }

                if (resumeDepenses.getEnAttenteValidation() > 0) {
                alertes.add(alert("DEPENSES_ATTENTE_VALIDATION", "DEPENSES", "WARNING", "Dépenses en attente de validation",
                    "Des demandes de dépenses attendent une validation.",
                    resumeDepenses.getEnAttenteValidation(), "DEPENSES_EN_ATTENTE", "Voir les dépenses", routeWithDate("/caisses/depenses", dateReference, Map.of("statut", "EN_ATTENTE_VALIDATION")),
                    null, "DepenseCaisse", dateReference));
            }

                if (resumeDepenses.getValideesNonPayees() > 0) {
                alertes.add(alert("DEPENSES_VALIDEES_NON_PAYEES", "DEPENSES", "WARNING", "Dépenses validées non payées",
                    "Des dépenses validées n'ont pas encore été payées.",
                    resumeDepenses.getValideesNonPayees(), "DEPENSES_VALIDEES", "Voir les dépenses", routeWithDate("/caisses/depenses", dateReference, Map.of("statut", "VALIDEE")),
                    null, "DepenseCaisse", dateReference));
            }

                long depensesPayeesJour = depensesList.stream()
                .filter(d -> d.getStatut() == DepenseCaisseStatus.PAYEE)
                .filter(d -> d.getDatePaiement() != null && d.getDatePaiement().toLocalDate().equals(dateReference))
                .count();
            if (depensesPayeesJour > 0) {
                alertes.add(alert("DEPENSES_PAYEES_JOUR", "DEPENSES", "INFO", "Dépenses payées aujourd'hui",
                    "Vue rapide des dépenses réglées sur la journée de référence.",
                    depensesPayeesJour, "DEPENSES_PAYEES", "Voir les dépenses", routeWithDate("/caisses/depenses", dateReference, Map.of("statut", "PAYEE")),
                    null, "DepenseCaisse", dateReference));
            }

            if (resumeRecettes.getSoumises() > 0) {
                alertes.add(alert("RECETTES_ATTENTE_VALIDATION", "RECETTES", "WARNING", "Recettes en attente de validation",
                    "Des recettes terrain soumises attendent encore un contrôle.",
                    resumeRecettes.getSoumises(), "RECETTES_ATTENTE", "Voir les recettes", routeWithDate("/recettes", dateReference, Map.of("statut", "EN_ATTENTE_VALIDATION")),
                    null, "RecetteJournaliereTerrain", dateReference));
            }

            long recettesAvecEcart = recettesList.stream().filter(r -> r.getVariance() != null && r.getVariance().doubleValue() > 0).count();
            if (recettesAvecEcart > 0) {
                RecetteJournaliereTerrain recetteAvecEcart = recettesList.stream().filter(r -> r.getVariance() != null && r.getVariance().doubleValue() > 0).findFirst().orElse(null);
                Long referenceId = recetteAvecEcart != null ? recetteAvecEcart.getId() : null;
                String route = referenceId != null
                    ? "/recettes/" + referenceId
                    : routeWithDate("/recettes", dateReference, Map.of("statut", "REJETEE"));
                alertes.add(alert("RECETTES_AVEC_ECART", "RECETTES", "WARNING", "Recettes avec écart",
                    "Des recettes présentent une variance et demandent vérification.",
                    recettesAvecEcart, "RECETTES_ECART", "Ouvrir les recettes", route,
                    referenceId, "RecetteJournaliereTerrain", dateReference));
            }

            long recettesValideesJour = recettesList.stream()
                .filter(r -> r.getStatut() == StatutRecetteJournaliere.VALIDEE)
                .filter(r -> r.getDateValidation() != null && r.getDateValidation().toLocalDate().equals(dateReference))
                .count();
            if (recettesValideesJour > 0) {
                alertes.add(alert("RECETTES_VALIDEES_JOUR", "RECETTES", "INFO", "Recettes validées aujourd'hui",
                    "Accès direct à la liste des recettes validées sur la journée.",
                    recettesValideesJour, "RECETTES_VALIDEES", "Voir les recettes", routeWithDate("/recettes", dateReference, Map.of("statut", "VALIDEE")),
                    null, "RecetteJournaliereTerrain", dateReference));
            }

            long retraitsAttenteValidation = retraitsList.stream().filter(r -> r.getStatut() == StatutDemandeRetrait.EN_ATTENTE_VALIDATION).count();
            if (retraitsAttenteValidation > 0) {
                alertes.add(alert("RETRAITS_ATTENTE_VALIDATION", "RETRAITS", "WARNING", "Retraits en attente de validation",
                    "Des demandes de retrait épargne attendent encore validation.",
                    retraitsAttenteValidation, "RETRAITS_ATTENTE", "Voir les retraits", "/epargne/demandes-retrait?statut=EN_ATTENTE_VALIDATION",
                    null, "DemandeRetraitEpargne", dateReference));
            }

            long retraitsValidesNonPayes = retraitsList.stream().filter(r -> r.getStatut() == StatutDemandeRetrait.VALIDEE).count();
            if (retraitsValidesNonPayes > 0) {
                alertes.add(alert("RETRAITS_VALIDES_NON_PAYES", "RETRAITS", "WARNING", "Retraits validés non payés",
                    "Des retraits validés restent en attente de décaissement.",
                    retraitsValidesNonPayes, "RETRAITS_VALIDES", "Voir les retraits", "/epargne/demandes-retrait?statut=VALIDEE",
                    null, "DemandeRetraitEpargne", dateReference));
            }

            long retraitsPayesJour = retraitsList.stream()
                .filter(r -> r.getStatut() == StatutDemandeRetrait.DECAISSEE)
                .filter(r -> r.getDateModification() != null && r.getDateModification().toLocalDate().equals(dateReference))
                .count();
            if (retraitsPayesJour > 0) {
                alertes.add(alert("RETRAITS_PAYES_JOUR", "RETRAITS", "INFO", "Retraits payés aujourd'hui",
                    "Accès direct aux retraits décaissés sur la journée.",
                    retraitsPayesJour, "RETRAITS_PAYES", "Voir les retraits", "/epargne/demandes-retrait?statut=DECAISSEE&dateDebut=" + dateReference + "&dateFin=" + dateReference,
                    null, "DemandeRetraitEpargne", dateReference));
            }

            long creditsApprouvesNonDecaisses = creditsList.stream().filter(c -> c.getStatut() == StatutCredit.APPROUVE).count();
            if (creditsApprouvesNonDecaisses > 0) {
                alertes.add(alert("CREDITS_APPROUVES_NON_DECAISSES", "CREDITS", "WARNING", "Crédits approuvés non décaissés",
                    "Des crédits approuvés sont en attente de décaissement.",
                    creditsApprouvesNonDecaisses, "CREDITS_APPROUVES", "Voir les crédits", "/credits/liste?statut=APPROUVE",
                    null, "Credit", dateReference));
            }

            long decaissementsCreditJour = operationsJour.stream().filter(o -> o.getSource() == SourceOperationCaisse.CREDIT_DECAISSEMENT).count();
            if (decaissementsCreditJour > 0) {
                alertes.add(alert("DECAISSEMENTS_CREDIT_JOUR", "CREDITS", "INFO", "Décaissements crédit du jour",
                    "Afficher les décaissements crédit enregistrés aujourd'hui dans le journal caisse.",
                    decaissementsCreditJour, "JOURNAL_DECAISSEMENTS", "Voir le journal", routeWithDate("/caisses/journal", dateReference, Map.of("source", "CREDIT_DECAISSEMENT")),
                    null, "OperationCaisse", dateReference));
            }

            long remboursementsCreditJour = operationsJour.stream().filter(o -> o.getSource() == SourceOperationCaisse.CREDIT_REMBOURSEMENT).count();
            if (remboursementsCreditJour > 0) {
                alertes.add(alert("REMBOURSEMENTS_CREDIT_JOUR", "CREDITS", "INFO", "Remboursements crédit du jour",
                    "Afficher les remboursements crédit enregistrés aujourd'hui dans le journal caisse.",
                    remboursementsCreditJour, "JOURNAL_REMBOURSEMENTS", "Voir le journal", routeWithDate("/caisses/journal", dateReference, Map.of("source", "CREDIT_REMBOURSEMENT")),
                    null, "OperationCaisse", dateReference));
            }

                if (resumeAudit.getCritiques() > 0) {
                alertes.add(alert("AUDIT_CRITIQUE", "AUDIT", "CRITIQUE", "Actions critiques récentes",
                    "Des événements d'audit critiques ont été détectés.",
                    resumeAudit.getCritiques(), "AUDIT_CRITICAL", "Voir l'audit", "/audit/logs?severity=CRITICAL",
                    null, "AuditLog", dateReference));
            }

                if (resumeAudit.getWarnings() > 0) {
                alertes.add(alert("AUDIT_WARNING", "AUDIT", "WARNING", "Événements warning récents",
                    "Des warnings d'audit nécessitent une revue.",
                    resumeAudit.getWarnings(), "AUDIT_WARNING", "Voir l'audit", "/audit/logs?severity=WARNING",
                    null, "AuditLog", dateReference));
            }

            long refusTransitionSession = audits.stream()
                .filter(a -> a.getAction() == AuditAction.REFUS_TRANSITION || a.getAction() == AuditAction.ACCESS_DENIED)
                .filter(a -> "SessionCaisse".equalsIgnoreCase(a.getEntityType()) || a.getSessionCaisseId() != null)
                .count();
            if (refusTransitionSession > 0) {
                alertes.add(alert("AUDIT_REFUS_SESSION", "AUDIT", "WARNING", "Tentatives refusées sur session",
                    "Des tentatives refusées ont été détectées sur des sessions caisse.",
                    refusTransitionSession, "AUDIT_REFUS_SESSION", "Voir l'audit", "/audit/logs?action=REFUS_TRANSITION&module=SESSION_CAISSE",
                    null, "AuditLog", dateReference));
            }

            long totalDepensesJour = operationsJour.stream().filter(o -> o.getCategorieOperation() == CategorieOperationCaisse.DEPENSE).count();
            if (totalDepensesJour > 0) {
                alertes.add(alert("JOURNAL_DEPENSES_JOUR", "JOURNAL", "INFO", "Total dépenses du jour",
                    "Accès direct aux opérations de dépense du journal caisse.",
                    totalDepensesJour, "JOURNAL_DEPENSES", "Voir le journal", routeWithDate("/caisses/journal", dateReference, Map.of("categorie", "DEPENSE")),
                    null, "OperationCaisse", dateReference));
            }

            long totalEntreesJour = operationsJour.stream().filter(o -> o.getTypeOperation() == TypeOperationCaisse.ENTREE).count();
            if (totalEntreesJour > 0) {
                alertes.add(alert("JOURNAL_ENTREES_JOUR", "JOURNAL", "INFO", "Total entrées du jour",
                    "Accès direct aux entrées du journal caisse.",
                    totalEntreesJour, "JOURNAL_ENTREES", "Voir le journal", routeWithDate("/caisses/journal", dateReference, Map.of("typeOperation", "ENTREE")),
                    null, "OperationCaisse", dateReference));
            }

            long totalSortiesJour = operationsJour.stream().filter(o -> o.getTypeOperation() == TypeOperationCaisse.SORTIE).count();
            if (totalSortiesJour > 0) {
                alertes.add(alert("JOURNAL_SORTIES_JOUR", "JOURNAL", "INFO", "Total sorties du jour",
                    "Accès direct aux sorties du journal caisse.",
                    totalSortiesJour, "JOURNAL_SORTIES", "Voir le journal", routeWithDate("/caisses/journal", dateReference, Map.of("typeOperation", "SORTIE")),
                    null, "OperationCaisse", dateReference));
            }

                if (resumeCaisse.getEcartsDetectes() > 0 || resumeCaisse.getEcartsEnInvestigation() > 0) {
                alertes.add(alert("RAPPORT_ECARTS", "RAPPORT", "WARNING", "Rapport écarts à consulter",
                    "Consulter le rapport des écarts sur la période de référence.",
                    resumeCaisse.getEcartsDetectes() + resumeCaisse.getEcartsEnInvestigation(),
                    "RAPPORT_ECARTS", "Voir le rapport", "/caisses/rapports/ecarts?dateDebut=" + dateReference + "&dateFin=" + dateReference,
                    null, "RapportCaisseEcarts", dateReference));
            }

            return alertes.stream()
                .sorted((a, b) -> severityPriority(a.getNiveau()) - severityPriority(b.getNiveau()))
                .collect(Collectors.toList());
    }

    private AlerteDashboardResponse alert(
                String code,
                String module,
            String niveau,
            String titre,
            String message,
            Long nombre,
            String actionCode,
            String actionLibelle,
                String actionRoute,
                Long referenceId,
                String referenceType,
                LocalDate date
    ) {
            String normalizedRoute = normalizeRoute(actionRoute);
        return AlerteDashboardResponse.builder()
                .code(code)
                .type(code)
                .module(module)
                .niveau(niveau)
                .titre(titre)
                .message(message)
                .nombre(nombre)
                .routeFrontend(normalizedRoute)
                .actionUrl(normalizedRoute)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .date(date)
                .action(ActionDashboardResponse.builder()
                        .code(actionCode)
                        .libelle(actionLibelle)
                    .route(normalizedRoute)
                        .build())
                .build();
    }

            private String routeWithDate(String baseRoute, LocalDate date, Map<String, String> params) {
            Map<String, String> merged = new LinkedHashMap<>();
            merged.put("dateDebut", date.toString());
            merged.put("dateFin", date.toString());
            if (params != null) {
                merged.putAll(params);
            }
            return normalizeRoute(baseRoute + "?" + merged.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&")));
            }

            private String normalizeRoute(String route) {
            if (route == null || route.isBlank()) {
                return "/dashboard/controle-interne";
            }
            return route.startsWith("/") ? route : "/" + route;
            }

            private int severityPriority(String niveau) {
            if (niveau == null) {
                return 9;
            }
            String normalized = niveau.toUpperCase();
            if ("CRITIQUE".equals(normalized) || "CRITICAL".equals(normalized)) {
                return 0;
            }
            if ("WARNING".equals(normalized)) {
                return 1;
            }
            if ("INFO".equals(normalized)) {
                return 2;
            }
            return 3;
            }

    private LocalDate resolveDateDebut(LocalDate date, LocalDate dateDebut) {
        if (date != null) {
            return date;
        }
        if (dateDebut != null) {
            return dateDebut;
        }
        return LocalDate.now();
    }

    private LocalDate resolveDateFin(LocalDate date, LocalDate dateFin) {
        if (date != null) {
            return date;
        }
        if (dateFin != null) {
            return dateFin;
        }
        return LocalDate.now();
    }

    private boolean matchesSite(Long candidate, Long expectedSiteId) {
        return expectedSiteId == null || (candidate != null && candidate.equals(expectedSiteId));
    }

    private boolean matchesCaisse(Long candidate, Long expectedCaisseId) {
        return expectedCaisseId == null || (candidate != null && candidate.equals(expectedCaisseId));
    }

    private boolean matchesDateRange(LocalDate candidate, LocalDate dateDebut, LocalDate dateFin) {
        return candidate != null && !candidate.isBefore(dateDebut) && !candidate.isAfter(dateFin);
    }

    private long countSessionsByStatus(List<SessionCaisse> sessions, StatutSessionCaisse... statuts) {
        return sessions.stream().filter(s -> hasStatus(s.getStatut(), statuts)).count();
    }

    private long countEcartsByStatus(List<EcartCaisse> ecarts, StatutEcartCaisse... statuts) {
        return ecarts.stream().filter(e -> hasStatus(e.getStatut(), statuts)).count();
    }

    private long countDepensesByStatus(List<DepenseCaisse> depenses, DepenseCaisseStatus... statuts) {
        return depenses.stream().filter(d -> hasStatus(d.getStatut(), statuts)).count();
    }

    private long countRecettesByStatus(List<RecetteJournaliereTerrain> recettes, StatutRecetteJournaliere... statuts) {
        return recettes.stream().filter(r -> hasStatus(r.getStatut(), statuts)).count();
    }

    private long countRetraitsByStatus(List<DemandeRetraitEpargne> retraits, StatutDemandeRetrait... statuts) {
        return retraits.stream().filter(r -> hasStatus(r.getStatut(), statuts)).count();
    }

    private long countCreditsByStatus(List<Credit> credits, StatutCredit... statuts) {
        return credits.stream().filter(c -> hasStatus(c.getStatut(), statuts)).count();
    }

    @SafeVarargs
    private <T> boolean hasStatus(T current, T... statuts) {
        if (current == null || statuts == null) {
            return false;
        }
        for (T statut : statuts) {
            if (current.equals(statut)) {
                return true;
            }
        }
        return false;
    }
}
