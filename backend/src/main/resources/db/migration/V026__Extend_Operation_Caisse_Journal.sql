-- LOT 5: Journal caisse ameliore
-- IMPORTANT: migration defensive pour MySQL 5.7 (pas de ADD COLUMN IF NOT EXISTS)
-- Ajoute uniquement les colonnes/index manquants dans operation_caisse.

-- ========== Colonnes ==========

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'operation_caisse'
     AND column_name = 'solde_apres_operation') = 0,
  'ALTER TABLE operation_caisse ADD COLUMN solde_apres_operation DECIMAL(18,2) NULL AFTER montant',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'operation_caisse'
     AND column_name = 'depense_caisse_id') = 0,
  'ALTER TABLE operation_caisse ADD COLUMN depense_caisse_id BIGINT NULL AFTER recette_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'operation_caisse'
     AND column_name = 'retrait_epargne_id') = 0,
  'ALTER TABLE operation_caisse ADD COLUMN retrait_epargne_id BIGINT NULL AFTER credit_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'operation_caisse'
     AND column_name = 'reference_metier') = 0,
  'ALTER TABLE operation_caisse ADD COLUMN reference_metier VARCHAR(120) NULL AFTER reference_externe',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'operation_caisse'
     AND column_name = 'utilisateur_id') = 0,
  'ALTER TABLE operation_caisse ADD COLUMN utilisateur_id BIGINT NULL AFTER created_by',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'operation_caisse'
     AND column_name = 'role_utilisateur') = 0,
  'ALTER TABLE operation_caisse ADD COLUMN role_utilisateur VARCHAR(50) NULL AFTER utilisateur_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'operation_caisse'
     AND column_name = 'site_id') = 0,
  'ALTER TABLE operation_caisse ADD COLUMN site_id BIGINT NULL AFTER caisse_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'operation_caisse'
     AND column_name = 'commentaire') = 0,
  'ALTER TABLE operation_caisse ADD COLUMN commentaire TEXT NULL AFTER observation',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== Backfill minimal compatibilite ==========
UPDATE operation_caisse
SET reference_metier = reference_externe
WHERE reference_metier IS NULL
  AND reference_externe IS NOT NULL;

UPDATE operation_caisse
SET utilisateur_id = created_by
WHERE utilisateur_id IS NULL
  AND created_by IS NOT NULL;

UPDATE operation_caisse
SET commentaire = COALESCE(observation, description)
WHERE commentaire IS NULL
  AND (observation IS NOT NULL OR description IS NOT NULL);

UPDATE operation_caisse oc
JOIN caisse c ON c.id = oc.caisse_id
SET oc.site_id = c.site_id
WHERE oc.site_id IS NULL
  AND c.site_id IS NOT NULL;

-- ========== Contraintes FK defensives ==========

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE()
     AND table_name = 'operation_caisse'
     AND constraint_name = 'fk_operation_caisse_depense_caisse') = 0,
  'ALTER TABLE operation_caisse ADD CONSTRAINT fk_operation_caisse_depense_caisse FOREIGN KEY (depense_caisse_id) REFERENCES depense_caisse(id) ON DELETE SET NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE()
     AND table_name = 'operation_caisse'
     AND constraint_name = 'fk_operation_caisse_retrait_epargne') = 0,
  'ALTER TABLE operation_caisse ADD CONSTRAINT fk_operation_caisse_retrait_epargne FOREIGN KEY (retrait_epargne_id) REFERENCES demande_retrait_epargne(id) ON DELETE SET NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE()
     AND table_name = 'operation_caisse'
     AND constraint_name = 'fk_operation_caisse_utilisateur') = 0,
  'ALTER TABLE operation_caisse ADD CONSTRAINT fk_operation_caisse_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE SET NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE constraint_schema = DATABASE()
     AND table_name = 'operation_caisse'
     AND constraint_name = 'fk_operation_caisse_site') = 0,
  'ALTER TABLE operation_caisse ADD CONSTRAINT fk_operation_caisse_site FOREIGN KEY (site_id) REFERENCES site(id) ON DELETE SET NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== Index ==========

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'operation_caisse' AND index_name = 'idx_operation_caisse_session_caisse_id') = 0,
  'CREATE INDEX idx_operation_caisse_session_caisse_id ON operation_caisse(session_caisse_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'operation_caisse' AND index_name = 'idx_operation_caisse_caisse_id') = 0,
  'CREATE INDEX idx_operation_caisse_caisse_id ON operation_caisse(caisse_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'operation_caisse' AND index_name = 'idx_operation_caisse_site_id') = 0,
  'CREATE INDEX idx_operation_caisse_site_id ON operation_caisse(site_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'operation_caisse' AND index_name = 'idx_operation_caisse_utilisateur_id') = 0,
  'CREATE INDEX idx_operation_caisse_utilisateur_id ON operation_caisse(utilisateur_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'operation_caisse' AND index_name = 'idx_operation_caisse_type') = 0,
  'CREATE INDEX idx_operation_caisse_type ON operation_caisse(type_operation)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'operation_caisse' AND index_name = 'idx_operation_caisse_categorie') = 0,
  'CREATE INDEX idx_operation_caisse_categorie ON operation_caisse(categorie_operation)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'operation_caisse' AND index_name = 'idx_operation_caisse_source_operation') = 0,
  'CREATE INDEX idx_operation_caisse_source_operation ON operation_caisse(source_operation)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'operation_caisse' AND index_name = 'idx_operation_caisse_date_operation') = 0,
  'CREATE INDEX idx_operation_caisse_date_operation ON operation_caisse(date_operation)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'operation_caisse' AND index_name = 'idx_operation_caisse_depense_caisse_id') = 0,
  'CREATE INDEX idx_operation_caisse_depense_caisse_id ON operation_caisse(depense_caisse_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'operation_caisse' AND index_name = 'idx_operation_caisse_credit_id') = 0,
  'CREATE INDEX idx_operation_caisse_credit_id ON operation_caisse(credit_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'operation_caisse' AND index_name = 'idx_operation_caisse_retrait_epargne_id') = 0,
  'CREATE INDEX idx_operation_caisse_retrait_epargne_id ON operation_caisse(retrait_epargne_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'operation_caisse' AND index_name = 'idx_operation_caisse_recette_id') = 0,
  'CREATE INDEX idx_operation_caisse_recette_id ON operation_caisse(recette_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT 'V026 OK' AS migration_status;