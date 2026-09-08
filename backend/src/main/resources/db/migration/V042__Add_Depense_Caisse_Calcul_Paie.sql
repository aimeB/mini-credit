-- Extension depense_caisse: détail nullable du calcul paie personnel.
-- Migration défensive MySQL 5.7, sans modification des dépenses historiques.

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'depense_caisse' AND column_name = 'epargne_collectee_reference') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN epargne_collectee_reference DECIMAL(18,2) NULL AFTER motif_ecart_remuneration', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'depense_caisse' AND column_name = 'remboursement_collecte_reference') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN remboursement_collecte_reference DECIMAL(18,2) NULL AFTER epargne_collectee_reference', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'depense_caisse' AND column_name = 'nombre_carnets_vendus') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN nombre_carnets_vendus INT NULL AFTER remboursement_collecte_reference', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'depense_caisse' AND column_name = 'prime_mobilisation_epargne') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN prime_mobilisation_epargne DECIMAL(18,2) NULL AFTER nombre_carnets_vendus', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'depense_caisse' AND column_name = 'prime_mobilisation_remboursement') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN prime_mobilisation_remboursement DECIMAL(18,2) NULL AFTER prime_mobilisation_epargne', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'depense_caisse' AND column_name = 'bonus_carnets') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN bonus_carnets DECIMAL(18,2) NULL AFTER prime_mobilisation_remboursement', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'depense_caisse' AND column_name = 'prime_motivation_manuelle') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN prime_motivation_manuelle DECIMAL(18,2) NULL AFTER bonus_carnets', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'depense_caisse' AND column_name = 'motif_prime_motivation_manuelle') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN motif_prime_motivation_manuelle TEXT NULL AFTER prime_motivation_manuelle', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'depense_caisse' AND column_name = 'mode_calcul_paie') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN mode_calcul_paie VARCHAR(40) NULL AFTER motif_prime_motivation_manuelle', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'depense_caisse' AND column_name = 'detail_calcul_paie_json') = 0,
  'ALTER TABLE depense_caisse ADD COLUMN detail_calcul_paie_json LONGTEXT NULL AFTER mode_calcul_paie', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;