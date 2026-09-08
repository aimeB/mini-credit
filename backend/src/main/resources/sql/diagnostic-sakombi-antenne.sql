-- Diagnostic et correction ciblée: Site de Sakombi -> Antenne (Agence)
-- IMPORTANT:
-- 1) Le concept "antenne" est porté par la table `agence` dans le modèle actuel.
-- 2) Ce script ne crée aucune antenne/agence. Il ne fait qu'utiliser une agence existante.

-- 1) Le site "Site de Sakombi" existe-t-il ?
SELECT id, code_site, nom_site, agence_id
FROM site
WHERE UPPER(nom_site) = 'SITE DE SAKOMBI';

-- 2) Le site a-t-il un champ de rattachement antenne/agence ?
-- (attendu: colonne `agence_id`)
SHOW COLUMNS FROM site LIKE 'agence_id';
SHOW COLUMNS FROM site LIKE 'antenne_id';
SHOW COLUMNS FROM site LIKE 'bureau_id';

-- 3) Le champ est-il renseigné pour Sakombi ?
SELECT s.id, s.nom_site, s.agence_id, a.nom_agence AS antenne_nom
FROM site s
LEFT JOIN agence a ON a.id = s.agence_id
WHERE UPPER(s.nom_site) = 'SITE DE SAKOMBI';

-- 4) Existe-t-il une table antenne/agence/bureau ?
SHOW TABLES LIKE 'agence';
SHOW TABLES LIKE 'antenne';
SHOW TABLES LIKE 'bureau';

-- 5) Relation réelle actuelle Site -> Antenne
SELECT
  kcu.TABLE_NAME,
  kcu.COLUMN_NAME,
  kcu.REFERENCED_TABLE_NAME,
  kcu.REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE kcu
WHERE kcu.TABLE_SCHEMA = DATABASE()
  AND kcu.TABLE_NAME = 'site'
  AND kcu.COLUMN_NAME = 'agence_id';

-- 6) Correction de données (seulement si Sakombi n'est pas correctement rattaché)
-- Cette mise à jour ne s'applique que si une agence contenant "Sakombi" existe déjà.
UPDATE site s
SET s.agence_id = (
  SELECT MIN(a.id)
  FROM agence a
  WHERE UPPER(a.nom_agence) LIKE '%SAKOMBI%'
     OR UPPER(a.code_agence) LIKE '%SAKOMBI%'
)
WHERE UPPER(s.nom_site) = 'SITE DE SAKOMBI'
  AND (
    s.agence_id IS NULL
    OR s.agence_id = 0
    OR s.agence_id NOT IN (SELECT id FROM agence)
  )
  AND (
    SELECT MIN(a2.id)
    FROM agence a2
    WHERE UPPER(a2.nom_agence) LIKE '%SAKOMBI%'
       OR UPPER(a2.code_agence) LIKE '%SAKOMBI%'
  ) IS NOT NULL;

-- Vérification finale
SELECT s.id, s.code_site, s.nom_site, s.agence_id, a.nom_agence AS antenne_nom
FROM site s
LEFT JOIN agence a ON a.id = s.agence_id
WHERE UPPER(s.nom_site) = 'SITE DE SAKOMBI';
