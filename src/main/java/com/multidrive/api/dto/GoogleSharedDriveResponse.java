package com.multidrive.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleSharedDriveResponse(

        String id,

        String name,

        Boolean hidden,

        String createdTime,

        GoogleDriveSharedDriveCapabilitiesResponse capabilities,

        GoogleDriveSharedDriveRestrictionsResponse restrictions
) {
}