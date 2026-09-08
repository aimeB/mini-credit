-- PATCH P1: Une seule caisse active par site

DROP TRIGGER IF EXISTS trg_caisse_single_active_site_insert;
DROP TRIGGER IF EXISTS trg_caisse_single_active_site_update;

DELIMITER $$

CREATE TRIGGER trg_caisse_single_active_site_insert
BEFORE INSERT ON caisse
FOR EACH ROW
BEGIN
    IF NEW.actif = TRUE THEN
        IF EXISTS (
            SELECT 1
            FROM caisse c
            WHERE c.site_id = NEW.site_id
              AND c.actif = TRUE
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Une caisse active existe deja pour ce site';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_caisse_single_active_site_update
BEFORE UPDATE ON caisse
FOR EACH ROW
BEGIN
    IF NEW.actif = TRUE THEN
        IF EXISTS (
            SELECT 1
            FROM caisse c
            WHERE c.site_id = NEW.site_id
              AND c.id <> NEW.id
              AND c.actif = TRUE
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Une caisse active existe deja pour ce site';
        END IF;
    END IF;
END$$

DELIMITER ;

SELECT 'V019 OK' AS migration_status;
