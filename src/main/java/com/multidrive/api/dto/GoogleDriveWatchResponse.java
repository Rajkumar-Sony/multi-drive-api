package com.multidrive.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleDriveWatchResponse(String kind, String id, String resourceId, String resourceUri, String token,
		String expiration) {
}