-- PATCH 1B: Add source_operation and reference_externe columns to operation_caisse
-- Implements Q3 traceability: each operation has a source (RECETTE_JOURNALIERE, CREDIT_DECAISSEMENT, etc.)
-- and optional external reference for non-receipt sources.
-- 
-- Phase 6B.2 Preservation: recette_id remains NULLABLE
-- recette_id is MANDATORY ONLY if source_operation = 'RECETTE_JOURNALIERE' (enforced at application level)
-- 
-- Created: 5 Juin 2026

-- STEP 1: Add columns (initially nullable for backward compatibility)
ALTER TABLE operation_caisse 
ADD COLUMN source_operation VARCHAR(50) NULL AFTER recette_id,
ADD COLUMN reference_externe VARCHAR(100) NULL AFTER source_operation;

-- STEP 2: Initialize source_operation for existing rows
-- Strategy:
--   - If recette_id is NOT NULL → source was RECETTE_JOURNALIERE (from V009)
--   - Otherwise → assign LEGACY (placeholder for pre-Phase6C operations)
-- This ensures backward compatibility with existing data
UPDATE operation_caisse 
SET source_operation = CASE 
    WHEN recette_id IS NOT NULL THEN 'RECETTE_JOURNALIERE'
    ELSE 'LEGACY'
END;

-- STEP 3: Make source_operation NOT NULL after initialization
ALTER TABLE operation_caisse 
MODIFY COLUMN source_operation VARCHAR(50) NOT NULL;

-- STEP 4: Create indices for performance (Q3 traçabilité)
CREATE INDEX idx_operation_caisse_source 
ON operation_caisse(source_operation);

CREATE INDEX idx_operation_caisse_reference 
ON operation_caisse(reference_externe);

-- STEP 5: Document enum values in migration comment
-- SourceOperationCaisse enum values:
--   RECETTE_JOURNALIERE: Operation generated from RecetteJournaliereTerrain.valider()
--   RETRAIT_EPARGNE: Épargne withdrawal operation
--   CREDIT_DECAISSEMENT: Credit disbursement operation
--   CREDIT_REMBOURSEMENT: Credit repayment operation
--   MANUEL: Manual entry by CAISSIER
--   AJUSTEMENT: Adjustment/reconciliation operation
--   APPROVISIONNEMENT: Cash supply/funding
--   AUTRE: Other/uncategorized
--   LEGACY: Pre-Phase6C operations (no source specified)

-- Constraints validation (enforced at application level, not DB):
-- 1. If source_operation = 'RECETTE_JOURNALIERE': recette_id MUST NOT NULL
-- 2. If source_operation IN ('CREDIT_DECAISSEMENT', 'CREDIT_REMBOURSEMENT'): credit_id MUST NOT NULL
-- 3. If source_operation IN ('RETRAIT_EPARGNE'): operation_epargne_id MUST NOT NULL
-- 4. If source_operation = 'MANUEL': reference_externe SHOULD be populated
-- 5. Audit trail: createdBy, dateCreated (from BaseEntity) + source_operation form complete traçabilité
