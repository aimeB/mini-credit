-- Normalize legacy/typographical status values without deleting history.

UPDATE session_caisse
SET statut = 'CLOTUREE',
    date_modification = NOW()
WHERE statut = 'FERMEE';

UPDATE fiche_journaliere_agent_terrain
SET statut = 'ANNULEE',
    date_modification = NOW()
WHERE statut = 'ANNULEA';

SELECT 'V049 OK' AS migration_status;