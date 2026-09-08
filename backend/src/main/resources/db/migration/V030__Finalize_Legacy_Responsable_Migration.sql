-- Finalisation migration legacy RESPONSABLE -> CHEF_BUREAU
-- CONSERVATEUR: la migration opérationnelle est consolidée dans V017.
-- Cette V030 est volontairement non destructive et sert uniquement de vérification.
-- Le rôle RESPONSABLE est conservé comme alias technique temporaire.

SET @chef_bureau_role_id = (SELECT id FROM role WHERE code_role = 'CHEF_BUREAU' LIMIT 1);
SET @responsable_role_id = (SELECT id FROM role WHERE code_role = 'RESPONSABLE' LIMIT 1);

SELECT CONCAT(
    'V030 CHECK | CHEF_BUREAU present: ',
    IF(@chef_bureau_role_id IS NULL, 'NO', 'YES'),
    ' | CHEF_BUREAU users: ',
    IFNULL((SELECT COUNT(*) FROM utilisateur WHERE role_id = @chef_bureau_role_id), 0),
    ' | RESPONSABLE users restants: ',
    IFNULL((SELECT COUNT(*) FROM utilisateur WHERE role_id = @responsable_role_id), 0)
) AS migration_status;
