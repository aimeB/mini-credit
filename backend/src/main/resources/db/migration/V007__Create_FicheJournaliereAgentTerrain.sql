-- PHASE 6B.1: Create FicheJournaliereAgentTerrain table
-- Fiche journalière consolidée agent terrain

CREATE TABLE fiche_journaliere_agent_terrain (
    id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    
    -- Relations
    utilisateur_id BIGINT NOT NULL,
    site_id BIGINT,
    valide_par_id BIGINT,
    
    -- Identité & contexte
    date_fiche DATE NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON',
    
    -- Consolidation financière (montants en BigDecimal 19,2)
    epargne_collectee_total DECIMAL(19,2) NOT NULL DEFAULT 0,
    remboursement_collectes DECIMAL(19,2) NOT NULL DEFAULT 0,
    frais_collectes DECIMAL(19,2) NOT NULL DEFAULT 0,
    autres_recettes DECIMAL(19,2) NOT NULL DEFAULT 0,
    -- montant_total_collecte: FORMULA (read-only)
    
    -- Dénombrements
    nombre_membres_visites INT NOT NULL DEFAULT 0,
    nombre_nouveaux_membres INT NOT NULL DEFAULT 0,
    nombre_carnets_distribues INT NOT NULL DEFAULT 0,
    
    -- Contrôle caisse
    total_especes_remises DECIMAL(19,2),
    variance DECIMAL(19,2) DEFAULT 0,
    variance_percentage DOUBLE DEFAULT 0.0,
    excedent DECIMAL(19,2) NOT NULL DEFAULT 0,
    manquant DECIMAL(19,2) NOT NULL DEFAULT 0,
    
    -- Observations
    observations_agent TEXT,
    observations_controleur TEXT,
    
    -- Statut & validation
    date_validation DATETIME,
    motif_rejet TEXT,
    raison_annulation TEXT,
    
    -- Audit
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification DATETIME,
    
    -- Constraints
    CONSTRAINT fk_fiche_agent FOREIGN KEY (utilisateur_id) 
        REFERENCES utilisateur(id) ON DELETE RESTRICT,
    CONSTRAINT fk_fiche_site FOREIGN KEY (site_id) 
        REFERENCES site(id) ON DELETE SET NULL,
    CONSTRAINT fk_fiche_valide_par FOREIGN KEY (valide_par_id) 
        REFERENCES utilisateur(id) ON DELETE SET NULL,
    
    -- Unique constraint: one fiche per agent per day
    CONSTRAINT uq_fiche_agent_date UNIQUE KEY (utilisateur_id, date_fiche)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create indexes for common queries
CREATE INDEX idx_utilisateur_id ON fiche_journaliere_agent_terrain(utilisateur_id);
CREATE INDEX idx_site_id ON fiche_journaliere_agent_terrain(site_id);
CREATE INDEX idx_date_fiche ON fiche_journaliere_agent_terrain(date_fiche);
CREATE INDEX idx_statut ON fiche_journaliere_agent_terrain(statut);
CREATE INDEX idx_valide_par_id ON fiche_journaliere_agent_terrain(valide_par_id);
CREATE INDEX idx_fiche_agent_date ON fiche_journaliere_agent_terrain(utilisateur_id, date_fiche);
CREATE INDEX idx_statut_date ON fiche_journaliere_agent_terrain(statut, date_fiche);

-- ALTER recette_journaliere_terrain: add FK to fiche_journaliere_agent_terrain
ALTER TABLE recette_journaliere_terrain 
ADD COLUMN fiche_journaliere_id BIGINT AFTER agent_id;

ALTER TABLE recette_journaliere_terrain 
ADD CONSTRAINT fk_recette_fiche FOREIGN KEY (fiche_journaliere_id) 
    REFERENCES fiche_journaliere_agent_terrain(id) ON DELETE SET NULL;

-- Index on fiche_journaliere_id for consolidation queries
CREATE INDEX idx_recette_fiche ON recette_journaliere_terrain(fiche_journaliere_id);

-- ROLLBACK STRATEGY:
-- IF rollback needed:
--   1. DROP TABLE fiche_journaliere_agent_terrain;
--   2. ALTER TABLE recette_journaliere_terrain DROP CONSTRAINT fk_recette_fiche;
--   3. ALTER TABLE recette_journaliere_terrain DROP COLUMN fiche_journaliere_id;
