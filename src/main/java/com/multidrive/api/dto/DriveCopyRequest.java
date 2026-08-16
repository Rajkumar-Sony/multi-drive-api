package com.multidrive.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DriveCopyRequest(

		@NotNull(message = "destinationSourceId is required") Long destinationSourceId,

		Long destinationParentItemId,

		@Size(max = 255, message = "name must not exceed 255 characters") String name) {
}
