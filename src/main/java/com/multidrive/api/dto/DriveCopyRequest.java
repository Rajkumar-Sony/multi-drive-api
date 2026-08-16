package com.multidrive.api.dto;

public record DriveCopyRequest(

        Long destinationSourceId,

        Long destinationParentItemId,

        String name
) {
}
