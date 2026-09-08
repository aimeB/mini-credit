-- V001__Create_Agence.sql
-- Création de la table Agence - Conteneur physique des opérations
-- Date: 2026-06-04

CREATE TABLE IF NOT EXISTS agence (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code_agence VARCHAR(50) NOT NULL UNIQUE,
    nom_agence VARCHAR(100) NOT NULL,
    adresse VARCHAR(255),
    telephone VARCHAR(20),
    email VARCHAR(100),
    ville VARCHAR(100),
    actif BOOLEAN DEFAULT TRUE NOT NULL,
    description TEXT,
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification DATETIME ON UPDATE CURRENT_TIMESTAMP,

    -- Indexes pour performance
    INDEX idx_agence_actif (actif),
    INDEX idx_agence_code (code_agence),
    INDEX idx_agence_ville (ville),
    UNIQUE KEY uk_agence_code (code_agence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed data: créer les agences par défaut
INSERT INTO agence (code_agence, nom_agence, adresse, telephone, email, ville, actif, description)
VALUES 
    ('AGE-HQ', 'Agence Siège', 'Kinshasa, RDC', '+243971234567', 'siege@microcredit.cd', 'Kinshasa', TRUE, 'Siège principal du réseau'),
    ('AGE-KAS', 'Agence Kasai', 'Kasai, RDC', '+243971234568', 'kasai@microcredit.cd', 'Kasai', TRUE, 'Agence régionale Kasai');

-- Commit
COMMIT;
