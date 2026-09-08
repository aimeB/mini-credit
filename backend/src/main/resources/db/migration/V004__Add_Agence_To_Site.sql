-- Phase 3: Add Agence FK to Site
-- Each Site belongs to exactly ONE Agence
-- Migration handles existing data safely

-- Step 1: Add column as NULLABLE initially (safe for existing tables)
ALTER TABLE site ADD COLUMN agence_id BIGINT NULL;

-- Step 2: Set default value for all existing sites (use agence with ID=1)
UPDATE site SET agence_id = 1 WHERE agence_id IS NULL;

-- Step 3: Add FK constraint
ALTER TABLE site 
ADD CONSTRAINT fk_site_agence 
FOREIGN KEY (agence_id) REFERENCES agence(id);

-- Step 4: Create index for query performance
CREATE INDEX idx_site_agence_id ON site(agence_id);

-- Step 5: Make column NOT NULL (now safe since all rows have value from Step 2)
ALTER TABLE site MODIFY COLUMN agence_id BIGINT NOT NULL;
