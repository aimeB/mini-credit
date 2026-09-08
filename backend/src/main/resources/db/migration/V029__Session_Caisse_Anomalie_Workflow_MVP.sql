-- MVP Anomalie session caisse
-- 1) Champs de correction sur session_caisse
ALTER TABLE session_caisse
    ADD COLUMN motif_annulation VARCHAR(1000) NULL AFTER observation,
    ADD COLUMN annulee_par_id BIGINT NULL AFTER motif_annulation,
    ADD COLUMN date_annulation DATETIME NULL AFTER annulee_par_id,
    ADD COLUMN statut_correction VARCHAR(80) NULL AFTER date_annulation,
    ADD COLUMN commentaire_correction VARCHAR(1000) NULL AFTER statut_correction;

ALTER TABLE session_caisse
    ADD CONSTRAINT fk_session_caisse_annulee_par
    FOREIGN KEY (annulee_par_id) REFERENCES utilisateur(id);

-- 2) Table des anomalies de session
CREATE TABLE session_caisse_anomalie (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    type_anomalie VARCHAR(80) NOT NULL,
    ancien_statut VARCHAR(40) NOT NULL,
    nouveau_statut VARCHAR(40) NULL,
    motif VARCHAR(1000) NOT NULL,
    commentaire VARCHAR(1000) NULL,
    demande_par_id BIGINT NULL,
    valide_par_id BIGINT NULL,
    date_demande DATETIME NOT NULL,
    date_validation DATETIME NULL,
    statut_dossier VARCHAR(30) NOT NULL,
    action_executee VARCHAR(120) NULL,
    audit_id BIGINT NULL,
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification DATETIME NULL,
    CONSTRAINT fk_sca_session FOREIGN KEY (session_id) REFERENCES session_caisse(id),
    CONSTRAINT fk_sca_demande_par FOREIGN KEY (demande_par_id) REFERENCES utilisateur(id),
    CONSTRAINT fk_sca_valide_par FOREIGN KEY (valide_par_id) REFERENCES utilisateur(id)
);

CREATE INDEX idx_sca_session ON session_caisse_anomalie(session_id);
CREATE INDEX idx_sca_type ON session_caisse_anomalie(type_anomalie);
CREATE INDEX idx_sca_statut_dossier ON session_caisse_anomalie(statut_dossier);
CREATE INDEX idx_sca_date_demande ON session_caisse_anomalie(date_demande);

-- 3) Adapter la règle d'unicité caisse + date_comptable
-- L'index unique historique bloque toute réouverture après annulation.
ALTER TABLE session_caisse DROP INDEX uk_session_caisse_caisse_date_comptable;

DROP TRIGGER IF EXISTS trg_session_caisse_single_day_insert;
DROP TRIGGER IF EXISTS trg_session_caisse_single_day_update;

DELIMITER $$

CREATE TRIGGER trg_session_caisse_single_day_insert
BEFORE INSERT ON session_caisse
FOR EACH ROW
BEGIN
    IF NEW.statut IN ('OUVERTE', 'PRE_CLOTUREE', 'VALIDEE_CONTROLE', 'CLOTUREE') THEN
        IF EXISTS (
            SELECT 1
            FROM session_caisse s
            WHERE s.caisse_id = NEW.caisse_id
              AND s.date_comptable = NEW.date_comptable
              AND s.statut IN ('OUVERTE', 'PRE_CLOTUREE', 'VALIDEE_CONTROLE', 'CLOTUREE')
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Une session existe deja pour cette caisse a la date comptable du jour';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_session_caisse_single_day_update
BEFORE UPDATE ON session_caisse
FOR EACH ROW
BEGIN
    IF NEW.statut IN ('OUVERTE', 'PRE_CLOTUREE', 'VALIDEE_CONTROLE', 'CLOTUREE') THEN
        IF EXISTS (
            SELECT 1
            FROM session_caisse s
            WHERE s.caisse_id = NEW.caisse_id
              AND s.date_comptable = NEW.date_comptable
              AND s.id <> NEW.id
              AND s.statut IN ('OUVERTE', 'PRE_CLOTUREE', 'VALIDEE_CONTROLE', 'CLOTUREE')
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Une session existe deja pour cette caisse a la date comptable du jour';
        END IF;
    END IF;
END$$

DELIMITER ;
