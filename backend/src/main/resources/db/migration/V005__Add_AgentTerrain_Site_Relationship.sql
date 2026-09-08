-- Phase 4: Create N:M relationship between AgentTerrain and Site
-- Allows a field agent to be assigned to multiple operational sites

-- Step 1: Create JOIN TABLE for N:M relationship
CREATE TABLE agent_terrain_site (
    agent_terrain_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    
    PRIMARY KEY (agent_terrain_id, site_id),
    UNIQUE KEY uk_agent_site_unique (agent_terrain_id, site_id),
    
    CONSTRAINT fk_ats_agent FOREIGN KEY (agent_terrain_id) 
        REFERENCES agent_terrain(id) ON DELETE CASCADE,
    CONSTRAINT fk_ats_site FOREIGN KEY (site_id) 
        REFERENCES site(id) ON DELETE CASCADE,
    
    INDEX idx_ats_site_id (site_id)
);

-- Step 2: No data migration needed
-- The relationship is purely additive:
-- - agent_terrain.site_id remains unchanged (primary site assignment)
-- - agent_terrain_site table starts empty and is populated via API/service
