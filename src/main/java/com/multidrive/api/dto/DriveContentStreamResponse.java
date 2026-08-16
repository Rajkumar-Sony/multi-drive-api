package com.multidrive.api.dto;

import com.multidrive.api.service.DriveContentWriter;

public record DriveContentStreamResponse(

        String fileName,

        String contentType,

        Long contentLength,

        DriveContentWriter writer
) {
}
