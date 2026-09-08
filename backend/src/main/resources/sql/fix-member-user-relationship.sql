-- ================================================
-- 🔧 FIX: Synchroniser la relation Membre <-> Utilisateur
-- ================================================
-- Ce script corrige l'incohérence bidirectionnelle où:
--   - Utilisateur pointe vers Membre (membre_id)
--   - Mais Membre ne pointait PAS vers Utilisateur (utilisateur_id = NULL)
--
-- EXÉCUTION: Avant de déployer, faire un BACKUP et exécuter ce script
-- ================================================

-- Step 1: Synchroniser tous les Membre.utilisateur_id depuis Utilisateur.membre_id
UPDATE membre m
SET m.utilisateur_id = (
    SELECT u.id 
    FROM utilisateur u 
    WHERE u.membre_id = m.id 
    LIMIT 1
)
WHERE m.utilisateur_id IS NULL 
  AND EXISTS (
    SELECT 1 FROM utilisateur u 
    WHERE u.membre_id = m.id
  );

-- Step 2: Vérifier qu'il n'y a pas d'incohérences
-- Cette requête doit retourner 0 lignes après le fix
SELECT m.id, m.codeMembre, m.utilisateur_id, u.id as utilisateur_id_attendu
FROM membre m
LEFT JOIN utilisateur u ON u.membre_id = m.id
WHERE (m.utilisateur_id IS NULL AND u.id IS NOT NULL)
   OR (m.utilisateur_id IS NOT NULL AND u.id IS NULL)
   OR (m.utilisateur_id != u.id);

-- Step 3: Log du succès
SELECT CONCAT('✓ Synchronisation Membre<->Utilisateur terminée. ', 
       COUNT(*), ' lignes mises à jour.') as message
FROM membre m
WHERE m.utilisateur_id IS NOT NULL;
