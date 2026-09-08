-- Extension depense_caisse: bénéficiaire structuré pour les dépenses salaire.
-- Migration défensive MySQL 5.7.

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'depense_caisse'
     AND column_name = 'beneficiaire_id') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN beneficiaire_id BIGINT NULL AFTER beneficiaire',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'depense_caisse'
     AND column_name = 'beneficiaire_nom') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN beneficiaire_nom VARCHAR(150) NULL AFTER beneficiaire_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'depense_caisse'
     AND column_name = 'beneficiaire_role') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN beneficiaire_role VARCHAR(80) NULL AFTER beneficiaire_nom',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'depense_caisse'
     AND column_name = 'beneficiaire_agence') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN beneficiaire_agence VARCHAR(150) NULL AFTER beneficiaire_role',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE table_schema = DATABASE()
     AND table_name = 'depense_caisse'
     AND constraint_name = 'fk_depense_caisse_beneficiaire') = 0,
  'ALTER TABLE depense_caisse ADD CONSTRAINT fk_depense_caisse_beneficiaire FOREIGN KEY (beneficiaire_id) REFERENCES utilisateur(id) ON DELETE SET NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'depense_caisse'
     AND index_name = 'idx_depense_caisse_beneficiaire_id') = 0,
  'CREATE INDEX idx_depense_caisse_beneficiaire_id ON depense_caisse(beneficiaire_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
