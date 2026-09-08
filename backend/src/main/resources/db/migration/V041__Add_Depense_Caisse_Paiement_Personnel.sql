-- Extension depense_caisse: rattachement employe et contrôle rémunération pour paiements personnel.
-- Migration défensive MySQL 5.7, champs nullable pour préserver l'historique.

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'depense_caisse'
     AND column_name = 'employe_id') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN employe_id BIGINT NULL AFTER beneficiaire_agence',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'depense_caisse'
     AND column_name = 'periode_paie') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN periode_paie VARCHAR(7) NULL AFTER employe_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'depense_caisse'
     AND column_name = 'type_paiement_personnel') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN type_paiement_personnel VARCHAR(30) NULL AFTER periode_paie',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'depense_caisse'
     AND column_name = 'montant_remuneration_reference') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN montant_remuneration_reference DECIMAL(18,2) NULL AFTER type_paiement_personnel',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'depense_caisse'
     AND column_name = 'montant_ecart_remuneration') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN montant_ecart_remuneration DECIMAL(18,2) NULL AFTER montant_remuneration_reference',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'depense_caisse'
     AND column_name = 'motif_ecart_remuneration') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN motif_ecart_remuneration TEXT NULL AFTER montant_ecart_remuneration',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
   WHERE table_schema = DATABASE()
     AND table_name = 'depense_caisse'
     AND constraint_name = 'fk_depense_caisse_employe') = 0,
  'ALTER TABLE depense_caisse ADD CONSTRAINT fk_depense_caisse_employe FOREIGN KEY (employe_id) REFERENCES employe(id) ON DELETE SET NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'depense_caisse'
     AND index_name = 'idx_depense_caisse_employe_paie') = 0,
  'CREATE INDEX idx_depense_caisse_employe_paie ON depense_caisse(employe_id, periode_paie, type_paiement_personnel, statut)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
