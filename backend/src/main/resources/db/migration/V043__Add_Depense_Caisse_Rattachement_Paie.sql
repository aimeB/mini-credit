SET @schema_name := DATABASE();

SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'depense_caisse' AND COLUMN_NAME = 'nature_paiement_paie'
);
SET @sql := IF(@col_exists = 0, 'ALTER TABLE depense_caisse ADD COLUMN nature_paiement_paie VARCHAR(40) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'depense_caisse' AND COLUMN_NAME = 'montant_salaire_du'
);
SET @sql := IF(@col_exists = 0, 'ALTER TABLE depense_caisse ADD COLUMN montant_salaire_du DECIMAL(18,2) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'depense_caisse' AND COLUMN_NAME = 'montant_deja_paye'
);
SET @sql := IF(@col_exists = 0, 'ALTER TABLE depense_caisse ADD COLUMN montant_deja_paye DECIMAL(18,2) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'depense_caisse' AND COLUMN_NAME = 'montant_restant_apres_paiement'
);
SET @sql := IF(@col_exists = 0, 'ALTER TABLE depense_caisse ADD COLUMN montant_restant_apres_paiement DECIMAL(18,2) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'depense_caisse' AND COLUMN_NAME = 'montant_retenue'
);
SET @sql := IF(@col_exists = 0, 'ALTER TABLE depense_caisse ADD COLUMN montant_retenue DECIMAL(18,2) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'depense_caisse' AND COLUMN_NAME = 'motif_retenue'
);
SET @sql := IF(@col_exists = 0, 'ALTER TABLE depense_caisse ADD COLUMN motif_retenue TEXT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'depense_caisse' AND COLUMN_NAME = 'motif_paiement_partiel'
);
SET @sql := IF(@col_exists = 0, 'ALTER TABLE depense_caisse ADD COLUMN motif_paiement_partiel TEXT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'depense_caisse' AND COLUMN_NAME = 'commentaire_paie'
);
SET @sql := IF(@col_exists = 0, 'ALTER TABLE depense_caisse ADD COLUMN commentaire_paie TEXT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;