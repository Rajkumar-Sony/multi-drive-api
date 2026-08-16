package com.multidrive.api.dto;

import java.util.List;

public record DriveExportOptionsResponse(

        Long itemId,

        String sourceMimeType,

        List<DriveExportFormatResponse> formats
) {
}
