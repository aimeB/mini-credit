CREATE TABLE collecte_operation_generee (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    collecte_id BIGINT NOT NULL,
    ligne_collecte_id BIGINT NULL,
    type_operation VARCHAR(60) NOT NULL,
    operation_id BIGINT NOT NULL,
    created_by BIGINT,
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_cog_collecte FOREIGN KEY (collecte_id) REFERENCES collecte_journaliere_terrain(id) ON DELETE CASCADE,
    CONSTRAINT fk_cog_ligne FOREIGN KEY (ligne_collecte_id) REFERENCES collecte_membre_ligne(id) ON DELETE SET NULL,
    CONSTRAINT fk_cog_created_by FOREIGN KEY (created_by) REFERENCES utilisateur(id) ON DELETE SET NULL,

    CONSTRAINT uk_cog_collecte_ligne_type UNIQUE (collecte_id, ligne_collecte_id, type_operation),
    CONSTRAINT uk_cog_type_operation_id UNIQUE (type_operation, operation_id),

    INDEX idx_cog_collecte (collecte_id),
    INDEX idx_cog_ligne (ligne_collecte_id),
    INDEX idx_cog_type (type_operation)
);
