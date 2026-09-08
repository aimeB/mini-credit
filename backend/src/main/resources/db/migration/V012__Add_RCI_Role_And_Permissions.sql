-- PATCH 11: Ajout du rôle RCI (Responsable du Contrôle Interne)
-- Date: 6 Juin 2026
-- Objectif: Implémenter RCI comme rôle technique distinct selon les documents 3N.
--
-- RCI ≠ Chef de Bureau (RESPONSABLE) ≠ Contrôleur (CONTROLEUR)
-- Périmètre RCI : audit, enquête, contrôle interne, consultation écarts.
-- RCI ne valide PAS les opérations courantes ni n'accepte les variances.

-- =============================================================================
-- STEP 1: Ajout du rôle RCI
-- =============================================================================
INSERT IGNORE INTO role (code_role, libelle, description, is_active, date_creation)
VALUES (
    'RCI',
    'Responsable du Contrôle Interne',
    'Audit, enquête, contrôle interne des opérations — distinct du Chef de Bureau et du Contrôleur',
    TRUE,
    NOW()
);

-- =============================================================================
-- STEP 2: Ajout des permissions spécifiques RCI
-- =============================================================================

-- Lecture et audit des écarts de caisse
INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('RCI_ECART_READ',
        'RCI — Consulter les écarts de caisse (lecture seule)',
        TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('RCI_ECART_INVESTIGATE',
        'RCI — Ouvrir ou suivre une enquête sur un écart de caisse',
        TRUE, NOW());

-- Audit et contrôle interne général
INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('RCI_AUDIT_READ',
        'RCI — Consulter les journaux d''audit (lecture seule)',
        TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('RCI_CONTROLE_INTERNE_READ',
        'RCI — Consulter les rapports de contrôle interne',
        TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('RCI_CONTROLE_INTERNE_AUDIT',
        'RCI — Effectuer un audit de contrôle interne',
        TRUE, NOW());

-- Lecture caisse (sessions, opérations) pour contexte d'audit
INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('RCI_CAISSE_READ',
        'RCI — Consulter les sessions et opérations de caisse (lecture seule)',
        TRUE, NOW());

-- =============================================================================
-- STEP 3: Mapping rôle RCI → permissions
-- =============================================================================
SET @rci_role_id = (SELECT id FROM role WHERE code_role = 'RCI' LIMIT 1);

INSERT IGNORE INTO role_permissions (role_id, permission_id, date_creation)
SELECT @rci_role_id, id, NOW()
FROM permissions
WHERE code IN (
    'RCI_ECART_READ',
    'RCI_ECART_INVESTIGATE',
    'RCI_AUDIT_READ',
    'RCI_CONTROLE_INTERNE_READ',
    'RCI_CONTROLE_INTERNE_AUDIT',
    'RCI_CAISSE_READ'
);

-- =============================================================================
-- VALIDATION
-- =============================================================================
SELECT CONCAT(
    'PATCH 11 OK | Role: ',
    IFNULL((SELECT code_role FROM role WHERE code_role = 'RCI'), 'NOT FOUND'),
    ' | Permissions mappees: ',
    IFNULL((SELECT COUNT(*) FROM role_permissions WHERE role_id = @rci_role_id), 0)
) AS migration_status;
