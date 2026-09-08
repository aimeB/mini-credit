CREATE TABLE IF NOT EXISTS ticket_recu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_ticket VARCHAR(50) NOT NULL,
    type_ticket VARCHAR(30) NOT NULL,
    statut VARCHAR(30) NOT NULL DEFAULT 'GENERE',
    original_ticket_id BIGINT NULL,
    nombre_impressions INT NOT NULL DEFAULT 0,
    nombre_duplicatas INT NOT NULL DEFAULT 0,
    date_generation DATETIME NOT NULL,
    date_derniere_impression DATETIME NULL,
    date_dernier_duplicata DATETIME NULL,
    operation_epargne_id BIGINT NULL,
    operation_caisse_id BIGINT NULL,
    operation_caisse_commission_id BIGINT NULL,
    demande_retrait_epargne_id BIGINT NULL,
    collecte_journaliere_id BIGINT NULL,
    collecte_membre_ligne_id BIGINT NULL,
    session_caisse_id BIGINT NULL,
    caisse_id BIGINT NULL,
    membre_id BIGINT NOT NULL,
    compte_epargne_id BIGINT NOT NULL,
    agence_id BIGINT NULL,
    site_id BIGINT NULL,
    utilisateur_createur_id BIGINT NULL,
    utilisateur_impression_id BIGINT NULL,
    utilisateur_duplicata_id BIGINT NULL,
    devise VARCHAR(10) NOT NULL DEFAULT 'CDF',
    montant_principal DECIMAL(18,2) NOT NULL,
    taux_commission DECIMAL(5,2) NULL,
    montant_commission DECIMAL(18,2) NULL,
    montant_total_debite DECIMAL(18,2) NULL,
    montant_remis_membre DECIMAL(18,2) NULL,
    ancien_solde DECIMAL(18,2) NOT NULL,
    nouveau_solde DECIMAL(18,2) NOT NULL,
    commentaire TEXT NULL,
    motif_duplicata TEXT NULL,
    code_verification VARCHAR(20) NOT NULL,
    qr_payload VARCHAR(300) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification DATETIME NULL,
    CONSTRAINT uk_ticket_recu_numero UNIQUE (numero_ticket),
    CONSTRAINT uk_ticket_recu_code_verification UNIQUE (code_verification),
    CONSTRAINT fk_ticket_original FOREIGN KEY (original_ticket_id) REFERENCES ticket_recu(id),
    CONSTRAINT fk_ticket_operation_epargne FOREIGN KEY (operation_epargne_id) REFERENCES operation_epargne(id),
    CONSTRAINT fk_ticket_operation_caisse FOREIGN KEY (operation_caisse_id) REFERENCES operation_caisse(id),
    CONSTRAINT fk_ticket_operation_caisse_commission FOREIGN KEY (operation_caisse_commission_id) REFERENCES operation_caisse(id),
    CONSTRAINT fk_ticket_demande_retrait FOREIGN KEY (demande_retrait_epargne_id) REFERENCES demande_retrait_epargne(id),
    CONSTRAINT fk_ticket_collecte_journaliere FOREIGN KEY (collecte_journaliere_id) REFERENCES collecte_journaliere_terrain(id),
    CONSTRAINT fk_ticket_collecte_ligne FOREIGN KEY (collecte_membre_ligne_id) REFERENCES collecte_membre_ligne(id),
    CONSTRAINT fk_ticket_session_caisse FOREIGN KEY (session_caisse_id) REFERENCES session_caisse(id),
    CONSTRAINT fk_ticket_caisse FOREIGN KEY (caisse_id) REFERENCES caisse(id),
    CONSTRAINT fk_ticket_membre FOREIGN KEY (membre_id) REFERENCES membre(id),
    CONSTRAINT fk_ticket_compte FOREIGN KEY (compte_epargne_id) REFERENCES compte_epargne(id),
    CONSTRAINT fk_ticket_agence FOREIGN KEY (agence_id) REFERENCES agence(id),
    CONSTRAINT fk_ticket_site FOREIGN KEY (site_id) REFERENCES site(id),
    CONSTRAINT fk_ticket_utilisateur_createur FOREIGN KEY (utilisateur_createur_id) REFERENCES utilisateur(id),
    CONSTRAINT fk_ticket_utilisateur_impression FOREIGN KEY (utilisateur_impression_id) REFERENCES utilisateur(id),
    CONSTRAINT fk_ticket_utilisateur_duplicata FOREIGN KEY (utilisateur_duplicata_id) REFERENCES utilisateur(id),
    CONSTRAINT fk_ticket_created_by FOREIGN KEY (created_by) REFERENCES utilisateur(id),
    CONSTRAINT fk_ticket_updated_by FOREIGN KEY (updated_by) REFERENCES utilisateur(id),
    CONSTRAINT chk_ticket_montant_principal CHECK (montant_principal >= 0),
    CONSTRAINT chk_ticket_impressions CHECK (nombre_impressions >= 0),
    CONSTRAINT chk_ticket_duplicatas CHECK (nombre_duplicatas >= 0)
);

CREATE UNIQUE INDEX uk_ticket_original_operation_type
    ON ticket_recu(operation_epargne_id, type_ticket, original_ticket_id);

CREATE INDEX idx_ticket_recu_operation_epargne ON ticket_recu(operation_epargne_id);
CREATE INDEX idx_ticket_recu_operation_caisse ON ticket_recu(operation_caisse_id);
CREATE INDEX idx_ticket_recu_demande_retrait ON ticket_recu(demande_retrait_epargne_id);
CREATE INDEX idx_ticket_recu_membre ON ticket_recu(membre_id);
CREATE INDEX idx_ticket_recu_compte ON ticket_recu(compte_epargne_id);
CREATE INDEX idx_ticket_recu_session ON ticket_recu(session_caisse_id);
CREATE INDEX idx_ticket_recu_caisse ON ticket_recu(caisse_id);
CREATE INDEX idx_ticket_recu_agence ON ticket_recu(agence_id);
CREATE INDEX idx_ticket_recu_type_statut ON ticket_recu(type_ticket, statut);

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES
    ('TICKET_RECU_READ', 'Consulter les tickets reçus', TRUE, NOW()),
    ('TICKET_RECU_PRINT', 'Imprimer ou marquer impression ticket reçu', TRUE, NOW()),
    ('TICKET_RECU_DUPLICATA', 'Générer un duplicata de ticket reçu', TRUE, NOW()),
    ('TICKET_RECU_VERIFY', 'Vérifier authenticité ticket reçu', TRUE, NOW()),
    ('TICKET_RECU_ADMIN', 'Administrer les tickets reçus', TRUE, NOW());
