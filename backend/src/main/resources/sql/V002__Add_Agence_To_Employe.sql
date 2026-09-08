-- V002__Add_Agence_To_Employe.sql
-- Ajoute la relation Employe → Agence
-- Chaque Employe appartient à UNE agence (FK NOT NULL)

ALTER TABLE employe 
ADD COLUMN agence_id BIGINT NOT NULL;

ALTER TABLE employe 
ADD CONSTRAINT fk_employe_agence 
FOREIGN KEY (agence_id) REFERENCES agence(id);

CREATE INDEX idx_employe_agence ON employe(agence_id);
