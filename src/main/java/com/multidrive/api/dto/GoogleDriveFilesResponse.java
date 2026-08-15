package com.multidrive.api.dto;

import java.util.List;

public record GoogleDriveFilesResponse(
        List<GoogleDriveFileResponse> files,
        String nextPageToken,
        Boolean incompleteSearch
) {
}