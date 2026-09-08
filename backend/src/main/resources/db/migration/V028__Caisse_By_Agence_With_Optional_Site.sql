-- P6B.2: rattachement principal des caisses a l'agence (site optionnel)

-- 1) Ajouter la colonne agence_id
ALTER TABLE caisse
    ADD COLUMN agence_id BIGINT NULL AFTER libelle;

-- 2) Backfill depuis le site (cas nominal)
UPDATE caisse c
JOIN site s ON s.id = c.site_id
SET c.agence_id = s.agence_id
WHERE c.agence_id IS NULL
  AND s.agence_id IS NOT NULL;

-- 3) Backfill depuis le caissier responsable (fallback)
UPDATE caisse c
JOIN utilisateur u ON u.id = c.caissier_responsable_id
JOIN employe e ON e.id = u.employe_id
SET c.agence_id = e.agence_id
WHERE c.agence_id IS NULL
  AND e.agence_id IS NOT NULL;

-- 4) Fallback final sur la premiere agence active
UPDATE caisse c
SET c.agence_id = (
    SELECT a.id
    FROM agence a
    WHERE a.actif = TRUE
    ORDER BY a.id
    LIMIT 1
)
WHERE c.agence_id IS NULL;

-- 5) Contraintes et index
ALTER TABLE caisse
    MODIFY COLUMN agence_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_caisse_agence FOREIGN KEY (agence_id) REFERENCES agence(id);

CREATE INDEX idx_caisse_agence_id ON caisse(agence_id);

-- 6) Remplacer la regle "une caisse active par site" par "une caisse active par agence"
DROP TRIGGER IF EXISTS trg_caisse_single_active_site_insert;
DROP TRIGGER IF EXISTS trg_caisse_single_active_site_update;
DROP TRIGGER IF EXISTS trg_caisse_single_active_agence_insert;
DROP TRIGGER IF EXISTS trg_caisse_single_active_agence_update;

DELIMITER $$

CREATE TRIGGER trg_caisse_single_active_agence_insert
BEFORE INSERT ON caisse
FOR EACH ROW
BEGIN
    IF NEW.actif = TRUE THEN
        IF EXISTS (
            SELECT 1
            FROM caisse c
            WHERE c.agence_id = NEW.agence_id
              AND c.actif = TRUE
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Une caisse active existe deja pour cette agence';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_caisse_single_active_agence_update
BEFORE UPDATE ON caisse
FOR EACH ROW
BEGIN
    IF NEW.actif = TRUE THEN
        IF EXISTS (
            SELECT 1
            FROM caisse c
            WHERE c.agence_id = NEW.agence_id
              AND c.id <> NEW.id
              AND c.actif = TRUE
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Une caisse active existe deja pour cette agence';
        END IF;
    END IF;
END$$

DELIMITER ;

SELECT 'V028 OK' AS migration_status;
