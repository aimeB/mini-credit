-- Nettoyage ciblé des doublons historiques caisse pour la collecte #3
-- Périmètre: environnement local/test UNIQUEMENT
-- Objectif: conserver uniquement l'ecriture globale RECETTE_JOURNALIERE de 51 000

-- =====================================================================
-- 0) GARDE-FOU ENVIRONNEMENT
-- =====================================================================
SELECT NOW() AS executed_at, @@hostname AS db_host, @@port AS db_port, DATABASE() AS db_name;
-- Vérifiez visuellement que la base est locale/test avant de continuer.
-- Recommandation: db_name contient local, test, dev ou sandbox.

-- =====================================================================
-- 1) PARAMETRES CIBLES
-- =====================================================================
SET @collecte_id := 3;
SET @montant_global_attendu := 51000;
SET @source_a_conserver := 'RECETTE_JOURNALIERE';
SET @obs_globale_prefix := 'Retour terrain collecte #3';

START TRANSACTION;

-- =====================================================================
-- 2) DIAGNOSTIC AVANT SUPPRESSION
-- =====================================================================
-- 2.1 Toutes les operations potentiellement liees a la collecte #3
SELECT
  oc.id,
  oc.session_caisse_id,
  oc.type,
  oc.source,
  oc.montant,
  oc.operation_epargne_id,
  oc.reference_externe,
  oc.description,
  oc.date_operation
FROM operation_caisse oc
WHERE (
    oc.reference_externe LIKE CONCAT('%#', @collecte_id, '%')
    OR oc.reference_externe LIKE CONCAT('%collecte #', @collecte_id, '%')
    OR oc.description LIKE CONCAT('%collecte #', @collecte_id, '%')
    OR oc.description LIKE CONCAT('%Collecte validee #', @collecte_id, '%')
    OR oc.description LIKE CONCAT('%Retour terrain collecte #', @collecte_id, '%')
)
OR (
    oc.description LIKE '%Mouvement epargne automatique%'
    AND oc.type = 'ENTREE'
)
ORDER BY oc.session_caisse_id, oc.id;

-- 2.2 Operation globale a conserver (doit retourner 1 ligne)
SELECT
  oc.id,
  oc.session_caisse_id,
  oc.type,
  oc.source,
  oc.montant,
  oc.reference_externe,
  oc.description
FROM operation_caisse oc
WHERE oc.source = @source_a_conserver
  AND oc.type = 'ENTREE'
  AND oc.montant = @montant_global_attendu
  AND oc.description LIKE CONCAT(@obs_globale_prefix, '%')
ORDER BY oc.id;

-- =====================================================================
-- 3) RESOLUTION CIBLE ET DOUBLONS
-- =====================================================================
-- 3.1 ID de l'operation globale a garder
SET @keep_operation_id := (
  SELECT oc.id
  FROM operation_caisse oc
  WHERE oc.source = @source_a_conserver
    AND oc.type = 'ENTREE'
    AND oc.montant = @montant_global_attendu
    AND oc.description LIKE CONCAT(@obs_globale_prefix, '%')
  ORDER BY oc.id
  LIMIT 1
);

SELECT @keep_operation_id AS keep_operation_id;

-- 3.2 Session cible (celle de l'operation globale)
SET @target_session_id := (
  SELECT oc.session_caisse_id
  FROM operation_caisse oc
  WHERE oc.id = @keep_operation_id
);

SELECT @target_session_id AS target_session_id;

-- 3.3 Liste des lignes a supprimer (doublons historiques)
DROP TEMPORARY TABLE IF EXISTS tmp_collecte3_to_delete;
CREATE TEMPORARY TABLE tmp_collecte3_to_delete AS
SELECT oc.id
FROM operation_caisse oc
WHERE oc.session_caisse_id = @target_session_id
  AND oc.id <> @keep_operation_id
  AND oc.type = 'ENTREE'
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

SELECT oc.*
FROM operation_caisse oc
JOIN tmp_collecte3_to_delete d ON d.id = oc.id
ORDER BY oc.id;

SELECT COUNT(*) AS nb_to_delete,
       COALESCE(SUM(oc.montant), 0) AS montant_total_to_delete
FROM operation_caisse oc
JOIN tmp_collecte3_to_delete d ON d.id = oc.id;

-- =====================================================================
-- 4) SAUVEGARDE ET SUPPRESSION CIBLEE
-- =====================================================================
CREATE TABLE IF NOT EXISTS maintenance_backup_operation_caisse_collecte3 LIKE operation_caisse;

INSERT INTO maintenance_backup_operation_caisse_collecte3
SELECT oc.*
FROM operation_caisse oc
JOIN tmp_collecte3_to_delete d ON d.id = oc.id;

DELETE oc
FROM operation_caisse oc
JOIN tmp_collecte3_to_delete d ON d.id = oc.id;

SELECT ROW_COUNT() AS deleted_rows;

-- =====================================================================
-- 5) RECALCUL TOTALS SESSION (SEULEMENT LA SESSION CIBLE)
-- =====================================================================
UPDATE session_caisse sc
SET
  sc.total_entrees = (
    SELECT COALESCE(SUM(oc.montant), 0)
    FROM operation_caisse oc
    WHERE oc.session_caisse_id = sc.id
      AND oc.type = 'ENTREE'
  ),
  sc.total_sorties = (
    SELECT COALESCE(SUM(oc.montant), 0)
    FROM operation_caisse oc
    WHERE oc.session_caisse_id = sc.id
      AND oc.type = 'SORTIE'
  ),
  sc.solde_theorique = COALESCE(sc.solde_ouverture, 0)
    + (
      SELECT COALESCE(SUM(CASE WHEN oc.type = 'ENTREE' THEN oc.montant ELSE 0 END), 0)
      FROM operation_caisse oc
      WHERE oc.session_caisse_id = sc.id
    )
    - (
      SELECT COALESCE(SUM(CASE WHEN oc.type = 'SORTIE' THEN oc.montant ELSE 0 END), 0)
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

-- =====================================================================
-- 6) VERIFICATION APRES NETTOYAGE
-- =====================================================================
SELECT
  oc.id,
  oc.type,
  oc.source,
  oc.montant,
  oc.reference_externe,
  oc.description
FROM operation_caisse oc
WHERE oc.session_caisse_id = @target_session_id
ORDER BY oc.id;

SELECT
  COUNT(*) AS nb_operations_session,
  COALESCE(SUM(CASE WHEN oc.type = 'ENTREE' THEN oc.montant ELSE 0 END), 0) AS entrees,
  COALESCE(SUM(CASE WHEN oc.type = 'SORTIE' THEN oc.montant ELSE 0 END), 0) AS sorties,
  COALESCE(SUM(CASE WHEN oc.type = 'ENTREE' THEN oc.montant ELSE -oc.montant END), 0) AS solde_filtre
FROM operation_caisse oc
WHERE oc.session_caisse_id = @target_session_id;

-- Attendu pour la collecte #3 sur la session cible:
-- entrees = 51000
-- sorties = 0
-- solde_filtre = 51000
-- nb_operations_session = 1 (si la session ne contient que ce flux cible)

-- =====================================================================
-- 7) FIN TRANSACTION
-- =====================================================================
-- COMMIT;
ROLLBACK;

-- Remplacez ROLLBACK par COMMIT apres validation du diagnostic/verification.