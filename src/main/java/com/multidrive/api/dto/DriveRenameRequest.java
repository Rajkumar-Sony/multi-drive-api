package com.multidrive.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DriveRenameRequest(

		@NotBlank(message = "name must not be blank") @Size(max = 255,
				message = "name must not exceed 255 characters") String name) {
}
