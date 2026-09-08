ALTER TABLE audit_logs
    ADD COLUMN module VARCHAR(50) NULL AFTER action,
    ADD COLUMN user_role VARCHAR(50) NULL AFTER role_code,
    ADD COLUMN site_id BIGINT NULL AFTER user_id,
    ADD COLUMN site_libelle VARCHAR(150) NULL AFTER site_id,
    ADD COLUMN caisse_id BIGINT NULL AFTER site_libelle,
    ADD COLUMN session_caisse_id BIGINT NULL AFTER caisse_id,
    ADD COLUMN date_action DATETIME NULL AFTER session_caisse_id,
    ADD COLUMN old_value TEXT NULL AFTER old_values_json,
    ADD COLUMN new_value TEXT NULL AFTER new_values_json,
    ADD COLUMN commentaire VARCHAR(1000) NULL AFTER reason,
    ADD COLUMN severity VARCHAR(20) NULL AFTER commentaire,
    ADD COLUMN reference_metier VARCHAR(120) NULL AFTER reference_number;

UPDATE audit_logs
SET date_action = COALESCE(date_action, date_creation)
WHERE date_action IS NULL;

UPDATE audit_logs
SET commentaire = reason
WHERE commentaire IS NULL AND reason IS NOT NULL;

UPDATE audit_logs
SET reference_metier = reference_number
WHERE reference_metier IS NULL AND reference_number IS NOT NULL;

UPDATE audit_logs
SET old_value = old_values_json
WHERE old_value IS NULL AND old_values_json IS NOT NULL;

UPDATE audit_logs
SET new_value = new_values_json
WHERE new_value IS NULL AND new_values_json IS NOT NULL;

CREATE INDEX idx_audit_logs_date_action ON audit_logs(date_action);
CREATE INDEX idx_audit_logs_module ON audit_logs(module);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_severity ON audit_logs(severity);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_site_id ON audit_logs(site_id);
CREATE INDEX idx_audit_logs_caisse_id ON audit_logs(caisse_id);
CREATE INDEX idx_audit_logs_session_caisse_id ON audit_logs(session_caisse_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_reference_metier ON audit_logs(reference_metier);
