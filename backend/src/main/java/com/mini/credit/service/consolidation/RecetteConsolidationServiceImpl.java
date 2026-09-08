package com.mini.credit.service.consolidation;

import com.mini.credit.entity.caisse.FicheJournaliereAgentTerrain;
import com.mini.credit.entity.caisse.RecetteJournaliereTerrain;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.StatutFicheJournaliere;
import com.mini.credit.enums.StatutRecetteJournaliere;
import com.mini.credit.enums.TypeRecette;
import com.mini.credit.mapper.FicheJournaliereMapper;
import com.mini.credit.repository.caisse.FicheJournaliereRepository;
import com.mini.credit.repository.caisse.RecetteJournaliereTerrainRepository;
import com.mini.credit.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

/**
 * PHASE 6B.2: Implementation of consolidation service
 *
 * Consolidates RecetteJournaliereTerrain (individual receipts) into
 * FicheJournaliereAgentTerrain (consolidated daily fiches)
 *
 * Scheduled to run at 17:00 every day via @Scheduled(cron="0 0 17 * * ?")
 *
 * Pattern: Idempotent - safe to run multiple times
 */
@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class RecetteConsolidationServiceImpl implements RecetteConsolidationService {

    // Dependencies
    private final RecetteJournaliereTerrainRepository recetteRepository;
    private final FicheJournaliereRepository ficheRepository;
    private final FicheJournaliereMapper ficheMapper;
    private final AuditService auditService;

    // Constants
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    /**
     * STEP 1: Consolidate all receipts from yesterday
     * Called automatically at 17:00 daily
     */
    @Override
    @Scheduled(cron = "0 0 17 * * ?") // 17:00 every day
    public int consolidateYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("🔔 Starting scheduled consolidation for yesterday: {}", yesterday);
        int count = consolidateDate(yesterday);
        log.info("✅ Scheduled consolidation complete: {} fiches created", count);
        return count;
    }

    /**
     * STEP 2: Consolidate receipts for a specific date
     * Calls consolidateDateRange with same date as debut and fin
     */
    @Override
    public int consolidateDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        log.info("📅 Consolidating receipts for date: {}", date);
        return consolidateDateRange(date, date);
    }

    /**
     * STEP 3: Consolidate receipts for a date range (MAIN LOGIC)
     *
     * Algorithm:
     * 1. Find all distinct agents with receipts in date range
     * 2. For each agent and each date:
     *    a. Check if fiche already consolidated (idempotence)
     *    b. Fetch all SOUMISE receipts for agent+date
     *    c. If no receipts, skip this agent+date
     *    d. Aggregate financial totals
     *    e. Aggregate count fields
     *    f. Create FicheJournaliereAgentTerrain in BROUILLON status
     *    g. Link receipts to fiche via FK setter
     *    h. Save fiche to database
     * 3. Return total count of fiches created
     */
    @Override
    public int consolidateDateRange(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null) {
            throw new IllegalArgumentException("Dates cannot be null");
        }
        if (debut.isAfter(fin)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        log.info("🔄 Starting consolidation for date range: {} to {}", debut, fin);

        int totalConsolidatedCount = 0;

        try {
            // STEP 1: Find all distinct agents with receipts in date range
            List<Utilisateur> agents = recetteRepository.findDistinctAgentsByDateRange(debut, fin);
            log.info("📊 Found {} agents with receipts in date range", agents.size());

            if (agents.isEmpty()) {
                log.info("ℹ️ No agents with receipts found for date range {} to {}", debut, fin);
                return 0;
            }

            // STEP 2: For each agent
            for (Utilisateur agent : agents) {
                log.debug("Processing agent: {} (ID: {})", agent.getUsername(), agent.getId());

                // Iterate through each date in range
                LocalDate currentDate = debut;
                while (!currentDate.isAfter(fin)) {
                    try {
                        // STEP 2a: Check if fiche already exists (idempotence)
                        boolean ficheExists = ficheRepository.existsByAgentTerrainIdAndDateFiche(
                            agent.getId(), currentDate);

                        if (ficheExists) {
                            log.debug("⏭️ Fiche already exists for agent {} on {}, skipping",
                                agent.getUsername(), currentDate);
                            currentDate = currentDate.plusDays(1);
                            continue;
                        }

                        // STEP 2b: Fetch all VALIDEE receipts for agent+date (after controller validation)
                        List<RecetteJournaliereTerrain> receipts = recetteRepository
                            .findByAgentTerrainAndDateAndStatut(
                                agent.getId(), currentDate, StatutRecetteJournaliere.VALIDEE);

                        log.debug("Found {} VALIDEE receipts for agent {} on {}",
                            receipts.size(), agent.getUsername(), currentDate);

                        // STEP 2c: If no receipts, skip this agent+date
                        if (receipts.isEmpty()) {
                            log.debug("No VALIDEE receipts found for agent {} on {}",
                                agent.getUsername(), currentDate);
                            currentDate = currentDate.plusDays(1);
                            continue;
                        }

                        // STEP 2d: Aggregate financial totals by classifying montant based on typeRecette
                        BigDecimal epargneTotal = receipts.stream()
                            .filter(r -> r.getTypeRecette() != null &&
                                    (r.getTypeRecette() == com.mini.credit.enums.TypeRecette.DEPOT ||
                                     r.getTypeRecette() == com.mini.credit.enums.TypeRecette.INTERET))
                            .map(RecetteJournaliereTerrain::getMontant)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal remboursementTotal = receipts.stream()
                            .filter(r -> r.getTypeRecette() == com.mini.credit.enums.TypeRecette.REMBOURSEMENT_CREDIT)
                            .map(RecetteJournaliereTerrain::getMontant)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal fraisTotal = receipts.stream()
                            .filter(r -> r.getTypeRecette() == com.mini.credit.enums.TypeRecette.FRAIS)
                            .map(RecetteJournaliereTerrain::getMontant)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal autresTotal = receipts.stream()
                            .filter(r -> r.getTypeRecette() == com.mini.credit.enums.TypeRecette.AUTRE)
                            .map(RecetteJournaliereTerrain::getMontant)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                        log.debug("Aggregated totals - Epargne: {}, Remb: {}, Frais: {}, Autres: {}",
                            epargneTotal, remboursementTotal, fraisTotal, autresTotal);

                        // STEP 2e: Aggregate count fields (dénombrements)
                        // Number of distinct members visited
                        long distinctMembres = receipts.stream()
                            .map(RecetteJournaliereTerrain::getMembre)
                            .distinct()
                            .count();
                        Integer membresVisites = (int) distinctMembres;

                        // For new members: check if dateAdhesion == dateFiche
                        final LocalDate fechaActual = currentDate;  // Make final for use in lambda
                        Integer nouveauxMembres = (int) receipts.stream()
                            .map(RecetteJournaliereTerrain::getMembre)
                            .filter(m -> m != null && m.getDateAdhesion() != null &&
                                    m.getDateAdhesion().equals(fechaActual))
                            .distinct()
                            .count();

                        // Carnets distributed: sum if field exists, otherwise 0
                        Integer carnetsDistribues = 0;  // TBD: clarify source in business requirements

                        log.debug("Aggregated counts - Membres: {}, Nouveaux: {}, Carnets: {}",
                            membresVisites, nouveauxMembres, carnetsDistribues);

                        // STEP 2f: Create FicheJournaliereAgentTerrain in BROUILLON status
                        Site agentSite = agent.getSite();  // agent has single site

                        FicheJournaliereAgentTerrain fiche = FicheJournaliereAgentTerrain.builder()
                            .agentTerrain(agent)
                            .dateFiche(currentDate)
                            .site(agentSite)
                            .epargneCollecteeTotal(epargneTotal)
                            .remboursementCollectes(remboursementTotal)
                            .fraisCollectes(fraisTotal)
                            .autresRecettes(autresTotal)
                            .nombreMembresVisites(membresVisites)
                            .nombreNouveauxMembres(nouveauxMembres)
                            .nombreCarnetsDistribues(carnetsDistribues)
                            .statut(StatutFicheJournaliere.BROUILLON) // IMPORTANT: starts BROUILLON
                            // NOTE: Do NOT set .recettes() here — bidirectional @Data causes StackOverflow
                            .build();

                        // STEP 2g: Save fiche first to get ID, then link receipts via owning side
                        FicheJournaliereAgentTerrain savedFiche = ficheRepository.save(fiche);

                        // Link receipts back to consolidated fiche via FK setter (owning side)
                        receipts.forEach(r -> r.setFicheJournaliere(savedFiche));
                        // Receipts are managed entities — dirty checking will persist FK update

                        totalConsolidatedCount++;

                        log.info("✅ Created consolidated fiche for agent {} on {} ({} receipts)",
                            agent.getUsername(), currentDate, receipts.size());

                        // Audit logging (simple logging for now)
                        log.debug("AUDIT: Consolidated {} receipts for agent {} (ID: {}) on {}",
                            receipts.size(), agent.getUsername(), agent.getId(), currentDate);

                    } catch (Exception e) {
                        log.error("Error consolidating for agent {} on {}: {}",
                            agent.getUsername(), currentDate, e.getMessage(), e);
                        // Continue with next date despite error
                    }

                    currentDate = currentDate.plusDays(1);
                }

            }

        } catch (Exception e) {
            log.error("Fatal error during consolidation: {}", e.getMessage(), e);
            throw new RuntimeException("Consolidation failed: " + e.getMessage(), e);
        }

        log.info("✅ Consolidation complete for date range {} to {}: {} fiches created",
            debut, fin, totalConsolidatedCount);

        return totalConsolidatedCount;
    }
}
