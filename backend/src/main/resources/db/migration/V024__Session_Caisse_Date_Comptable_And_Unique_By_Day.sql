-- PATCH 3N: session caisse strictement journaliere
-- 1 session = 1 caisse + 1 date comptable

ALTER TABLE session_caisse
    ADD COLUMN date_comptable DATE NULL AFTER date_controle;

UPDATE session_caisse
SET date_comptable = DATE(date_ouverture)
WHERE date_comptable IS NULL;

ALTER TABLE session_caisse
    MODIFY COLUMN date_comptable DATE NOT NULL;

CREATE INDEX idx_session_caisse_date_comptable
    ON session_caisse(date_comptable);

CREATE INDEX idx_session_caisse_statut
    ON session_caisse(statut);

CREATE INDEX idx_session_caisse_caisse_id
    ON session_caisse(caisse_id);

CREATE INDEX idx_session_caisse_caisse_statut_date
    ON session_caisse(caisse_id, statut, date_comptable);

ALTER TABLE session_caisse
    ADD CONSTRAINT uk_session_caisse_caisse_date_comptable
    UNIQUE (caisse_id, date_comptable);

SELECT 'V024 OK' AS migration_status;