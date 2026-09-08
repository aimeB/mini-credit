-- V033: Finalisation RBAC 3N Gestionnaire / Chef de Bureau
-- Objectifs:
-- - corriger les bases ou V032 n'aurait pas migre tous les utilisateurs legacy
-- - garantir les permissions de consultation/pre-analyse du Gestionnaire
-- - retirer explicitement les permissions de validation financiere du Gestionnaire

START TRANSACTION;

-- 1) Roles officiels requis
INSERT IGNORE INTO role (code_role, libelle, description, is_active, date_creation, date_modification)
VALUES
    ('GESTIONNAIRE', 'Gestionnaire', 'Pre-analyse credit, suivi terrain, observations et reclamations', TRUE, NOW(), NULL),
    ('CHEF_BUREAU', 'Chef de Bureau', 'Responsable antenne, approbation credit et supervision', TRUE, NOW(), NULL);

SET @agent_bureau_role_id := (SELECT id FROM role WHERE code_role = 'AGENT_BUREAU' LIMIT 1);
SET @responsable_role_id := (SELECT id FROM role WHERE code_role = 'RESPONSABLE' LIMIT 1);
SET @gestionnaire_role_id := (SELECT id FROM role WHERE code_role = 'GESTIONNAIRE' LIMIT 1);
SET @chef_bureau_role_id := (SELECT id FROM role WHERE code_role = 'CHEF_BUREAU' LIMIT 1);

-- 2) Migration des utilisateurs encore rattaches aux anciens roles
UPDATE utilisateur
SET role_id = @gestionnaire_role_id,
    date_modification = NOW()
WHERE @agent_bureau_role_id IS NOT NULL
  AND role_id = @agent_bureau_role_id;

UPDATE utilisateur
SET role_id = @chef_bureau_role_id,
    date_modification = NOW()
WHERE @responsable_role_id IS NOT NULL
  AND role_id = @responsable_role_id;

-- 3) Permissions metier Gestionnaire attendues par le RBAC 3N
INSERT IGNORE INTO permissions (code, description, is_active, date_creation, date_modification)
VALUES
    ('AGENT_TERRAIN_READ', 'Voir les agents terrain', TRUE, NOW(), NULL),
    ('SITE_READ', 'Voir les sites', TRUE, NOW(), NULL),
    ('TERRAIN_ACTIVITY_READ', 'Voir les activites terrain', TRUE, NOW(), NULL),
    ('CREDIT_PRE_ANALYSE', 'Effectuer la pre-analyse credit', TRUE, NOW(), NULL),
    ('PERFORMANCE_TERRAIN_READ', 'Voir les performances terrain', TRUE, NOW(), NULL),
    ('RECLAMATION_READ', 'Voir les reclamations', TRUE, NOW(), NULL),
    ('RECLAMATION_COMMENT', 'Commenter les reclamations', TRUE, NOW(), NULL);

-- 4) Attribution explicite, sans copier en bloc les anciennes permissions AGENT_BUREAU
INSERT IGNORE INTO role_permissions (role_id, permission_id, date_creation)
SELECT @gestionnaire_role_id, p.id, NOW()
FROM permissions p
WHERE @gestionnaire_role_id IS NOT NULL
  AND p.code IN (
      'AGENT_TERRAIN_READ',
      'SITE_READ',
      'TERRAIN_ACTIVITY_READ',
      'CREDIT_PRE_ANALYSE',
      'PERFORMANCE_TERRAIN_READ',
      'RECLAMATION_READ',
      'RECLAMATION_COMMENT',
      'MEMBRE_READ',
      'DEMANDE_CREDIT_READ',
      'DEMANDE_CREDIT_CREATE',
      'ANALYSE_RISQUE_READ',
      'ANALYSE_RISQUE_CREATE',
      'CREDIT_READ',
      'FICHE_JOURNALIERE_READ',
      'DASHBOARD_GLOBAL_READ',
      'TASK_READ_OWN',
      'TASK_READ_ANTENNE',
      'TASK_COMPLETE',
      'USER_PASSWORD_CHANGE'
  );

-- 5) Least privilege: le Gestionnaire ne valide pas, ne decaisse pas, ne cloture pas
DELETE rp
FROM role_permissions rp
JOIN permissions p ON p.id = rp.permission_id
WHERE rp.role_id = @gestionnaire_role_id
  AND p.code IN (
      'CONTROLEUR_RECETTES_VALIDATE',
      'CONTROLEUR_RETRAITS_VALIDATE',
      'CREDIT_APPROVE',
      'CREDIT_DISBURSE',
      'SESSION_CAISSE_CLOSE',
      'SESSION_CAISSE_FINAL_CLOSE',
      'SESSION_CAISSE_CONTROL_VALIDATE',
      'DEPENSE_CAISSE_VALIDATE',
      'DEPENSE_CAISSE_PAY',
      'GARANTIE_VALIDATE',
      'GARANTIE_CONTROL'
  );

-- 6) Les roles legacy restent historiques mais inactifs
UPDATE role
SET is_active = FALSE,
    date_modification = NOW()
WHERE code_role IN ('AGENT_BUREAU', 'RESPONSABLE');

COMMIT;

-- Controles utiles:
-- SELECT r.code_role, COUNT(*) FROM utilisateur u JOIN role r ON r.id = u.role_id
-- WHERE r.code_role IN ('AGENT_BUREAU','RESPONSABLE','GESTIONNAIRE','CHEF_BUREAU') GROUP BY r.code_role;
-- SELECT p.code FROM role_permissions rp JOIN permissions p ON p.id = rp.permission_id
-- WHERE rp.role_id = (SELECT id FROM role WHERE code_role = 'GESTIONNAIRE') ORDER BY p.code;