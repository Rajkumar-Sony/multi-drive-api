package com.multidrive.api.dto;

public record DriveExportFormatResponse(

        String label,

        String mimeType,

        String extension,

        boolean defaultFormat
) {
}
