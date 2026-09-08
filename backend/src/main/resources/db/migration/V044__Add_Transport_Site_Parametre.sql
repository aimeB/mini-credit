SET @schema_name := DATABASE();

CREATE TABLE IF NOT EXISTS transport_site_parametre (
    id BIGINT NOT NULL AUTO_INCREMENT,
    site_id BIGINT NOT NULL,
    montant_transport_mensuel DECIMAL(18,2) NOT NULL,
    actif BIT(1) NOT NULL DEFAULT b'1',
    date_debut_validite DATE NOT NULL,
    date_fin_validite DATE NULL,
    commentaire TEXT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    date_creation DATETIME NOT NULL,
    date_modification DATETIME NULL,
    PRIMARY KEY (id),
    INDEX idx_transport_site_param_site (site_id),
    INDEX idx_transport_site_param_actif (actif),
    CONSTRAINT fk_transport_site_param_site FOREIGN KEY (site_id) REFERENCES site(id)
);

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'depense_caisse' AND COLUMN_NAME = 'periode_charge');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE depense_caisse ADD COLUMN periode_charge VARCHAR(7) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'depense_caisse' AND COLUMN_NAME = 'type_charge_fixe');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE depense_caisse ADD COLUMN type_charge_fixe VARCHAR(40) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'depense_caisse' AND COLUMN_NAME = 'site_charge_id');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE depense_caisse ADD COLUMN site_charge_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'depense_caisse' AND COLUMN_NAME = 'montant_charge_fixe_reference');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE depense_caisse ADD COLUMN montant_charge_fixe_reference DECIMAL(18,2) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'depense_caisse' AND COLUMN_NAME = 'montant_ecart_charge_fixe');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE depense_caisse ADD COLUMN montant_ecart_charge_fixe DECIMAL(18,2) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'depense_caisse' AND COLUMN_NAME = 'commentaire_rapprochement');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE depense_caisse ADD COLUMN commentaire_rapprochement TEXT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'depense_caisse' AND CONSTRAINT_NAME = 'fk_depense_caisse_site_charge');
SET @sql := IF(@fk_exists = 0, 'ALTER TABLE depense_caisse ADD CONSTRAINT fk_depense_caisse_site_charge FOREIGN KEY (site_charge_id) REFERENCES site(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;