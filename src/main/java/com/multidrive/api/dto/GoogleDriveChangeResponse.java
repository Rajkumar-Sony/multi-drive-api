package com.multidrive.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleDriveChangeResponse(
        Boolean removed,
        String fileId,
        String time,
        String driveId,
        String changeType,
        GoogleDriveFileResponse file,
        GoogleSharedDriveResponse drive
) {
}