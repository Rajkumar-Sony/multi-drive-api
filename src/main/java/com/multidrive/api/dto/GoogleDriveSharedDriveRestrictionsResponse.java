package com.multidrive.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleDriveSharedDriveRestrictionsResponse(

        Boolean copyRequiresWriterPermission,

        Boolean domainUsersOnly,

        Boolean driveMembersOnly,

        Boolean adminManagedRestrictions,

        Boolean sharingFoldersRequiresOrganizerPermission,

        GoogleDriveDownloadRestrictionResponse downloadRestriction
) {
}