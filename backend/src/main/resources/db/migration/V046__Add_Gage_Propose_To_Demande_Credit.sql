-- V046: conservation du gage proposé dans le workflow crédit 3N

ALTER TABLE demande_credit
    ADD COLUMN gage_propose VARCHAR(255) NULL AFTER objet_credit;