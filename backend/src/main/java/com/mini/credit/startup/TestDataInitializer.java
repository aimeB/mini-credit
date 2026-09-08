package com.mini.credit.startup;

import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Initialiseur de données de test — DÉSACTIVÉ (base vide pour tests E2E manuels).
 *
 * Ce composant créait automatiquement des sites de test sans agence, ce qui
 * provoque une violation de contrainte (agence_id NOT NULL) depuis la Phase 3.
 *
 * Les sites doivent être créés manuellement via POST /api/sites
 * après avoir créé une agence via POST /api/agences.
 *
 * Pour réactiver la création automatique de données de test, ajouter un profil
 * dédié (ex: @Profile("dev-seed")) et fournir une agence valide.
 */
@Log4j2
@Component
@Profile("!test")
public class TestDataInitializer implements CommandLineRunner {

    public TestDataInitializer(SiteRepository siteRepository,
                               AgentTerrainRepository agentTerrainRepository) {
        // Paramètres conservés pour compatibilité Spring injection — non utilisés.
    }

    @Override
    public void run(String... args) {
        log.info("TestDataInitializer : désactivé — sites et agents à créer manuellement via l'interface.");
    }
}

