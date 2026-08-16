package com.multidrive.api.dto;

public record DriveCreateFolderRequest(

        Long sourceId,

        Long parentItemId,

        String name
) {
}