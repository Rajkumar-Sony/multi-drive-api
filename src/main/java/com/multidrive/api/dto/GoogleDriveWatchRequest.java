package com.multidrive.api.dto;

public record GoogleDriveWatchRequest(
        String id,
        String type,
        String address,
        String token,
        String expiration
) {
}