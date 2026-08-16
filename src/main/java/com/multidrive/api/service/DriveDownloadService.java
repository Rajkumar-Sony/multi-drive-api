package com.multidrive.api.service;

import com.multidrive.api.dto.DriveContentStreamResponse;
import com.multidrive.api.dto.DriveExportOptionsResponse;

public interface DriveDownloadService {

	DriveContentStreamResponse prepareDownload(String googleSubjectId, Long itemId);

	DriveContentStreamResponse prepareExport(String googleSubjectId, Long itemId, String exportMimeType);

	DriveExportOptionsResponse getExportOptions(String googleSubjectId, Long itemId);

}
