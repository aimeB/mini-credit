-- Phase 5.1: Create RecetteTerrainJournaliere table
-- Enregistrement des recettes collectées par agents terrain

CREATE TABLE recette_terrain_journaliere (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Relations obligatoires
    agent_terrain_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    
    -- Date/Timing
    date_recette DATE NOT NULL,
    
    -- Opérations terrain
    membres_visites INT NOT NULL DEFAULT 0,
    nouveaux_membres INT NOT NULL DEFAULT 0,
    carnet_distribues INT NOT NULL DEFAULT 0,
    
    -- Collecte épargne
    epargne_collectee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    epargne_source_type VARCHAR(255),
    
    -- Collecte crédit
    remboursements_credit_collectes DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    credit_ids_traites JSON,
    
    -- Frais et demandes
    frais_collectes DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    demandes_credit_recueillies INT NOT NULL DEFAULT 0,
    demandes_credit_ids JSON,
    
    -- Trésorerie
    especes_remises DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    especes_emises DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    excedent DECIMAL(15,2),
    manquant DECIMAL(15,2),
    
    -- Metadata
    observations LONGTEXT,
    piece_jointe_path VARCHAR(500),
    
    -- Workflow validation
    statut ENUM('BROUILLON','SOUMISE','VALIDEE','REJETEE') NOT NULL DEFAULT 'BROUILLON',
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_validation DATETIME,
    valide_par BIGINT,
    motif_rejet LONGTEXT,
    
    -- Audit
    date_modification DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    modifie_par BIGINT,
    actif BOOLEAN NOT NULL DEFAULT true,
    
    -- Indexes pour performance
    INDEX idx_agent_terrain (agent_terrain_id),
    INDEX idx_site (site_id),
    INDEX idx_date_recette (date_recette),
    INDEX idx_statut (statut),
    INDEX idx_agent_date (agent_terrain_id, date_recette),
    INDEX idx_site_statut (site_id, statut),
    INDEX idx_valide_par (valide_par),
    
    -- Foreign Keys
    CONSTRAINT fk_rtj_agent_terrain FOREIGN KEY (agent_terrain_id) 
        REFERENCES agent_terrain(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rtj_site FOREIGN KEY (site_id) 
        REFERENCES site(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rtj_valide_par FOREIGN KEY (valide_par) 
        REFERENCES utilisateur(id) ON DELETE SET NULL,
    CONSTRAINT fk_rtj_modifie_par FOREIGN KEY (modifie_par) 
        REFERENCES utilisateur(id) ON DELETE SET NULL,
    
    -- Uniques
    UNIQUE KEY uk_agent_date_recette (agent_terrain_id, date_recette)
);

-- Commentaire table
ALTER TABLE recette_terrain_journaliere COMMENT = 'Recettes journalières collectées par agents terrain - Phase 5.1';
