package com.multidrive.api.dto;

import org.springframework.web.multipart.MultipartFile;

public record DriveUploadRequest(

		Long sourceId,

		Long parentItemId,

		String name,

		MultipartFile file) {
}
