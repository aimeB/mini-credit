ALTER TABLE demande_retrait_epargne
    ADD COLUMN reference_retrait VARCHAR(20) NULL;

UPDATE demande_retrait_epargne
SET reference_retrait = CONCAT('RET-', YEAR(date_demande), '-', LPAD(id, 4, '0'))
WHERE reference_retrait IS NULL;

CREATE UNIQUE INDEX uk_demande_retrait_epargne_reference
    ON demande_retrait_epargne (reference_retrait);
