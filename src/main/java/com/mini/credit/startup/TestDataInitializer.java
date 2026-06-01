package com.mini.credit.startup;

import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.repository.site.SiteRepository;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * ⚙️ Initialize test data for Site and Agent entities
 * This runs on application startup if no data exists
 */
@Log4j2
@Component
public class TestDataInitializer implements CommandLineRunner {

    private final SiteRepository siteRepository;
    private final AgentTerrainRepository agentTerrainRepository;

    public TestDataInitializer(SiteRepository siteRepository, AgentTerrainRepository agentTerrainRepository) {
        this.siteRepository = siteRepository;
        this.agentTerrainRepository = agentTerrainRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("🌱 Initializing test data for Sites and Agents...");

        // Check if sites already exist
        long siteCount = siteRepository.count();
        if (siteCount == 0) {
            log.info("📍 Creating test sites...");

            // Create test site 1
            Site site1 = Site.builder()
                    .codeSite("SITE-KIN-001")
                    .nomSite("Siège Principal Kinshasa")
                    .ville("Kinshasa")
                    .commune("Kalamu")
                    .adresse("Boulevard du 30 Juin")
                    .actif(true)
                    .build();
            siteRepository.save(site1);
            log.info("✓ Created site: {}", site1.getNomSite());

            // Create test site 2
            Site site2 = Site.builder()
                    .codeSite("SITE-KIN-002")
                    .nomSite("Agence Lingwala")
                    .ville("Kinshasa")
                    .commune("Lingwala")
                    .adresse("Avenue du Roi Baudouin")
                    .actif(true)
                    .build();
            siteRepository.save(site2);
            log.info("✓ Created site: {}", site2.getNomSite());

            // Create test site 3
            Site site3 = Site.builder()
                    .codeSite("SITE-LUM-001")
                    .nomSite("Agence Lubumbashi")
                    .ville("Lubumbashi")
                    .commune("Katuba")
                    .adresse("Avenue Kasavubu")
                    .actif(true)
                    .build();
            siteRepository.save(site3);
            log.info("✓ Created site: {}", site3.getNomSite());
        } else {
            log.info("✓ Sites already exist, skipping creation ({} sites found)", siteCount);
        }

        // Check if agents already exist
        long agentCount = agentTerrainRepository.count();
        if (agentCount == 0) {
            log.info("👤 Skipping agent creation - agents will be created when users are assigned the AGENT_TERRAIN role");
        } else {
            log.info("✓ Agents already exist ({} agents found)", agentCount);
        }

        log.info("✅ Test data initialization completed!");
    }
}
