-- V032: Migration corrective des roles legacy RESPONSABLE et AGENT_BUREAU
-- Objectif:
-- - conserver l'historique en base
-- - migrer les donnees actives vers les roles officiels 3N
-- - ne plus attribuer de nouvelles permissions aux roles legacy
-- - laisser les lignes role en place, mais les desactiver apres migration

START TRANSACTION;

-- 1) Garantir l'existence des roles cibles
INSERT IGNORE INTO role (code_role, libelle, description, is_active, date_creation, date_modification)
VALUES
    (
        'CHEF_BUREAU',
        'Chef de Bureau',
        'Supervision, validation des credits, rapports',
        TRUE,
        NOW(),
        NULL
    ),
    (
        'GESTIONNAIRE',
        'Gestionnaire',
        'Pre-analyse credit, suivi administratif et supervision terrain',
        TRUE,
        NOW(),
        NULL
    );

SET @responsable_role_id := (SELECT id FROM role WHERE code_role = 'RESPONSABLE' LIMIT 1);
SET @agent_bureau_role_id := (SELECT id FROM role WHERE code_role = 'AGENT_BUREAU' LIMIT 1);
SET @chef_bureau_role_id := (SELECT id FROM role WHERE code_role = 'CHEF_BUREAU' LIMIT 1);
SET @gestionnaire_role_id := (SELECT id FROM role WHERE code_role = 'GESTIONNAIRE' LIMIT 1);

-- 2) Copier les permissions existantes des roles legacy vers les roles cibles si elles manquent
INSERT IGNORE INTO role_permissions (role_id, permission_id, date_creation)
SELECT @chef_bureau_role_id, rp.permission_id, NOW()
FROM role_permissions rp
WHERE rp.role_id = @responsable_role_id;

INSERT IGNORE INTO role_permissions (role_id, permission_id, date_creation)
SELECT @gestionnaire_role_id, rp.permission_id, NOW()
FROM role_permissions rp
WHERE rp.role_id = @agent_bureau_role_id;

-- 3) Migrer les utilisateurs vers les roles officiels
UPDATE utilisateur
SET role_id = @chef_bureau_role_id,
    date_modification = NOW()
WHERE role_id = @responsable_role_id;

UPDATE utilisateur
SET role_id = @gestionnaire_role_id,
    date_modification = NOW()
WHERE role_id = @agent_bureau_role_id;

-- 4) Migrer les WorkflowTask actives vers les roles officiels
--    Statuts actifs pris en compte: A_FAIRE, EN_COURS

-- 4.a) RESPONSABLE -> CHEF_BUREAU si aucune tache cible active equivalente n'existe deja
UPDATE workflow_task legacy_task
SET legacy_task.role_destinataire = 'CHEF_BUREAU',
    legacy_task.active_key = CONCAT(
        legacy_task.module, '|', legacy_task.type_action, '|', legacy_task.entity_type, '|',
        legacy_task.entity_id, '|CHEF_BUREAU|', legacy_task.antenne_id, '|ACTIVE'
    ),
    legacy_task.date_modification = NOW()
WHERE legacy_task.role_destinataire = 'RESPONSABLE'
  AND legacy_task.statut IN ('A_FAIRE', 'EN_COURS')
  AND NOT EXISTS (
      SELECT 1
      FROM workflow_task target_task
      WHERE target_task.module = legacy_task.module
        AND target_task.type_action = legacy_task.type_action
        AND target_task.entity_type = legacy_task.entity_type
        AND target_task.entity_id = legacy_task.entity_id
        AND target_task.antenne_id = legacy_task.antenne_id
        AND target_task.role_destinataire = 'CHEF_BUREAU'
        AND target_task.statut IN ('A_FAIRE', 'EN_COURS')
        AND target_task.id <> legacy_task.id
  );

-- 4.b) Si une tache cible active equivalente existe deja, archiver la tache legacy
UPDATE workflow_task legacy_task
SET legacy_task.statut = 'TERMINEE',
    legacy_task.completed_at = COALESCE(legacy_task.completed_at, NOW()),
    legacy_task.commentaire = CASE
        WHEN legacy_task.commentaire IS NULL OR legacy_task.commentaire = ''
            THEN 'Migrated to CHEF_BUREAU'
        ELSE CONCAT(legacy_task.commentaire, ' | Migrated to CHEF_BUREAU')
    END,
    legacy_task.active_key = NULL,
    legacy_task.date_modification = NOW()
WHERE legacy_task.role_destinataire = 'RESPONSABLE'
  AND legacy_task.statut IN ('A_FAIRE', 'EN_COURS')
  AND EXISTS (
      SELECT 1
      FROM workflow_task target_task
      WHERE target_task.module = legacy_task.module
        AND target_task.type_action = legacy_task.type_action
        AND target_task.entity_type = legacy_task.entity_type
        AND target_task.entity_id = legacy_task.entity_id
        AND target_task.antenne_id = legacy_task.antenne_id
        AND target_task.role_destinataire = 'CHEF_BUREAU'
        AND target_task.statut IN ('A_FAIRE', 'EN_COURS')
        AND target_task.id <> legacy_task.id
  );

-- 4.c) AGENT_BUREAU -> GESTIONNAIRE si aucune tache cible active equivalente n'existe deja
UPDATE workflow_task legacy_task
SET legacy_task.role_destinataire = 'GESTIONNAIRE',
    legacy_task.active_key = CONCAT(
        legacy_task.module, '|', legacy_task.type_action, '|', legacy_task.entity_type, '|',
        legacy_task.entity_id, '|GESTIONNAIRE|', legacy_task.antenne_id, '|ACTIVE'
    ),
    legacy_task.date_modification = NOW()
WHERE legacy_task.role_destinataire = 'AGENT_BUREAU'
  AND legacy_task.statut IN ('A_FAIRE', 'EN_COURS')
  AND NOT EXISTS (
      SELECT 1
      FROM workflow_task target_task
      WHERE target_task.module = legacy_task.module
        AND target_task.type_action = legacy_task.type_action
        AND target_task.entity_type = legacy_task.entity_type
        AND target_task.entity_id = legacy_task.entity_id
        AND target_task.antenne_id = legacy_task.antenne_id
        AND target_task.role_destinataire = 'GESTIONNAIRE'
        AND target_task.statut IN ('A_FAIRE', 'EN_COURS')
        AND target_task.id <> legacy_task.id
  );

-- 4.d) Si une tache cible active equivalente existe deja, archiver la tache legacy
UPDATE workflow_task legacy_task
SET legacy_task.statut = 'TERMINEE',
    legacy_task.completed_at = COALESCE(legacy_task.completed_at, NOW()),
    legacy_task.commentaire = CASE
        WHEN legacy_task.commentaire IS NULL OR legacy_task.commentaire = ''
            THEN 'Migrated to GESTIONNAIRE'
        ELSE CONCAT(legacy_task.commentaire, ' | Migrated to GESTIONNAIRE')
    END,
    legacy_task.active_key = NULL,
    legacy_task.date_modification = NOW()
WHERE legacy_task.role_destinataire = 'AGENT_BUREAU'
  AND legacy_task.statut IN ('A_FAIRE', 'EN_COURS')
  AND EXISTS (
      SELECT 1
      FROM workflow_task target_task
      WHERE target_task.module = legacy_task.module
        AND target_task.type_action = legacy_task.type_action
        AND target_task.entity_type = legacy_task.entity_type
        AND target_task.entity_id = legacy_task.entity_id
        AND target_task.antenne_id = legacy_task.antenne_id
        AND target_task.role_destinataire = 'GESTIONNAIRE'
        AND target_task.statut IN ('A_FAIRE', 'EN_COURS')
        AND target_task.id <> legacy_task.id
  );

-- 5) Desactiver les roles legacy sans suppression physique
UPDATE role
SET is_active = FALSE,
    date_modification = NOW()
WHERE code_role IN ('RESPONSABLE', 'AGENT_BUREAU');

COMMIT;

-- =====================================================================
-- Controles manuels avant/apres
-- =====================================================================
-- 1. Utilisateurs par role:
-- SELECT r.code_role, COUNT(*)
-- FROM utilisateur u
-- JOIN role r ON r.id = u.role_id
-- WHERE r.code_role IN ('RESPONSABLE','AGENT_BUREAU','CHEF_BUREAU','GESTIONNAIRE')
-- GROUP BY r.code_role;

-- 2. WorkflowTask par role_destinataire:
-- SELECT role_destinataire, COUNT(*)
-- FROM workflow_task
-- WHERE role_destinataire IN ('RESPONSABLE','AGENT_BUREAU','CHEF_BUREAU','GESTIONNAIRE')
-- GROUP BY role_destinataire;

-- 3. Permissions TASK par role cible:
-- SELECT r.code_role, p.code
-- FROM role r
-- JOIN role_permissions rp ON rp.role_id = r.id
-- JOIN permissions p ON p.id = rp.permission_id
-- WHERE r.code_role IN ('CHEF_BUREAU','GESTIONNAIRE')
--   AND p.code LIKE 'TASK_%'
-- ORDER BY r.code_role, p.code;

-- 4. Controle final obligatoire:
-- SELECT COUNT(*) AS nb_users_responsable
-- FROM utilisateur u
-- JOIN role r ON r.id = u.role_id
-- WHERE u.actif = 1
--   AND u.is_enabled = 1
--   AND r.code_role = 'RESPONSABLE';

-- SELECT COUNT(*) AS nb_users_agent_bureau
-- FROM utilisateur u
-- JOIN role r ON r.id = u.role_id
-- WHERE u.actif = 1
--   AND u.is_enabled = 1
--   AND r.code_role = 'AGENT_BUREAU';

-- SELECT COUNT(*) AS nb_tasks_responsable
-- FROM workflow_task
-- WHERE statut IN ('A_FAIRE', 'EN_COURS')
--   AND role_destinataire = 'RESPONSABLE';

-- SELECT COUNT(*) AS nb_tasks_agent_bureau
-- FROM workflow_task
-- WHERE statut IN ('A_FAIRE', 'EN_COURS')
--   AND role_destinataire = 'AGENT_BUREAU';