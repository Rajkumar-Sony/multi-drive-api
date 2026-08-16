package com.multidrive.api.dto;

import java.util.List;

public record GoogleDriveFileResponse(
        String id,
        String name,
        String mimeType,
        String modifiedTime,
        List<String> parents,
        String webViewLink,
        String driveId,
        Boolean trashed
) {
}