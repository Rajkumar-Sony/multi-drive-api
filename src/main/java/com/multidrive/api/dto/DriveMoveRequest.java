package com.multidrive.api.dto;

import jakarta.validation.constraints.NotNull;

public record DriveMoveRequest(

		@NotNull(message = "destinationSourceId is required") Long destinationSourceId,

		Long destinationParentItemId) {
}
