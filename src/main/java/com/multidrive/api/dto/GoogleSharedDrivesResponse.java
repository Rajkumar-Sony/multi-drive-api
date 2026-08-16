package com.multidrive.api.dto;

import java.util.List;

public record GoogleSharedDrivesResponse(List<GoogleSharedDriveResponse> drives, String nextPageToken) {
}