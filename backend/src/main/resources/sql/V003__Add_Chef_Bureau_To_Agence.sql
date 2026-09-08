-- V003__Add_Chef_Bureau_To_Agence.sql
-- Ajoute la relation Agence ← Chef de Bureau (Employe)
-- Une Agence peut avoir 0 ou 1 Chef de Bureau (FK OPTIONAL)
-- Si Chef de Bureau est supprimé, la relation est SET NULL

ALTER TABLE agence 
ADD COLUMN chef_bureau_id BIGINT NULLABLE;

ALTER TABLE agence 
ADD CONSTRAINT fk_agence_chef_bureau 
FOREIGN KEY (chef_bureau_id) REFERENCES employe(id) 
ON DELETE SET NULL;

CREATE INDEX idx_agence_chef_bureau ON agence(chef_bureau_id);
