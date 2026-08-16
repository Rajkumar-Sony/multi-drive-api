package com.multidrive.api.service;

import java.io.IOException;
import java.io.OutputStream;

public interface GoogleDriveContentStreamingService {

    void streamBlob(
            Long connectionId,
            Long userId,
            String googleFileId,
            OutputStream outputStream
    ) throws IOException;

    void streamExport(
            Long connectionId,
            Long userId,
            String googleFileId,
            String exportMimeType,
            OutputStream outputStream
    ) throws IOException;
}
