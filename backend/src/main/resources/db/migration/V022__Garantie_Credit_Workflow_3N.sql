-- V022: Workflow garantie crédit 3N
-- - Garantie épargne bloquée sur compte épargne
-- - Garantie matérielle séparée du risque
-- - Traçabilité des opérations liées à la demande/crédit

CREATE TABLE IF NOT EXISTS garantie_credit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    date_creation DATETIME NOT NULL,
    date_modification DATETIME NULL,
    demande_credit_id BIGINT NOT NULL,
    credit_id BIGINT NULL,
    membre_id BIGINT NOT NULL,
    compte_epargne_id BIGINT NULL,
    montant_credit DECIMAL(18,2) NOT NULL,
    devise VARCHAR(10) NOT NULL DEFAULT 'CDF',
    montant_garantie_requis DECIMAL(18,2) NOT NULL DEFAULT 0,
    montant_garantie_bloque DECIMAL(18,2) NOT NULL DEFAULT 0,
    montant_garantie_manquant DECIMAL(18,2) NOT NULL DEFAULT 0,
    statut_garantie_epargne VARCHAR(30) NOT NULL,
    controle_par BIGINT NULL,
    date_controle DATETIME NULL,
    date_blocage DATETIME NULL,
    date_liberation DATETIME NULL,
    commentaire_controle TEXT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_garantie_credit_demande (demande_credit_id),
    KEY idx_garantie_credit_membre (membre_id),
    KEY idx_garantie_credit_compte (compte_epargne_id),
    CONSTRAINT fk_garantie_credit_demande FOREIGN KEY (demande_credit_id) REFERENCES demande_credit(id),
    CONSTRAINT fk_garantie_credit_credit FOREIGN KEY (credit_id) REFERENCES credit(id),
    CONSTRAINT fk_garantie_credit_membre FOREIGN KEY (membre_id) REFERENCES membre(id),
    CONSTRAINT fk_garantie_credit_compte FOREIGN KEY (compte_epargne_id) REFERENCES compte_epargne(id),
    CONSTRAINT fk_garantie_credit_controle_par FOREIGN KEY (controle_par) REFERENCES utilisateur(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS garantie_materielle (
    id BIGINT NOT NULL AUTO_INCREMENT,
    date_creation DATETIME NOT NULL,
    date_modification DATETIME NULL,
    garantie_credit_id BIGINT NOT NULL,
    type_bien VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    valeur_estimee DECIMAL(18,2) NOT NULL,
    devise VARCHAR(10) NOT NULL DEFAULT 'CDF',
    proprietaire_declare VARCHAR(200) NULL,
    localisation TEXT NULL,
    reference_document VARCHAR(255) NULL,
    statut VARCHAR(30) NOT NULL,
    controle_par BIGINT NULL,
    date_controle DATETIME NULL,
    commentaire TEXT NULL,
    PRIMARY KEY (id),
    KEY idx_garantie_materielle_gc (garantie_credit_id),
    KEY idx_garantie_materielle_statut (statut),
    CONSTRAINT fk_garantie_materielle_gc FOREIGN KEY (garantie_credit_id) REFERENCES garantie_credit(id) ON DELETE CASCADE,
    CONSTRAINT fk_garantie_materielle_controle_par FOREIGN KEY (controle_par) REFERENCES utilisateur(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE operation_epargne
    ADD COLUMN demande_credit_id BIGINT NULL AFTER membre_id,
    ADD COLUMN credit_id BIGINT NULL AFTER demande_credit_id,
    ADD CONSTRAINT fk_operation_epargne_demande_credit FOREIGN KEY (demande_credit_id) REFERENCES demande_credit(id),
    ADD CONSTRAINT fk_operation_epargne_credit FOREIGN KEY (credit_id) REFERENCES credit(id);