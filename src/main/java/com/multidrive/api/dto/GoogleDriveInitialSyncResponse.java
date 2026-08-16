package com.multidrive.api.dto;

public record GoogleDriveInitialSyncResponse(

        Long connectionId,

        long myDriveItemCount,

        long sharedDriveCount,

        long sharedDriveItemCount,

        long totalItemCount,

        int staleItemCount
) {
}