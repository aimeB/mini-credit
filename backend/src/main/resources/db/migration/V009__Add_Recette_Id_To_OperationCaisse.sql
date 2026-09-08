-- PHASE 6B.2: Add recette_id column to operation_caisse for traceability
-- This column links OperationCaisse to the RecetteJournaliereTerrain that triggered auto-generation

ALTER TABLE operation_caisse 
ADD COLUMN recette_id BIGINT NULL,
ADD KEY idx_operation_caisse_recette (recette_id),
ADD CONSTRAINT fk_operation_caisse_recette 
  FOREIGN KEY (recette_id) 
  REFERENCES recette_journaliere_terrain(id) 
  ON DELETE SET NULL;

-- Index for fast lookups by recette
CREATE INDEX idx_operation_caisse_by_recette_id ON operation_caisse(recette_id);
