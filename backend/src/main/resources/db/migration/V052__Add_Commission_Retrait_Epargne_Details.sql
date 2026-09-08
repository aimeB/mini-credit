ALTER TABLE demande_retrait_epargne
    ADD COLUMN taux_commission_retrait DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN montant_total_debite DECIMAL(18,2) NOT NULL DEFAULT 0.00;

UPDATE demande_retrait_epargne
SET montant_total_debite = COALESCE(montant_demande, 0.00) + COALESCE(frais_retrait, 0.00)
WHERE montant_total_debite = 0.00;