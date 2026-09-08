-- Corrige l'accès CONTROLEUR au workflow de contrôle des sessions caisse.
-- Les endpoints utilisent les permissions canoniques CAISSE_* / SESSION_CAISSE_*.
-- Les anciennes permissions CONTROLEUR_* restent supportées côté code pour compatibilité.

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES
    ('CAISSE_READ', 'Voir les caisses', TRUE, NOW()),
    ('OPERATION_CAISSE_READ', 'Voir les opérations caisse', TRUE, NOW()),
    ('JOURNAL_CAISSE_READ', 'Consulter le journal de caisse', TRUE, NOW()),
    ('SESSION_CAISSE_CONTROL_VALIDATE', 'Valider le contrôle d''une session caisse', TRUE, NOW()),
    ('SESSION_CAISSE_ANOMALIE_READ', 'Consulter les anomalies de session caisse', TRUE, NOW());

SET @controleur_role_id = (SELECT id FROM role WHERE code_role = 'CONTROLEUR' LIMIT 1);

INSERT IGNORE INTO role_permissions (role_id, permission_id, date_creation)
SELECT @controleur_role_id, p.id, NOW()
FROM permissions p
WHERE @controleur_role_id IS NOT NULL
  AND p.code IN (
      'CAISSE_READ',
      'OPERATION_CAISSE_READ',
      'JOURNAL_CAISSE_READ',
      'SESSION_CAISSE_CONTROL_VALIDATE',
      'SESSION_CAISSE_ANOMALIE_READ'
  );