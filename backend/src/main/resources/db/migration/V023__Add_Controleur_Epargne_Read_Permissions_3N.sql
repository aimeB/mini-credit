-- V023: Garantir les permissions lecture/controle epargne pour CONTROLEUR (workflow 3N)
-- Objectif: éviter les 403 sur les endpoints de consultation epargne pour CONTROLEUR
-- Règles:
--   - INSERT IGNORE pour éviter les doublons
--   - Pas d'octroi de permissions d'écriture (depot/retrait/paiement)

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('EPARGNE_COMPTE_READ', 'Voir les comptes épargne', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('EPARGNE_OPERATION_READ', 'Voir les opérations épargne', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('CONTROLEUR_EPARGNE_READ', 'Voir les comptes épargne (CONTROLEUR)', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('CONTROLEUR_EPARGNE_OPERATION_READ', 'Voir les opérations épargne (CONTROLEUR)', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('CONTROLEUR_RETRAITS_VALIDATE', 'Valider retraits épargne (vérif solde)', TRUE, NOW());

INSERT IGNORE INTO permissions (code, description, is_active, date_creation)
VALUES ('GARANTIE_CONTROL', 'Contrôler les garanties crédit', TRUE, NOW());

SET @controleur_role_id = (SELECT id FROM role WHERE code_role = 'CONTROLEUR' LIMIT 1);

INSERT IGNORE INTO role_permissions (role_id, permission_id, date_creation)
SELECT @controleur_role_id, p.id, NOW()
FROM permissions p
WHERE p.code IN (
    'EPARGNE_COMPTE_READ',
    'EPARGNE_OPERATION_READ',
    'CONTROLEUR_EPARGNE_READ',
    'CONTROLEUR_EPARGNE_OPERATION_READ',
    'CONTROLEUR_RETRAITS_VALIDATE',
    'GARANTIE_CONTROL'
);

SELECT CONCAT(
    'V023 OK | CONTROLEUR role_id=', IFNULL(@controleur_role_id, 0),
    ' | mapped perms=',
    IFNULL((
        SELECT COUNT(*)
        FROM role_permissions rp
        INNER JOIN permissions p ON p.id = rp.permission_id
        WHERE rp.role_id = @controleur_role_id
          AND p.code IN (
              'EPARGNE_COMPTE_READ',
              'EPARGNE_OPERATION_READ',
              'CONTROLEUR_EPARGNE_READ',
              'CONTROLEUR_EPARGNE_OPERATION_READ',
              'CONTROLEUR_RETRAITS_VALIDATE',
              'GARANTIE_CONTROL'
          )
    ), 0)
) AS migration_status;
