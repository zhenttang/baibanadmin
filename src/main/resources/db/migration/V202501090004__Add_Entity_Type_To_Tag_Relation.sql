-- Add entity_type column to document_tag_relations to distinguish DOCUMENT/POST
ALTER TABLE `document_tag_relations`
    ADD COLUMN `entity_type` VARCHAR(20) NOT NULL DEFAULT 'DOCUMENT' AFTER `tag_id`;

-- Optional index to accelerate queries filtering by entity_type
CREATE INDEX `idx_entity_type` ON `document_tag_relations` (`entity_type`);

