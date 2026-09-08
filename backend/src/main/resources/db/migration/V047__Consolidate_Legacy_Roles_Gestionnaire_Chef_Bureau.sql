-- V047: Consolidation definitive des roles legacy AGENT_BUREAU / RESPONSABLE
-- Objectif: aucune donnee operationnelle active ne doit rester rattachee aux anciens roles.
-- Les audits historiques ne sont pas modifies afin de conserver la tracabilite.

START TRANSACTION;

INSERT IGNORE INTO role (code_role, libelle, description, is_active, date_creation, date_modification)
VALUES
    ('GESTIONNAIRE', 'Gestionnaire', 'Pre-analyse credit, suivi terrain, observations et reclamations', TRUE, NOW(), NULL),
    ('CHEF_BUREAU', 'Chef de Bureau', 'Responsable antenne, approbation credit et supervision', TRUE, NOW(), NULL);

SET @agent_bureau_role_id := (SELECT id FROM role WHERE code_role = 'AGENT_BUREAU' LIMIT 1);
SET @responsable_role_id := (SELECT id FROM role WHERE code_role = 'RESPONSABLE' LIMIT 1);
SET @gestionnaire_role_id := (SELECT id FROM role WHERE code_role = 'GESTIONNAIRE' LIMIT 1);
SET @chef_bureau_role_id := (SELECT id FROM role WHERE code_role = 'CHEF_BUREAU' LIMIT 1);

-- Utilisateurs actifs et inactifs: le token ne doit plus pouvoir porter AGENT_BUREAU / RESPONSABLE.
UPDATE utilisateur
SET role_id = @gestionnaire_role_id,
    date_modification = NOW()
WHERE @agent_bureau_role_id IS NOT NULL
  AND @gestionnaire_role_id IS NOT NULL
  AND role_id = @agent_bureau_role_id;

UPDATE utilisateur
SET role_id = @chef_bureau_role_id,
    date_modification = NOW()
WHERE @responsable_role_id IS NOT NULL
  AND @chef_bureau_role_id IS NOT NULL
  AND role_id = @responsable_role_id;

-- Taches workflow operationnelles: aucune tache active ne doit cibler les roles legacy.
UPDATE workflow_task
SET role_destinataire = 'GESTIONNAIRE',
    active_key = CASE
        WHEN active_key IS NULL THEN NULL
        ELSE REPLACE(active_key, '|AGENT_BUREAU|', '|GESTIONNAIRE|')
    END,
    date_modification = NOW()
WHERE role_destinataire = 'AGENT_BUREAU'
  AND statut IN ('A_FAIRE', 'EN_COURS');

UPDATE workflow_task
SET role_destinataire = 'CHEF_BUREAU',
    active_key = CASE
        WHEN active_key IS NULL THEN NULL
        ELSE REPLACE(active_key, '|RESPONSABLE|', '|CHEF_BUREAU|')
    END,
    date_modification = NOW()
WHERE role_destinataire = 'RESPONSABLE'
  AND statut IN ('A_FAIRE', 'EN_COURS');

-- Nettoyage defensif: si une ancienne tache terminee garde une active_key legacy, elle ne doit pas bloquer la recreation.
UPDATE workflow_task
SET active_key = NULL,
    date_modification = NOW()
WHERE statut NOT IN ('A_FAIRE', 'EN_COURS')
  AND active_key IS NOT NULL
  AND (active_key LIKE '%|AGENT_BUREAU|%' OR active_key LIKE '%|RESPONSABLE|%');

-- Permissions: copier les permissions legacy utiles vers les roles officiels avant desactivation.
INSERT IGNORE INTO role_permissions (role_id, permission_id, date_creation)
SELECT @gestionnaire_role_id, rp.permission_id, NOW()
FROM role_permissions rp
WHERE @agent_bureau_role_id IS NOT NULL
  AND @gestionnaire_role_id IS NOT NULL
  AND rp.role_id = @agent_bureau_role_id;

INSERT IGNORE INTO role_permissions (role_id, permission_id, date_creation)
SELECT @chef_bureau_role_id, rp.permission_id, NOW()
FROM role_permissions rp
WHERE @responsable_role_id IS NOT NULL
  AND @chef_bureau_role_id IS NOT NULL
  AND rp.role_id = @responsable_role_id;

-- Garantir les permissions minimales Mes actions / pre-analyse pour le Gestionnaire.
INSERT IGNORE INTO role_permissions (role_id, permission_id, date_creation)
SELECT @gestionnaire_role_id, p.id, NOW()
FROM permissions p
WHERE @gestionnaire_role_id IS NOT NULL
  AND p.code IN ('TASK_READ_OWN', 'TASK_READ_ANTENNE', 'CREDIT_PRE_ANALYSE', 'DEMANDE_CREDIT_READ', 'ANALYSE_RISQUE_READ', 'ANALYSE_RISQUE_CREATE');

-- Les roles legacy restent presents uniquement pour compatibilite enum/historique, mais inactifs.
UPDATE role
SET is_active = FALSE,
    date_modification = NOW()
WHERE code_role IN ('AGENT_BUREAU', 'RESPONSABLE');

COMMIT;

-- Verification manuelle post-migration:
-- SELECT r.code_role, COUNT(*) nb FROM utilisateur u JOIN role r ON r.id = u.role_id WHERE r.code_role IN ('AGENT_BUREAU','RESPONSABLE','GESTIONNAIRE','CHEF_BUREAU') GROUP BY r.code_role;
-- SELECT role_destinataire, statut, COUNT(*) nb FROM workflow_task WHERE role_destinataire IN ('AGENT_BUREAU','RESPONSABLE','GESTIONNAIRE','CHEF_BUREAU') GROUP BY role_destinataire, statut;
