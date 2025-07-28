-- Cleanup script for duplicate templates
-- This script removes duplicate templates keeping only the most recent one by name

-- First, let's see what duplicates we have
SELECT name, COUNT(*) as count, MIN(created_at) as oldest, MAX(created_at) as newest
FROM simple_templates 
GROUP BY name 
HAVING COUNT(*) > 1;

-- Delete duplicates keeping only the most recent one
WITH duplicates AS (
    SELECT id, name, created_at,
           ROW_NUMBER() OVER (PARTITION BY name ORDER BY created_at DESC) as rn
    FROM simple_templates
)
DELETE FROM simple_templates 
WHERE id IN (
    SELECT id FROM duplicates WHERE rn > 1
);

-- Verify cleanup
SELECT name, COUNT(*) as count, created_at
FROM simple_templates 
GROUP BY name, created_at 
ORDER BY name;