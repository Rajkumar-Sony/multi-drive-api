package com.multidrive.api.dto;

public record DriveMoveRequest(

        Long destinationSourceId,

        Long destinationParentItemId
) {
}
