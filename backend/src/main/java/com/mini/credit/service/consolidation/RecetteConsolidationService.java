package com.mini.credit.service.consolidation;

import java.time.LocalDate;

/**
 * PHASE 6B.2: Service for consolidating daily receipts into agent fiches
 *
 * Consolidation Flow:
 * 1. Find all agents with SOUMISE receipts on given date(s)
 * 2. For each agent:
 *    - Check if fiche already exists (idempotence)
 *    - Fetch all SOUMISE receipts
 *    - Aggregate financial totals (epargne, remboursement, frais, autres)
 *    - Aggregate counts (membres visités, nouveaux, carnets)
 *    - Create FicheJournaliereAgentTerrain in BROUILLON status
 *    - Link receipts to fiche via FK setter
 *    - Save to database
 *
 * Scheduling:
 * - consolidateYesterday() called automatically at 17:00 daily (via @Scheduled)
 * - consolidateDate() and consolidateDateRange() can be called manually (admin only)
 *
 * Key Pattern: Idempotent - can be run multiple times without creating duplicates
 */
public interface RecetteConsolidationService {

    /**
     * Consolidate all receipts from yesterday into daily fiches
     * Called automatically at 17:00 every day by @Scheduled(cron="0 0 17 * * ?")
     *
     * @return number of fiches created
     */
    int consolidateYesterday();

    /**
     * Consolidate all receipts from a specific date
     * Called manually for backfill or manual correction (admin only)
     *
     * @param date date to consolidate (e.g., LocalDate.of(2026, 6, 5))
     * @return number of fiches created
     */
    int consolidateDate(LocalDate date);

    /**
     * Consolidate all receipts between two dates (inclusive)
     * Called manually for date range consolidation (admin only)
     *
     * Example: consolidateDateRange(
     *   LocalDate.of(2026, 6, 1),
     *   LocalDate.of(2026, 6, 7)
     * ) // Consolidates entire week
     *
     * @param debut start date (inclusive)
     * @param fin end date (inclusive)
     * @return total number of fiches created across all dates
     */
    int consolidateDateRange(LocalDate debut, LocalDate fin);
}
