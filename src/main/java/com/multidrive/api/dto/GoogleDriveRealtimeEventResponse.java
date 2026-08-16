package com.multidrive.api.dto;

import com.multidrive.api.entity.GoogleDriveTrackerType;

import java.time.Instant;

public record GoogleDriveRealtimeEventResponse(

        String eventType,

        Long connectionId,

        Long trackerId,

        GoogleDriveTrackerType trackerType,

        String driveId,

        int changeCount,

        Instant occurredAt
) {
}