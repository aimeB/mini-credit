package com.mini.credit.service.audit;

import com.mini.credit.dto.audit.CaisseHistoryDiagnosticAnomalyDTO;
import com.mini.credit.dto.audit.CaisseHistoryDiagnosticReportDTO;
import com.mini.credit.entity.caisse.Caisse;
import com.mini.credit.entity.caisse.SessionCaisse;
import com.mini.credit.enums.StatutSessionCaisse;
import com.mini.credit.enums.TypeOperationCaisse;
import com.mini.credit.repository.caisse.CaisseRepository;
import com.mini.credit.repository.caisse.OperationCaisseRepository;
import com.mini.credit.repository.caisse.SessionCaisseRepository;
import com.mini.credit.repository.projection.OperationCaisseDiagnosticProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaisseHistoryDiagnosticService {

    private static final List<StatutSessionCaisse> ACTIVE_SESSION_STATUSES = List.of(
            StatutSessionCaisse.OUVERTE,
            StatutSessionCaisse.PRE_CLOTUREE,
            StatutSessionCaisse.VALIDEE_CONTROLE
    );

    private final SessionCaisseRepository sessionCaisseRepository;
    private final CaisseRepository caisseRepository;
    private final OperationCaisseRepository operationCaisseRepository;
    private final JdbcTemplate jdbcTemplate;

    public CaisseHistoryDiagnosticReportDTO generateDiagnosticReport() {
        LocalDateTime generatedAt = LocalDateTime.now();
        List<SessionCaisse> sessions = sessionCaisseRepository.findAll();
        List<Caisse> caisses = caisseRepository.findAll();
        long operationsAnalyzed = operationCaisseRepository.count();

        List<CaisseHistoryDiagnosticAnomalyDTO> anomalies = new ArrayList<>();
        detectSessionAmountMismatches(sessions, anomalies);
        detectSessionStatusInconsistencies(sessions, anomalies);
        detectDuplicateActiveSessions(sessions, anomalies);
        detectDuplicateActiveCaisses(caisses, anomalies);
        detectOperationIntegrityIssues(anomalies);
        detectEmptyDataset(sessions.size(), caisses.size(), operationsAnalyzed, anomalies);

        Map<String, Long> anomaliesByType = anomalies.stream()
                .collect(Collectors.groupingBy(
                        CaisseHistoryDiagnosticAnomalyDTO::getAnomalyType,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        return CaisseHistoryDiagnosticReportDTO.builder()
                .generatedAt(generatedAt)
                .generatedBy(resolveGeneratedBy())
                .sessionsAnalyzed(sessions.size())
                .caissesAnalyzed(caisses.size())
                .operationsAnalyzed(operationsAnalyzed)
                .anomalyCount(anomalies.size())
                .anomaliesByType(anomaliesByType)
                .readOnlyMode(true)
                .correctionsApplied(false)
                .anomalies(anomalies)
                .build();
    }

            private void detectEmptyDataset(
                int sessionsAnalyzed,
                int caissesAnalyzed,
                long operationsAnalyzed,
                List<CaisseHistoryDiagnosticAnomalyDTO> anomalies
            ) {
            if (sessionsAnalyzed != 0 || caissesAnalyzed != 0 || operationsAnalyzed != 0) {
                return;
            }

            anomalies.add(CaisseHistoryDiagnosticAnomalyDTO.builder()
                .anomalyType("EMPTY_DATASET_WARNING")
                .severity("WARNING")
                .recommendation("Aucune caisse, session ou opération caisse n’a été trouvée. Le diagnostic est techniquement exécuté, mais il ne permet pas de conclure sur la conformité métier des données caisse.")
                .details("Le périmètre analysé est vide: 0 session, 0 caisse et 0 opération caisse détectées pendant l'exécution du diagnostic.")
                .build());
            }

    private void detectSessionAmountMismatches(
            List<SessionCaisse> sessions,
            List<CaisseHistoryDiagnosticAnomalyDTO> anomalies
    ) {
        for (SessionCaisse session : sessions) {
            BigDecimal expectedEntrees = safeAmount(
                    operationCaisseRepository.sumBySessionAndType(session.getId(), TypeOperationCaisse.ENTREE)
            );
            BigDecimal actualEntrees = safeAmount(session.getTotalEntrees());
            if (actualEntrees.compareTo(expectedEntrees) != 0) {
                anomalies.add(buildSessionAnomaly(
                        "TOTAL_ENTREES_INCORRECT",
                        "WARNING",
                        session,
                        expectedEntrees,
                        actualEntrees,
                        "Vérifier l'agrégat totalEntrees de la session par rapport aux opérations ENTREE historisées.",
                        "Le totalEntrees stocké ne correspond pas à la somme des opérations ENTREE de la session."
                ));
            }

            BigDecimal expectedSorties = safeAmount(
                    operationCaisseRepository.sumBySessionAndType(session.getId(), TypeOperationCaisse.SORTIE)
            );
            BigDecimal actualSorties = safeAmount(session.getTotalSorties());
            if (actualSorties.compareTo(expectedSorties) != 0) {
                anomalies.add(buildSessionAnomaly(
                        "TOTAL_SORTIES_INCORRECT",
                        "WARNING",
                        session,
                        expectedSorties,
                        actualSorties,
                        "Vérifier l'agrégat totalSorties de la session par rapport aux opérations SORTIE historisées.",
                        "Le totalSorties stocké ne correspond pas à la somme des opérations SORTIE de la session."
                ));
            }

            BigDecimal expectedSoldeTheorique = safeAmount(session.getSoldeOuverture())
                    .add(expectedEntrees)
                    .subtract(expectedSorties);
            BigDecimal actualSoldeTheorique = safeAmount(session.getSoldeTheorique());
            if (actualSoldeTheorique.compareTo(expectedSoldeTheorique) != 0) {
                anomalies.add(buildSessionAnomaly(
                        "SOLDE_THEORIQUE_INCORRECT",
                        "WARNING",
                        session,
                        expectedSoldeTheorique,
                        actualSoldeTheorique,
                        "Vérifier le solde théorique stocké de la session par rapport au calcul d'ouverture + entrées - sorties.",
                        "Le soldeTheorique stocké ne correspond pas au calcul basé sur les opérations historisées."
                ));
            }
        }
    }

    private void detectSessionStatusInconsistencies(
            List<SessionCaisse> sessions,
            List<CaisseHistoryDiagnosticAnomalyDTO> anomalies
    ) {
        for (SessionCaisse session : sessions) {
            if (session.getStatut() == StatutSessionCaisse.CLOTUREE
                    && (session.getDateControle() == null || session.getControleValidePar() == null)) {
                anomalies.add(buildSessionAnomaly(
                        "CLOSED_SESSION_WITHOUT_CONTROL_VALIDATION",
                        "CRITICAL",
                        session,
                        null,
                        null,
                        "Contrôler la traçabilité de validation avant clôture finale.",
                        "Une session CLOTUREE ne porte pas toutes les métadonnées de validation contrôle attendues."
                ));
            }

            if (session.getStatut() == StatutSessionCaisse.CLOTUREE && session.getDateCloture() == null) {
                anomalies.add(buildSessionAnomaly(
                        "CLOSED_SESSION_WITHOUT_DATE_CLOTURE",
                        "CRITICAL",
                        session,
                        null,
                        null,
                        "Renseigner la date de clôture après analyse de l'historique technique et d'audit.",
                        "Une session CLOTUREE est dépourvue de dateCloture."
                ));
            }

            if (session.getStatut() == StatutSessionCaisse.VALIDEE_CONTROLE
                    && (session.getDateControle() == null || session.getControleValidePar() == null)) {
                anomalies.add(buildSessionAnomaly(
                        "VALIDATED_CONTROL_SESSION_MISSING_CONTROL_METADATA",
                        "CRITICAL",
                        session,
                        null,
                        null,
                        "Compléter les métadonnées de validation contrôle ou reconstituer la trace via audit.",
                        "Une session VALIDEE_CONTROLE ne porte pas dateControle et/ou controleValidePar."
                ));
            }

            if (hasNonZeroAmount(session.getEcartCaisse()) && isBlank(session.getObservation())) {
                anomalies.add(buildSessionAnomaly(
                        "SESSION_WITH_ECART_WITHOUT_OBSERVATION",
                        "WARNING",
                        session,
                        BigDecimal.ZERO,
                        session.getEcartCaisse(),
                        "Documenter la justification de l'écart avant toute correction manuelle.",
                        "La session présente un écart de caisse mais aucune observation explicative."
                ));
            }
        }
    }

    private void detectDuplicateActiveSessions(
            List<SessionCaisse> sessions,
            List<CaisseHistoryDiagnosticAnomalyDTO> anomalies
    ) {
        Map<Long, List<SessionCaisse>> sessionsByCaisse = sessions.stream()
                .filter(session -> session.getCaisse() != null && session.getCaisse().getId() != null)
                .filter(session -> ACTIVE_SESSION_STATUSES.contains(session.getStatut()))
                .collect(Collectors.groupingBy(session -> session.getCaisse().getId(), LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<Long, List<SessionCaisse>> entry : sessionsByCaisse.entrySet()) {
            if (entry.getValue().size() <= 1) {
                continue;
            }

            SessionCaisse sample = entry.getValue().get(0);
            String sessionIds = entry.getValue().stream()
                    .map(SessionCaisse::getId)
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));

            anomalies.add(CaisseHistoryDiagnosticAnomalyDTO.builder()
                    .anomalyType("MULTIPLE_ACTIVE_SESSIONS_ON_CAISSE")
                    .severity("CRITICAL")
                    .caisseId(entry.getKey())
                    .siteId(extractSiteId(sample.getCaisse()))
                    .currentStatus("ACTIVE")
                    .dateOuverture(sample.getDateOuverture())
                    .recommendation("Analyser les sessions actives concurrentes et clôturer manuellement l'incohérence après validation métier.")
                    .details("Plusieurs sessions actives ont été détectées pour la même caisse. Sessions concernées: [" + sessionIds + "].")
                    .build());
        }
    }

    private void detectDuplicateActiveCaisses(
            List<Caisse> caisses,
            List<CaisseHistoryDiagnosticAnomalyDTO> anomalies
    ) {
        Map<Long, List<Caisse>> activeCaissesBySite = caisses.stream()
                .filter(caisse -> Boolean.TRUE.equals(caisse.getActif()))
                .filter(caisse -> caisse.getSite() != null && caisse.getSite().getId() != null)
                .collect(Collectors.groupingBy(caisse -> caisse.getSite().getId(), LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<Long, List<Caisse>> entry : activeCaissesBySite.entrySet()) {
            if (entry.getValue().size() <= 1) {
                continue;
            }

            String caisseIds = entry.getValue().stream()
                    .map(Caisse::getId)
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));

            anomalies.add(CaisseHistoryDiagnosticAnomalyDTO.builder()
                    .anomalyType("MULTIPLE_ACTIVE_CAISSES_ON_SITE")
                    .severity("WARNING")
                    .siteId(entry.getKey())
                    .currentStatus("ACTIVE")
                    .recommendation("Vérifier si plusieurs caisses actives sur le même site sont attendues par l'organisation cible.")
                    .details("Plusieurs caisses actives ont été détectées sur le même site. Caisses concernées: [" + caisseIds + "].")
                    .build());
        }
    }

    private void detectOperationIntegrityIssues(List<CaisseHistoryDiagnosticAnomalyDTO> anomalies) {
        for (OperationCaisseDiagnosticProjection projection : operationCaisseRepository.findOperationsWithoutSessionReference()) {
            anomalies.add(buildOperationAnomaly(
                    "OPERATION_WITHOUT_SESSION",
                    "CRITICAL",
                    projection,
                    "Rattacher l'opération à une session valide après investigation comptable et audit.",
                    "Une opération historique référence une session nulle."
            ));
        }

        for (OperationCaisseDiagnosticProjection projection : operationCaisseRepository.findOperationsWithMissingSessionReference()) {
            anomalies.add(buildOperationAnomaly(
                    "OPERATION_WITH_MISSING_SESSION_REFERENCE",
                    "CRITICAL",
                    projection,
                    "Reconstituer ou corriger la référence de session après validation manuelle.",
                    "Une opération historique référence une session inexistante en base."
            ));
        }

            if (!hasOperationObservationColumn()) {
                anomalies.add(CaisseHistoryDiagnosticAnomalyDTO.builder()
                    .anomalyType("MODEL_LIMITATION_OPERATION_OBSERVATION_COLUMN_MISSING")
                    .severity("INFO")
                    .recommendation("Ajouter ou réaligner la colonne operation_caisse.observation avant d'exiger ce contrôle sur cette base.")
                    .details("Le schéma réel ne contient pas la colonne operation_caisse.observation ; le contrôle des opérations AJUSTEMENT sans observation n'a pas pu être exécuté.")
                    .build());
                return;
            }

            for (OperationCaisseDiagnosticProjection projection : operationCaisseRepository.findAdjustmentOperationsWithoutObservation()) {
                anomalies.add(buildOperationAnomaly(
                    "ADJUSTMENT_OPERATION_WITHOUT_OBSERVATION",
                    "WARNING",
                    projection,
                    "Compléter l'observation de justification de l'ajustement avant toute action corrective.",
                    "Une opération source AJUSTEMENT est dépourvue d'observation."
                ));
        }
    }

            private boolean hasOperationObservationColumn() {
            Long count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.columns
                where table_schema = database()
                  and table_name = 'operation_caisse'
                  and column_name = 'observation'
                """,
                Long.class
            );
            return count != null && count > 0;
            }

    private CaisseHistoryDiagnosticAnomalyDTO buildSessionAnomaly(
            String anomalyType,
            String severity,
            SessionCaisse session,
            BigDecimal expectedAmount,
            BigDecimal actualAmount,
            String recommendation,
            String details
    ) {
        return CaisseHistoryDiagnosticAnomalyDTO.builder()
                .anomalyType(anomalyType)
                .severity(severity)
                .sessionId(session.getId())
                .caisseId(session.getCaisse() != null ? session.getCaisse().getId() : null)
                .siteId(session.getCaisse() != null ? extractSiteId(session.getCaisse()) : null)
                .expectedAmount(expectedAmount)
                .actualAmount(actualAmount)
                .delta(computeDelta(expectedAmount, actualAmount))
                .currentStatus(session.getStatut() != null ? session.getStatut().name() : null)
                .dateOuverture(session.getDateOuverture())
                .dateCloture(session.getDateCloture())
                .recommendation(recommendation)
                .details(details)
                .build();
    }

    private CaisseHistoryDiagnosticAnomalyDTO buildOperationAnomaly(
            String anomalyType,
            String severity,
            OperationCaisseDiagnosticProjection projection,
            String recommendation,
            String details
    ) {
        return CaisseHistoryDiagnosticAnomalyDTO.builder()
                .anomalyType(anomalyType)
                .severity(severity)
                .sessionId(projection.getSessionId())
                .caisseId(projection.getCaisseId())
                .operationId(projection.getOperationId())
                .siteId(projection.getSiteId())
                .recommendation(recommendation)
                .details(details)
                .build();
    }

    private Long extractSiteId(Caisse caisse) {
        return caisse != null && caisse.getSite() != null ? caisse.getSite().getId() : null;
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private boolean hasNonZeroAmount(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) != 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private BigDecimal computeDelta(BigDecimal expectedAmount, BigDecimal actualAmount) {
        if (expectedAmount == null || actualAmount == null) {
            return null;
        }
        return actualAmount.subtract(expectedAmount);
    }

    private String resolveGeneratedBy() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "ANONYMOUS";
    }
}