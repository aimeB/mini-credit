ALTER TABLE collecte_membre_ligne
    ADD COLUMN montant_souhaite DECIMAL(15,2) NULL AFTER commentaire,
    ADD COLUMN objet_credit VARCHAR(255) NULL AFTER montant_souhaite,
    ADD COLUMN gage_propose VARCHAR(255) NULL AFTER objet_credit,
    ADD COLUMN modalite_remboursement ENUM('JOURNALIERE','HEBDOMADAIRE','MENSUELLE') NULL AFTER gage_propose;
