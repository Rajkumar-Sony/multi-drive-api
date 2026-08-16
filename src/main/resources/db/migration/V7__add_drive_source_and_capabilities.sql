ALTER TABLE google_drive_items
    ADD COLUMN source_id BIGINT;


-- Backfill My Drive items.

UPDATE google_drive_items item
SET source_id = source.id
FROM google_drive_sources source
WHERE item.connection_id = source.connection_id
  AND item.source_type = 'MY_DRIVE'
  AND source.source_type = 'MY_DRIVE';


-- Backfill Shared Drive items.

UPDATE google_drive_items item
SET source_id = source.id
FROM google_drive_sources source
WHERE item.connection_id = source.connection_id
  AND item.source_type = 'SHARED_DRIVE'
  AND item.drive_id = source.google_drive_id
  AND source.source_type = 'SHARED_DRIVE';


ALTER TABLE google_drive_items
    ALTER COLUMN source_id SET NOT NULL;


ALTER TABLE google_drive_items
    ADD CONSTRAINT fk_google_drive_items_source
        FOREIGN KEY (source_id)
        REFERENCES google_drive_sources(id)
        ON DELETE CASCADE;


CREATE INDEX idx_google_drive_items_source
    ON google_drive_items(source_id);


-- ============================================================
-- Google Drive item capabilities
-- ============================================================

ALTER TABLE google_drive_items
    ADD COLUMN cap_can_move_children_out_of_drive BOOLEAN,
    ADD COLUMN cap_can_read_drive BOOLEAN,
    ADD COLUMN cap_can_edit BOOLEAN,
    ADD COLUMN cap_can_copy BOOLEAN,
    ADD COLUMN cap_can_comment BOOLEAN,
    ADD COLUMN cap_can_add_children BOOLEAN,
    ADD COLUMN cap_can_delete BOOLEAN,
    ADD COLUMN cap_can_download BOOLEAN,
    ADD COLUMN cap_can_list_children BOOLEAN,
    ADD COLUMN cap_can_remove_children BOOLEAN,
    ADD COLUMN cap_can_rename BOOLEAN,
    ADD COLUMN cap_can_trash BOOLEAN,
    ADD COLUMN cap_can_read_revisions BOOLEAN,
    ADD COLUMN cap_can_change_copy_requires_writer_permission BOOLEAN,
    ADD COLUMN cap_can_untrash BOOLEAN,
    ADD COLUMN cap_can_modify_content BOOLEAN,
    ADD COLUMN cap_can_delete_children BOOLEAN,
    ADD COLUMN cap_can_trash_children BOOLEAN,
    ADD COLUMN cap_can_move_item_out_of_drive BOOLEAN,
    ADD COLUMN cap_can_add_my_drive_parent BOOLEAN,
    ADD COLUMN cap_can_remove_my_drive_parent BOOLEAN,
    ADD COLUMN cap_can_move_item_within_drive BOOLEAN,
    ADD COLUMN cap_can_share BOOLEAN,
    ADD COLUMN cap_can_move_children_within_drive BOOLEAN,
    ADD COLUMN cap_can_add_folder_from_another_drive BOOLEAN,
    ADD COLUMN cap_can_change_security_update_enabled BOOLEAN,
    ADD COLUMN cap_can_accept_ownership BOOLEAN,
    ADD COLUMN cap_can_read_labels BOOLEAN,
    ADD COLUMN cap_can_modify_labels BOOLEAN,
    ADD COLUMN cap_can_modify_editor_content_restriction BOOLEAN,
    ADD COLUMN cap_can_modify_owner_content_restriction BOOLEAN,
    ADD COLUMN cap_can_remove_content_restriction BOOLEAN,
    ADD COLUMN cap_can_disable_inherited_permissions BOOLEAN,
    ADD COLUMN cap_can_enable_inherited_permissions BOOLEAN,
    ADD COLUMN cap_can_change_item_download_restriction BOOLEAN,
    ADD COLUMN cap_can_start_approval BOOLEAN;


-- ============================================================
-- Google Drive source metadata
-- ============================================================

ALTER TABLE google_drive_sources
    ADD COLUMN hidden BOOLEAN,
    ADD COLUMN google_created_time TIMESTAMP WITH TIME ZONE;


-- ============================================================
-- Google Drive source capabilities
-- ============================================================

ALTER TABLE google_drive_sources
    ADD COLUMN cap_can_add_children BOOLEAN,
    ADD COLUMN cap_can_comment BOOLEAN,
    ADD COLUMN cap_can_copy BOOLEAN,
    ADD COLUMN cap_can_delete_drive BOOLEAN,
    ADD COLUMN cap_can_download BOOLEAN,
    ADD COLUMN cap_can_edit BOOLEAN,
    ADD COLUMN cap_can_list_children BOOLEAN,
    ADD COLUMN cap_can_manage_members BOOLEAN,
    ADD COLUMN cap_can_read_revisions BOOLEAN,
    ADD COLUMN cap_can_rename BOOLEAN,
    ADD COLUMN cap_can_rename_drive BOOLEAN,
    ADD COLUMN cap_can_change_drive_background BOOLEAN,
    ADD COLUMN cap_can_share BOOLEAN,
    ADD COLUMN cap_can_change_copy_requires_writer_permission_restriction BOOLEAN,
    ADD COLUMN cap_can_change_domain_users_only_restriction BOOLEAN,
    ADD COLUMN cap_can_change_drive_members_only_restriction BOOLEAN,
    ADD COLUMN cap_can_change_sharing_folders_requires_organizer_permission_restriction BOOLEAN,
    ADD COLUMN cap_can_reset_drive_restrictions BOOLEAN,
    ADD COLUMN cap_can_delete_children BOOLEAN,
    ADD COLUMN cap_can_trash_children BOOLEAN,
    ADD COLUMN cap_can_change_download_restriction BOOLEAN;


-- ============================================================
-- Shared Drive restrictions
-- ============================================================

ALTER TABLE google_drive_sources
    ADD COLUMN restriction_copy_requires_writer_permission BOOLEAN,
    ADD COLUMN restriction_domain_users_only BOOLEAN,
    ADD COLUMN restriction_drive_members_only BOOLEAN,
    ADD COLUMN restriction_admin_managed BOOLEAN,
    ADD COLUMN restriction_sharing_folders_requires_organizer BOOLEAN,
    ADD COLUMN restriction_download_readers BOOLEAN,
    ADD COLUMN restriction_download_writers BOOLEAN;