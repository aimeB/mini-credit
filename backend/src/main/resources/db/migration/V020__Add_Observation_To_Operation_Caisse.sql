-- PATCH P1: Ajouter le champ observation sur operation_caisse

ALTER TABLE operation_caisse
    ADD COLUMN observation TEXT NULL AFTER description;

SELECT 'V020 OK' AS migration_status;
