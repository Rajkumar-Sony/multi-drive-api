package com.multidrive.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class GoogleDriveSourceRestrictions {

    @Column(name = "restriction_copy_requires_writer_permission")
    private Boolean copyRequiresWriterPermission;

    @Column(name = "restriction_domain_users_only")
    private Boolean domainUsersOnly;

    @Column(name = "restriction_drive_members_only")
    private Boolean driveMembersOnly;

    @Column(name = "restriction_admin_managed")
    private Boolean adminManagedRestrictions;

    @Column(name = "restriction_sharing_folders_requires_organizer")
    private Boolean sharingFoldersRequiresOrganizerPermission;

    @Column(name = "restriction_download_readers")
    private Boolean downloadRestrictedForReaders;

    @Column(name = "restriction_download_writers")
    private Boolean downloadRestrictedForWriters;

    public Boolean getCopyRequiresWriterPermission() {
        return copyRequiresWriterPermission;
    }

    public void setCopyRequiresWriterPermission(Boolean value) {
        this.copyRequiresWriterPermission = value;
    }

    public Boolean getDomainUsersOnly() {
        return domainUsersOnly;
    }

    public void setDomainUsersOnly(Boolean value) {
        this.domainUsersOnly = value;
    }

    public Boolean getDriveMembersOnly() {
        return driveMembersOnly;
    }

    public void setDriveMembersOnly(Boolean value) {
        this.driveMembersOnly = value;
    }

    public Boolean getAdminManagedRestrictions() {
        return adminManagedRestrictions;
    }

    public void setAdminManagedRestrictions(Boolean value) {
        this.adminManagedRestrictions = value;
    }

    public Boolean getSharingFoldersRequiresOrganizerPermission() {
        return sharingFoldersRequiresOrganizerPermission;
    }

    public void setSharingFoldersRequiresOrganizerPermission(Boolean value) {
        this.sharingFoldersRequiresOrganizerPermission = value;
    }

    public Boolean getDownloadRestrictedForReaders() {
        return downloadRestrictedForReaders;
    }

    public void setDownloadRestrictedForReaders(Boolean value) {
        this.downloadRestrictedForReaders = value;
    }

    public Boolean getDownloadRestrictedForWriters() {
        return downloadRestrictedForWriters;
    }

    public void setDownloadRestrictedForWriters(Boolean value) {
        this.downloadRestrictedForWriters = value;
    }
}