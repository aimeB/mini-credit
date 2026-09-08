-- Qualite des donnees personnes: unicite intra-table uniquement.
-- A executer apres diagnostic des doublons existants, sinon les index uniques echoueront.

-- Diagnostics prealables:
-- SELECT REPLACE(TRIM(telephone), ' ', '') AS telephone_normalise, COUNT(*) FROM employe WHERE telephone IS NOT NULL AND TRIM(telephone) <> '' GROUP BY telephone_normalise HAVING COUNT(*) > 1;
-- SELECT UPPER(TRIM(prenom)) AS prenom_normalise, UPPER(TRIM(nom)) AS nom_normalise, COUNT(*) FROM employe WHERE prenom IS NOT NULL AND nom IS NOT NULL GROUP BY prenom_normalise, nom_normalise HAVING COUNT(*) > 1;
-- SELECT REPLACE(TRIM(telephone_principal), ' ', '') AS telephone_normalise, COUNT(*) FROM membre WHERE telephone_principal IS NOT NULL AND TRIM(telephone_principal) <> '' GROUP BY telephone_normalise HAVING COUNT(*) > 1;
-- SELECT UPPER(TRIM(prenom)) AS prenom_normalise, UPPER(TRIM(nom)) AS nom_normalise, COUNT(*) FROM membre WHERE prenom IS NOT NULL AND nom IS NOT NULL GROUP BY prenom_normalise, nom_normalise HAVING COUNT(*) > 1;

ALTER TABLE employe
    ADD COLUMN telephone_normalise VARCHAR(20)
        GENERATED ALWAYS AS (NULLIF(REPLACE(TRIM(telephone), ' ', ''), '')) STORED,
    ADD COLUMN prenom_normalise VARCHAR(100)
        GENERATED ALWAYS AS (NULLIF(UPPER(TRIM(prenom)), '')) STORED,
    ADD COLUMN nom_normalise VARCHAR(100)
        GENERATED ALWAYS AS (NULLIF(UPPER(TRIM(nom)), '')) STORED;

ALTER TABLE employe
    ADD UNIQUE KEY uk_employe_telephone_normalise (telephone_normalise),
    ADD UNIQUE KEY uk_employe_prenom_nom_normalise (prenom_normalise, nom_normalise);

ALTER TABLE membre
    ADD COLUMN telephone_principal_normalise VARCHAR(20)
        GENERATED ALWAYS AS (NULLIF(REPLACE(TRIM(telephone_principal), ' ', ''), '')) STORED,
    ADD COLUMN prenom_normalise VARCHAR(100)
        GENERATED ALWAYS AS (NULLIF(UPPER(TRIM(prenom)), '')) STORED,
    ADD COLUMN nom_normalise VARCHAR(100)
        GENERATED ALWAYS AS (NULLIF(UPPER(TRIM(nom)), '')) STORED;

ALTER TABLE membre
    ADD UNIQUE KEY uk_membre_telephone_principal_normalise (telephone_principal_normalise),
    ADD UNIQUE KEY uk_membre_prenom_nom_normalise (prenom_normalise, nom_normalise);