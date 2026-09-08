-- PRIORITÉ 1: Add CONTROLEUR Role and Permissions
-- Date: 5 Juin 2026
-- Purpose: Setup role CONTROLEUR with all validation & control permissions

-- =============================================================================
-- STEP 1: Add CONTROLEUR role (if not exists)
-- =============================================================================
INSERT IGNORE INTO role (code_role, libelle, description, is_active, date_creation)
VALUES (
    'CONTROLEUR',
    'Contrôleur',
    'Validation recettes, retraits, crédits, réconciliation caisse',
    TRUE,
    NOW()
);

-- Get CONTROLEUR role ID for use in mappings
-- Note: @Variable :controleur_role_id will be set by app after insert

-- =============================================================================
-- STEP 2: Add CONTROLEUR-specific permissions (if not exists)
-- =============================================================================
-- Note: Some permissions may already exist from previous migrations
-- Using INSERT IGNORE to handle duplicates safely

-- Recette validation permissions
INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('CONTROLEUR_RECETTES_VALIDATE', 'Valider recettes journalières (réconciliation)', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('CONTROLEUR_RETRAITS_VALIDATE', 'Valider retraits épargne (vérif solde)', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('CONTROLEUR_CREDITS_VALIDATE', 'Valider crédits (garanties, frais, décaissement)', TRUE, NOW());

-- Caisse & Épargne controls
INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('CONTROLEUR_EPARGNE_READ', 'Voir les comptes épargne (CONTROLEUR)', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('CONTROLEUR_EPARGNE_OPERATION_READ', 'Voir les opérations épargne (CONTROLEUR)', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('CONTROLEUR_CREDIT_READ', 'Voir les crédits (CONTROLEUR)', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('CONTROLEUR_DEMANDE_CREDIT_READ', 'Voir les demandes crédit (CONTROLEUR)', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('CONTROLEUR_DEMANDE_CREDIT_VALIDATE', 'Valider/modifier analyse risque demandes crédit', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('CONTROLEUR_SESSION_CAISSE_READ', 'Voir les SessionCaisse (CONTROLEUR)', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('CONTROLEUR_SESSION_CAISSE_VALIDATE', 'Valider clôture SessionCaisse', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('CONTROLEUR_ECART_READ', 'Voir les écarts caisse', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('CONTROLEUR_ECART_VALIDATE', 'Valider/modifier écarts investigation', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('CONTROLEUR_AUDIT_READ', 'Consulter audit logs antenne', TRUE, NOW());

-- Phase 6B.1: Fiche Journalière permissions
INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('FICHE_JOURNALIERE_CREATE', 'Créer une fiche journalière agent', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('FICHE_JOURNALIERE_READ', 'Voir les fiches journalières agent', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('FICHE_JOURNALIERE_EDIT', 'Modifier une fiche journalière agent (BROUILLON)', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('FICHE_JOURNALIERE_DELETE', 'Supprimer une fiche journalière agent (BROUILLON)', TRUE, NOW());

-- =============================================================================
-- STEP 3: Map CONTROLEUR role to all required permissions
-- =============================================================================
-- Create temporary variables for role and permission IDs
SET @controleur_role_id = (SELECT id FROM role WHERE code_role = 'CONTROLEUR' LIMIT 1);

-- Map all CONTROLEUR permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id, date_creation)
SELECT @controleur_role_id, id, NOW()
FROM permissions
WHERE code IN (
    -- Recette validations
    'CONTROLEUR_RECETTES_VALIDATE',
    'CONTROLEUR_RETRAITS_VALIDATE',
    'CONTROLEUR_CREDITS_VALIDATE',
    
    -- Epargne & Credit reads
    'CONTROLEUR_EPARGNE_READ',
    'CONTROLEUR_EPARGNE_OPERATION_READ',
    'CONTROLEUR_CREDIT_READ',
    'CONTROLEUR_DEMANDE_CREDIT_READ',
    'CONTROLEUR_DEMANDE_CREDIT_VALIDATE',
    
    -- Caisse controls
    'CONTROLEUR_SESSION_CAISSE_READ',
    'CONTROLEUR_SESSION_CAISSE_VALIDATE',
    'CONTROLEUR_ECART_READ',
    'CONTROLEUR_ECART_VALIDATE',
    'CONTROLEUR_AUDIT_READ',
    
    -- Phase 6B.1: Fiche Journalière
    'FICHE_JOURNALIERE_CREATE',
    'FICHE_JOURNALIERE_READ',
    'FICHE_JOURNALIERE_EDIT',
    'FICHE_JOURNALIERE_DELETE'
);

-- =============================================================================
-- VALIDATION & LOGGING
-- =============================================================================
SELECT CONCAT(
    '✅ PRIORITÉ 1 Migrations Complete | ',
    'Role: ',
    IFNULL((SELECT code_role FROM role WHERE code_role = 'CONTROLEUR'), 'NOT FOUND'),
    ' | Permissions mapped: ',
    IFNULL((SELECT COUNT(*) FROM role_permissions WHERE role_id = @controleur_role_id), 0)
) AS migration_status;
