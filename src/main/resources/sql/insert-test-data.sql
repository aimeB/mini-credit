-- ================================================
-- 🌱 INSERT TEST DATA: Sites and Agents
-- ================================================

-- Insert test sites if they don't exist
INSERT INTO site (code_site, nom_site, ville, commune, adresse, actif, date_creation)
SELECT 'SITE-KIN-001' as code_site, 'Siège Principal Kinshasa' as nom_site, 'Kinshasa' as ville, 'Kalamu' as commune, 'Boulevard du 30 Juin' as adresse, 1 as actif, NOW() as date_creation
WHERE NOT EXISTS (SELECT 1 FROM site WHERE code_site = 'SITE-KIN-001')
UNION ALL
SELECT 'SITE-KIN-002', 'Agence Lingwala', 'Kinshasa', 'Lingwala', 'Avenue du Roi Baudouin', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM site WHERE code_site = 'SITE-KIN-002')
UNION ALL
SELECT 'SITE-LUM-001', 'Agence Lumumbashi', 'Lubumbashi', 'Katuba', 'Avenue Kasavubu', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM site WHERE code_site = 'SITE-LUM-001');

-- Get the site IDs for inserting agents
SET @site_kin_001 = (SELECT id FROM site WHERE code_site = 'SITE-KIN-001' LIMIT 1);
SET @site_kin_002 = (SELECT id FROM site WHERE code_site = 'SITE-KIN-002' LIMIT 1);

-- Insert test agents if they don't exist
INSERT INTO agent_terrain (matricule, site_id, date_creation)
SELECT 'AGT-001' as matricule, @site_kin_001 as site_id, NOW() as date_creation
WHERE @site_kin_001 IS NOT NULL 
  AND NOT EXISTS (SELECT 1 FROM agent_terrain WHERE matricule = 'AGT-001')
UNION ALL
SELECT 'AGT-002', @site_kin_001, NOW()
WHERE @site_kin_001 IS NOT NULL 
  AND NOT EXISTS (SELECT 1 FROM agent_terrain WHERE matricule = 'AGT-002')
UNION ALL
SELECT 'AGT-003', @site_kin_002, NOW()
WHERE @site_kin_002 IS NOT NULL 
  AND NOT EXISTS (SELECT 1 FROM agent_terrain WHERE matricule = 'AGT-003');

-- Verify insertion
SELECT '✓ Sites inserted/verified:' as status, COUNT(*) as count FROM site;
SELECT '✓ Agents inserted/verified:' as status, COUNT(*) as count FROM agent_terrain;
