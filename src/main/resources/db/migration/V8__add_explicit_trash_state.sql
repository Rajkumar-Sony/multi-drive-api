ALTER TABLE google_drive_items
    ADD COLUMN explicitly_trashed BOOLEAN;


-- Existing visible items are known not to be explicitly trashed.
-- Existing trashed rows are left NULL because their exact historical
-- state might not be known yet.

UPDATE google_drive_items
SET explicitly_trashed = FALSE
WHERE trashed = FALSE;


-- Folder traversal will frequently use:
--
-- connection_id
-- source_id
-- parent_id
--
-- so index that relationship for recursive hierarchy operations.

CREATE INDEX idx_google_drive_items_hierarchy
    ON google_drive_items(
        connection_id,
        source_id,
        parent_id
    );


CREATE INDEX idx_google_drive_items_trash
    ON google_drive_items(
        connection_id,
        trashed
    );
