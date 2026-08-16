package com.multidrive.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleDriveChangesResponse(List<GoogleDriveChangeResponse> changes, String nextPageToken,
		String newStartPageToken) {
}