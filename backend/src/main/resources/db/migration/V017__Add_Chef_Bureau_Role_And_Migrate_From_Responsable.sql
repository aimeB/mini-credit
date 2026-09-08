-- PATCH P1: Migration progressive RESPONSABLE -> CHEF_BUREAU
-- Objectif: créer le rôle officiel CHEF_BUREAU et migrer les utilisateurs existants.

INSERT IGNORE INTO role (code_role, libelle, description, is_active, date_creation)
VALUES (
    'CHEF_BUREAU',
    'Chef de Bureau',
    'Supervision, validation des crédits, rapports',
    TRUE,
    NOW()
);

-- Permission dédiée à la validation de contrôle de session caisse
INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES (
    'SESSION_CAISSE_CONTROL_VALIDATE',
    'Valider le contrôle d''une session caisse',
    TRUE,
    NOW()
);

SET @chef_bureau_role_id = (SELECT id FROM role WHERE code_role = 'CHEF_BUREAU' LIMIT 1);
SET @responsable_role_id = (SELECT id FROM role WHERE code_role = 'RESPONSABLE' LIMIT 1);
SET @controleur_role_id = (SELECT id FROM role WHERE code_role = 'CONTROLEUR' LIMIT 1);
SET @admin_role_id = (SELECT id FROM role WHERE code_role = 'ADMIN' LIMIT 1);
SET @session_control_perm_id = (SELECT id FROM permissions WHERE code = 'SESSION_CAISSE_CONTROL_VALIDATE' LIMIT 1);

-- Copier les permissions RESPONSABLE vers CHEF_BUREAU (alias temporaire)
INSERT IGNORE INTO role_permissions (role_id, permission_id, date_creation)
SELECT @chef_bureau_role_id, rp.permission_id, NOW()
FROM role_permissions rp
WHERE rp.role_id = @responsable_role_id;

-- Attribuer explicitement la permission de validation contrôle à ADMIN et CONTROLEUR
INSERT IGNORE INTO role_permissions (role_id, permission_id, date_creation)
VALUES
    (@admin_role_id, @session_control_perm_id, NOW()),
    (@controleur_role_id, @session_control_perm_id, NOW());

-- Migration des utilisateurs existants RESPONSABLE vers CHEF_BUREAU
UPDATE utilisateur
SET role_id = @chef_bureau_role_id,
    date_modification = NOW()
WHERE role_id = @responsable_role_id;

SELECT CONCAT(
    'V017 OK | CHEF_BUREAU users: ',
    IFNULL((SELECT COUNT(*) FROM utilisateur WHERE role_id = @chef_bureau_role_id), 0),
    ' | RESPONSABLE users restants: ',
    IFNULL((SELECT COUNT(*) FROM utilisateur WHERE role_id = @responsable_role_id), 0)
) AS migration_status;
