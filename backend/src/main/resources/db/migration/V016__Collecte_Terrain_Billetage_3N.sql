ALTER TABLE collecte_journaliere_terrain
    ADD COLUMN especes_declarees_agent DECIMAL(15,2) NULL AFTER especes_remises,
    ADD COLUMN especes_confirmees_caissier DECIMAL(15,2) NULL AFTER especes_declarees_agent,
    ADD COLUMN date_confirmation_billetage DATETIME NULL AFTER especes_confirmees_caissier,
    ADD COLUMN confirme_par_caissier_id BIGINT NULL AFTER date_confirmation_billetage,
    ADD COLUMN observation_billetage LONGTEXT NULL AFTER confirme_par_caissier_id,
    ADD COLUMN billetage_confirme BIT(1) NOT NULL DEFAULT b'0' AFTER observation_billetage;

UPDATE collecte_journaliere_terrain
SET especes_declarees_agent = COALESCE(especes_remises, 0),
    especes_confirmees_caissier = COALESCE(especes_remises, 0),
    billetage_confirme = CASE WHEN statut IN ('VALIDEE', 'REJETEE') THEN b'1' ELSE b'0' END,
    date_confirmation_billetage = CASE WHEN statut IN ('VALIDEE', 'REJETEE') THEN COALESCE(validated_at, submitted_at) ELSE NULL END,
    confirme_par_caissier_id = NULL,
    observation_billetage = observations;

ALTER TABLE collecte_journaliere_terrain
    MODIFY COLUMN especes_declarees_agent DECIMAL(15,2) NOT NULL,
    MODIFY COLUMN especes_confirmees_caissier DECIMAL(15,2) NOT NULL;
