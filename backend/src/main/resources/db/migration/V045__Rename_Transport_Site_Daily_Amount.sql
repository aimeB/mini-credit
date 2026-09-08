SET @schema_name := DATABASE();

SET @old_col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'transport_site_parametre' AND COLUMN_NAME = 'montant_transport_mensuel');
SET @new_col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'transport_site_parametre' AND COLUMN_NAME = 'montant_transport_journalier_par_agent');
SET @sql := IF(@old_col_exists = 1 AND @new_col_exists = 0,
    'ALTER TABLE transport_site_parametre CHANGE COLUMN montant_transport_mensuel montant_transport_journalier_par_agent DECIMAL(18,2) NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @new_col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'transport_site_parametre' AND COLUMN_NAME = 'montant_transport_journalier_par_agent');
SET @sql := IF(@new_col_exists = 0,
    'ALTER TABLE transport_site_parametre ADD COLUMN montant_transport_journalier_par_agent DECIMAL(18,2) NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;