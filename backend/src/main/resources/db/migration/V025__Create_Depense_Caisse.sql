CREATE TABLE depense_caisse (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_caisse_id BIGINT NULL,
    caisse_id BIGINT NOT NULL,
    site_id BIGINT NULL,
    categorie VARCHAR(40) NOT NULL,
    montant DECIMAL(18,2) NOT NULL,
    devise VARCHAR(10) NOT NULL DEFAULT 'CDF',
    motif TEXT NOT NULL,
    beneficiaire VARCHAR(255) NULL,
    justificatif_url VARCHAR(500) NULL,
    statut VARCHAR(40) NOT NULL,
    demande_par_id BIGINT NULL,
    valide_par_id BIGINT NULL,
    paye_par_id BIGINT NULL,
    date_demande DATETIME NOT NULL,
    date_soumission DATETIME NULL,
    date_validation DATETIME NULL,
    date_paiement DATETIME NULL,
    operation_caisse_id BIGINT NULL,
    commentaire_validation TEXT NULL,
    motif_rejet TEXT NULL,
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification DATETIME NULL,
    CONSTRAINT fk_depense_caisse_session FOREIGN KEY (session_caisse_id) REFERENCES session_caisse(id),
    CONSTRAINT fk_depense_caisse_caisse FOREIGN KEY (caisse_id) REFERENCES caisse(id),
    CONSTRAINT fk_depense_caisse_site FOREIGN KEY (site_id) REFERENCES site(id),
    CONSTRAINT fk_depense_caisse_demande_par FOREIGN KEY (demande_par_id) REFERENCES utilisateur(id),
    CONSTRAINT fk_depense_caisse_valide_par FOREIGN KEY (valide_par_id) REFERENCES utilisateur(id),
    CONSTRAINT fk_depense_caisse_paye_par FOREIGN KEY (paye_par_id) REFERENCES utilisateur(id),
    CONSTRAINT fk_depense_caisse_operation_caisse FOREIGN KEY (operation_caisse_id) REFERENCES operation_caisse(id)
);

CREATE INDEX idx_depense_caisse_statut ON depense_caisse(statut);
CREATE INDEX idx_depense_caisse_date_demande ON depense_caisse(date_demande);
CREATE INDEX idx_depense_caisse_session_caisse_id ON depense_caisse(session_caisse_id);
CREATE INDEX idx_depense_caisse_caisse_id ON depense_caisse(caisse_id);
CREATE INDEX idx_depense_caisse_site_id ON depense_caisse(site_id);
CREATE INDEX idx_depense_caisse_operation_caisse_id ON depense_caisse(operation_caisse_id);

ALTER TABLE depense_caisse
    ADD CONSTRAINT uk_depense_caisse_operation UNIQUE (operation_caisse_id);

SELECT 'V025 OK' AS migration_status;