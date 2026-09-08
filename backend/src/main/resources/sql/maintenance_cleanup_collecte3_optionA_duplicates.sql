-- =============================================================
-- MAINTENANCE LOCALE/TEST - Nettoyage doublons caisse collecte #3
-- -------------------------------------------------------------
-- Contexte:
--   Après correction Option A, des écritures détaillées historiques restent
--   visibles dans l'historique caisse pour la collecte #3.
--
-- Objectif:
--   1) Conserver UNIQUEMENT l'écriture globale de retour terrain:
--      - source_operation = 'RECETTE_JOURNALIERE'
--      - montant = 51000
--      - description like 'Retour terrain collecte #3%'
--   2) Supprimer uniquement les doublons historiques détaillés:
--      - 50000 | Mouvement epargne automatique / Epargne #1
--      - 1000  | Collecte validee #3 - CARNET
--   3) Recalculer total_entrees / total_sorties / solde_theorique de la session.
--
-- IMPORTANT:
--   - Script strictement pour base locale/test.
--   - Ne touche PAS: operation_epargne, collecte_journaliere_terrain,
--     collecte_membre_ligne, collecte_operation_generee, membre.
-- =============================================================

-- 0) GARDE-FOU ENVIRONNEMENT
SELECT NOW() AS executed_at, @@hostname AS db_host, @@port AS db_port, DATABASE() AS db_name;
-- Vérifier manuellement que db_host/db_name correspondent à local/test.

SET @collecte_id := 3;
SET @montant_retour_attendu := 51000;
SET @source_retour := 'RECETTE_JOURNALIERE';
SET @desc_retour_prefix := 'Retour terrain collecte #3';

START TRANSACTION;

-- =============================================================
-- A) DIAGNOSTIC AVANT SUPPRESSION
-- =============================================================

-- A1. Cible globale à conserver
SELECT
    oc.id,
    oc.session_caisse_id,
    oc.type_operation,
    oc.source_operation,
    oc.montant,
    oc.recette_id,
    oc.operation_epargne_id,
    oc.reference_externe,
    oc.description,
    oc.date_operation
FROM operation_caisse oc
WHERE oc.type_operation = 'ENTREE'
  AND oc.source_operation = @source_retour
  AND oc.montant = @montant_retour_attendu
  AND oc.recette_id = @collecte_id
  AND oc.description LIKE CONCAT(@desc_retour_prefix, '%')
ORDER BY oc.id;

-- A2. Toutes les opérations caisse liées à la collecte #3 (diagnostic large)
SELECT
    oc.id,
    oc.session_caisse_id,
    oc.type_operation,
    oc.source_operation,
    oc.montant,
    oc.recette_id,
    oc.operation_epargne_id,
    oc.reference_externe,
    oc.description,
    oc.date_operation
FROM operation_caisse oc
WHERE (
    oc.recette_id = @collecte_id
    OR oc.description LIKE CONCAT('%collecte #', @collecte_id, '%')
    OR oc.reference_externe LIKE CONCAT('%collecte #', @collecte_id, '%')
    OR oc.description LIKE CONCAT('%Collecte validee #', @collecte_id, '%')
    OR oc.reference_externe LIKE CONCAT('%Collecte validee #', @collecte_id, '%')
)
ORDER BY oc.session_caisse_id, oc.id;

-- A3. Traces de génération (lecture seule, non supprimées)
SELECT
    cog.id,
    cog.collecte_id,
    cog.ligne_collecte_id,
    cog.type_operation,
    cog.operation_id,
    cog.created_by,
    cog.date_creation
FROM collecte_operation_generee cog
WHERE cog.collecte_id = @collecte_id
ORDER BY cog.id;

-- =============================================================
-- B) RESOLUTION DES CIBLES (KEEP + DELETE)
-- =============================================================

SET @keep_operation_id := (
    SELECT oc.id
    FROM operation_caisse oc
    WHERE oc.type_operation = 'ENTREE'
      AND oc.source_operation = @source_retour
      AND oc.montant = @montant_retour_attendu
      AND oc.recette_id = @collecte_id
      AND oc.description LIKE CONCAT(@desc_retour_prefix, '%')
    ORDER BY oc.id
    LIMIT 1
);

SELECT @keep_operation_id AS keep_operation_id;

SET @target_session_id := (
    SELECT oc.session_caisse_id
    FROM operation_caisse oc
    WHERE oc.id = @keep_operation_id
);

SELECT @target_session_id AS target_session_id;

DROP TEMPORARY TABLE IF EXISTS tmp_collecte3_to_delete;
CREATE TEMPORARY TABLE tmp_collecte3_to_delete (
    id BIGINT PRIMARY KEY
);

INSERT INTO tmp_collecte3_to_delete (id)
SELECT oc.id
FROM operation_caisse oc
WHERE oc.session_caisse_id = @target_session_id
  AND oc.id <> @keep_operation_id
  AND oc.type_operation = 'ENTREE'
  AND (
      (
        oc.montant = 50000
        AND (
          oc.description LIKE '%Mouvement epargne automatique%'
          OR oc.reference_externe LIKE '%Epargne #1%'
          OR oc.operation_epargne_id IS NOT NULL
        )
      )
      OR
      (
        oc.montant = 1000
        AND (
          oc.description LIKE CONCAT('%Collecte validee #', @collecte_id, ' - CARNET%')
          OR oc.reference_externe LIKE CONCAT('%Collecte validee #', @collecte_id, ' - CARNET%')
        )
      )
  );

-- B1. Prévisualisation des doublons identifiés
SELECT
    oc.id,
    oc.session_caisse_id,
    oc.type_operation,
    oc.source_operation,
    oc.montant,
    oc.recette_id,
    oc.operation_epargne_id,
    oc.reference_externe,
    oc.description
FROM operation_caisse oc
JOIN tmp_collecte3_to_delete d ON d.id = oc.id
ORDER BY oc.id;

SELECT
    COUNT(*) AS nb_to_delete,
    COALESCE(SUM(oc.montant), 0) AS montant_total_to_delete
FROM operation_caisse oc
JOIN tmp_collecte3_to_delete d ON d.id = oc.id;

-- =============================================================
-- C) SAUVEGARDE + SUPPRESSION CIBLEE
-- =============================================================

CREATE TABLE IF NOT EXISTS maintenance_backup_operation_caisse_collecte3 LIKE operation_caisse;

INSERT INTO maintenance_backup_operation_caisse_collecte3
SELECT oc.*
FROM operation_caisse oc
JOIN tmp_collecte3_to_delete d ON d.id = oc.id;

DELETE oc
FROM operation_caisse oc
JOIN tmp_collecte3_to_delete d ON d.id = oc.id;

SELECT ROW_COUNT() AS deleted_rows;

-- =============================================================
-- D) RECALCUL TOTAUX SESSION CAISSE
-- =============================================================

UPDATE session_caisse sc
SET
    sc.total_entrees = (
        SELECT COALESCE(SUM(oc.montant), 0)
        FROM operation_caisse oc
        WHERE oc.session_caisse_id = sc.id
          AND oc.type_operation = 'ENTREE'
    ),
    sc.total_sorties = (
        SELECT COALESCE(SUM(oc.montant), 0)
        FROM operation_caisse oc
        WHERE oc.session_caisse_id = sc.id
          AND oc.type_operation = 'SORTIE'
    ),
    sc.solde_theorique = COALESCE(sc.solde_ouverture, 0)
        + (
            SELECT COALESCE(SUM(CASE WHEN oc.type_operation = 'ENTREE' THEN oc.montant ELSE 0 END), 0)
            FROM operation_caisse oc
            WHERE oc.session_caisse_id = sc.id
        )
        - (
            SELECT COALESCE(SUM(CASE WHEN oc.type_operation = 'SORTIE' THEN oc.montant ELSE 0 END), 0)
            FROM operation_caisse oc
            WHERE oc.session_caisse_id = sc.id
        )
WHERE sc.id = @target_session_id;

SELECT
    sc.id AS session_id,
    sc.solde_ouverture,
    sc.total_entrees,
    sc.total_sorties,
    sc.solde_theorique
FROM session_caisse sc
WHERE sc.id = @target_session_id;

-- =============================================================
-- E) VERIFICATION APRES NETTOYAGE
-- =============================================================

-- E1. Détail des opérations restantes sur la session ciblée
SELECT
    oc.id,
    oc.type_operation,
    oc.source_operation,
    oc.montant,
    oc.recette_id,
    oc.reference_externe,
    oc.description
FROM operation_caisse oc
WHERE oc.session_caisse_id = @target_session_id
ORDER BY oc.id;

-- E2. Synthèse session
SELECT
    COUNT(*) AS nb_operations,
    COALESCE(SUM(CASE WHEN oc.type_operation = 'ENTREE' THEN oc.montant ELSE 0 END), 0) AS entrees,
    COALESCE(SUM(CASE WHEN oc.type_operation = 'SORTIE' THEN oc.montant ELSE 0 END), 0) AS sorties,
    COALESCE(SUM(CASE WHEN oc.type_operation = 'ENTREE' THEN oc.montant ELSE -oc.montant END), 0) AS solde_filtre
FROM operation_caisse oc
WHERE oc.session_caisse_id = @target_session_id;

-- Attendu (si la session ne contient que le flux collecte #3):
--   entrees = 51000
--   sorties = 0
--   solde_filtre = 51000
--   nb_operations = 1

-- =============================================================
-- F) FIN
-- =============================================================
-- Mode sécurité par défaut:
ROLLBACK;

-- Quand le diagnostic/contrôle est conforme:
-- 1) relancer le script
-- 2) remplacer ROLLBACK par COMMIT
-- COMMIT;
