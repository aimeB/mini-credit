ALTER TABLE demande_retrait_epargne
    ADD COLUMN frais_retrait DECIMAL(18,2) NOT NULL DEFAULT 0.00;