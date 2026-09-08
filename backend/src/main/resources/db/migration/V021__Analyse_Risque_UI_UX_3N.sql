-- V021: Renforcement analyse risque 3N
-- - Identifiant analyse auto-généré
-- - Devises explicites pour tous les montants d'analyse
-- - Traçabilité score corrigé manuellement

ALTER TABLE analyse_risque
    ADD COLUMN analyse_id VARCHAR(80) NULL AFTER id,
    ADD COLUMN chiffre_affaires_devise VARCHAR(10) NULL AFTER chiffre_affaires_estime,
    ADD COLUMN revenu_net_devise VARCHAR(10) NULL AFTER revenu_net_estime,
    ADD COLUMN charges_mensuelles_devise VARCHAR(10) NULL AFTER charges_mensuelles,
    ADD COLUMN capacite_remboursement_devise VARCHAR(10) NULL AFTER capacite_remboursement,
    ADD COLUMN montant_demande_devise VARCHAR(10) NULL AFTER capacite_remboursement_devise,
    ADD COLUMN frais_demande_devise VARCHAR(10) NULL AFTER montant_demande_devise,
    ADD COLUMN depot_requis_devise VARCHAR(10) NULL AFTER frais_demande_devise,
    ADD COLUMN depot_paye_devise VARCHAR(10) NULL AFTER depot_requis_devise,
    ADD COLUMN score_risque_corrige_manuellement TINYINT(1) NOT NULL DEFAULT 0 AFTER score_risque;

UPDATE analyse_risque ar
JOIN demande_credit dc ON dc.id = ar.demande_credit_id
SET ar.analyse_id = CONCAT('ANR-', ar.demande_credit_id, '-', DATE_FORMAT(COALESCE(ar.date_creation, NOW()), '%Y%m%d'), '-', LPAD(ar.id, 4, '0'))
WHERE ar.analyse_id IS NULL OR ar.analyse_id = '';

UPDATE analyse_risque ar
JOIN demande_credit dc ON dc.id = ar.demande_credit_id
SET ar.chiffre_affaires_devise = IFNULL(ar.chiffre_affaires_devise, dc.devise),
    ar.revenu_net_devise = IFNULL(ar.revenu_net_devise, dc.devise),
    ar.charges_mensuelles_devise = IFNULL(ar.charges_mensuelles_devise, dc.devise),
    ar.capacite_remboursement_devise = IFNULL(ar.capacite_remboursement_devise, dc.devise),
    ar.montant_demande_devise = IFNULL(ar.montant_demande_devise, dc.devise),
    ar.frais_demande_devise = IFNULL(ar.frais_demande_devise, dc.devise),
    ar.depot_requis_devise = IFNULL(ar.depot_requis_devise, dc.devise),
    ar.depot_paye_devise = IFNULL(ar.depot_paye_devise, dc.devise);

ALTER TABLE analyse_risque
    MODIFY COLUMN analyse_id VARCHAR(80) NOT NULL,
    ADD UNIQUE KEY uk_analyse_risque_analyse_id (analyse_id);
