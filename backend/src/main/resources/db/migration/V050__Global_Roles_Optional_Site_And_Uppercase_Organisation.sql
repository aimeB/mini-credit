-- V050: site obligatoire uniquement pour AGENT_TERRAIN + normalisation noms organisation.
-- A executer via l'outil SQL de maintenance si Flyway n'est pas actif dans l'environnement.

ALTER TABLE employe
    MODIFY site_id BIGINT NULL;

-- Si la colonne utilisateur.site_id existe dans la base cible, elle doit aussi rester nullable.
-- Decommenter uniquement apres verification de la colonne dans le schema courant.
-- ALTER TABLE utilisateur
--     MODIFY site_id BIGINT NULL;

UPDATE agence
SET nom_agence = UPPER(TRIM(nom_agence))
WHERE nom_agence IS NOT NULL;

UPDATE site
SET nom_site = UPPER(TRIM(nom_site))
WHERE nom_site IS NOT NULL;