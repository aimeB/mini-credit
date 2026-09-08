-- PATCH P1: Workflow strict de session caisse + unicité session active
-- Etats cibles: OUVERTE -> PRE_CLOTUREE -> VALIDEE_CONTROLE -> CLOTUREE

-- Harmoniser les données existantes: les anciennes sessions FERMEE deviennent CLOTUREE
UPDATE session_caisse
SET statut = 'CLOTUREE',
    date_modification = NOW()
WHERE statut = 'FERMEE';

DROP TRIGGER IF EXISTS trg_session_caisse_single_active_insert;
DROP TRIGGER IF EXISTS trg_session_caisse_single_active_update;

DELIMITER $$

CREATE TRIGGER trg_session_caisse_single_active_insert
BEFORE INSERT ON session_caisse
FOR EACH ROW
BEGIN
    IF NEW.statut IN ('OUVERTE', 'PRE_CLOTUREE', 'VALIDEE_CONTROLE') THEN
        IF EXISTS (
            SELECT 1
            FROM session_caisse sc
            WHERE sc.caisse_id = NEW.caisse_id
              AND sc.statut IN ('OUVERTE', 'PRE_CLOTUREE', 'VALIDEE_CONTROLE')
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Une session active existe deja pour cette caisse';
        END IF;

        IF EXISTS (
            SELECT 1
            FROM session_caisse sc
            WHERE sc.utilisateur_id = NEW.utilisateur_id
              AND sc.statut IN ('OUVERTE', 'PRE_CLOTUREE', 'VALIDEE_CONTROLE')
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Cet utilisateur a deja une session active';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_session_caisse_single_active_update
BEFORE UPDATE ON session_caisse
FOR EACH ROW
BEGIN
    IF NEW.statut IN ('OUVERTE', 'PRE_CLOTUREE', 'VALIDEE_CONTROLE') THEN
        IF EXISTS (
            SELECT 1
            FROM session_caisse sc
            WHERE sc.caisse_id = NEW.caisse_id
              AND sc.id <> NEW.id
              AND sc.statut IN ('OUVERTE', 'PRE_CLOTUREE', 'VALIDEE_CONTROLE')
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Une session active existe deja pour cette caisse';
        END IF;

        IF EXISTS (
            SELECT 1
            FROM session_caisse sc
            WHERE sc.utilisateur_id = NEW.utilisateur_id
              AND sc.id <> NEW.id
              AND sc.statut IN ('OUVERTE', 'PRE_CLOTUREE', 'VALIDEE_CONTROLE')
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Cet utilisateur a deja une session active';
        END IF;
    END IF;
END$$

DELIMITER ;

SELECT 'V018 OK' AS migration_status;
