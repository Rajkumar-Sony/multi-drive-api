package com.multidrive.api.dto;

import java.time.LocalDateTime;

public record GoogleDriveAccountResponse(Long connectionId, String email, String status,
		LocalDateTime accessTokenExpiry) {
}