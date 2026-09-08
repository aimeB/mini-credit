-- V048: Suppression definitive des anciens roles retires du referentiel 3N.
-- Objectif post-migration: SELECT id, code_role FROM role WHERE code_role IN (...) retourne 0 ligne.

START TRANSACTION;

INSERT IGNORE INTO role (code_role, libelle, description, is_active, date_creation, date_modification)
VALUES
    ('GESTIONNAIRE', 'Gestionnaire', 'Pre-analyse credit, suivi administratif et supervision terrain', TRUE, NOW(), NULL),
    ('CHEF_BUREAU', 'Chef de Bureau', 'Supervision, validation des credits, rapports', TRUE, NOW(), NULL);

SET @removed_gestionnaire_id := (SELECT id FROM role WHERE code_role = 'AGENT_BUREAU' LIMIT 1);
SET @removed_chef_bureau_id := (SELECT id FROM role WHERE code_role = 'RESPONSABLE' LIMIT 1);
SET @gestionnaire_id := (SELECT id FROM role WHERE code_role = 'GESTIONNAIRE' LIMIT 1);
SET @chef_bureau_id := (SELECT id FROM role WHERE code_role = 'CHEF_BUREAU' LIMIT 1);

UPDATE utilisateur
SET role_id = @gestionnaire_id,
    date_modification = NOW()
WHERE @removed_gestionnaire_id IS NOT NULL
  AND @gestionnaire_id IS NOT NULL
  AND role_id = @removed_gestionnaire_id;

UPDATE utilisateur
SET role_id = @chef_bureau_id,
    date_modification = NOW()
WHERE @removed_chef_bureau_id IS NOT NULL
  AND @chef_bureau_id IS NOT NULL
  AND role_id = @removed_chef_bureau_id;

UPDATE workflow_task
SET role_destinataire = 'GESTIONNAIRE',
    active_key = CASE
        WHEN active_key IS NULL THEN NULL
        ELSE REPLACE(active_key, '|AGENT_BUREAU|', '|GESTIONNAIRE|')
    END,
    date_modification = NOW()
WHERE role_destinataire = 'AGENT_BUREAU';

UPDATE workflow_task
SET role_destinataire = 'CHEF_BUREAU',
    active_key = CASE
        WHEN active_key IS NULL THEN NULL
        ELSE REPLACE(active_key, '|RESPONSABLE|', '|CHEF_BUREAU|')
    END,
    date_modification = NOW()
WHERE role_destinataire = 'RESPONSABLE';

UPDATE workflow_task
SET active_key = REPLACE(active_key, '|AGENT_BUREAU|', '|GESTIONNAIRE|'),
    date_modification = NOW()
WHERE active_key LIKE '%|AGENT_BUREAU|%';

UPDATE workflow_task
SET active_key = REPLACE(active_key, '|RESPONSABLE|', '|CHEF_BUREAU|'),
    date_modification = NOW()
WHERE active_key LIKE '%|RESPONSABLE|%';

INSERT IGNORE INTO role_permissions (role_id, permission_id, date_creation)
SELECT @gestionnaire_id, rp.permission_id, NOW()
FROM role_permissions rp
WHERE @removed_gestionnaire_id IS NOT NULL
  AND @gestionnaire_id IS NOT NULL
  AND rp.role_id = @removed_gestionnaire_id;

INSERT IGNORE INTO role_permissions (role_id, permission_id, date_creation)
SELECT @chef_bureau_id, rp.permission_id, NOW()
FROM role_permissions rp
WHERE @removed_chef_bureau_id IS NOT NULL
  AND @chef_bureau_id IS NOT NULL
  AND rp.role_id = @removed_chef_bureau_id;

DELETE rp
FROM role_permissions rp
JOIN role r ON r.id = rp.role_id
WHERE r.code_role IN ('AGENT_BUREAU', 'RESPONSABLE');

DELETE FROM role
WHERE code_role IN ('AGENT_BUREAU', 'RESPONSABLE');

COMMIT;

-- Verification attendue:
-- SELECT id, code_role FROM role WHERE code_role IN ('AGENT_BUREAU','RESPONSABLE');
