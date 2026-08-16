package com.multidrive.api.service.impl;

import com.multidrive.api.dto.DriveContentStreamResponse;
import com.multidrive.api.dto.DriveExportFormatResponse;
import com.multidrive.api.dto.DriveExportOptionsResponse;
import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.exception.DriveContentNotSupportedException;
import com.multidrive.api.exception.DriveItemNotFoundException;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.service.DriveDownloadService;
import com.multidrive.api.service.DriveExportFormatRegistry;
import com.multidrive.api.service.DriveOperationCapabilityGuard;
import com.multidrive.api.service.GoogleDriveContentStreamingService;
import com.multidrive.api.service.GoogleDriveFileMutationService;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class DriveDownloadServiceImpl
        implements DriveDownloadService {

    private final GoogleDriveItemRepository
            googleDriveItemRepository;

    private final GoogleDriveFileMutationService
            googleDriveFileMutationService;

    private final GoogleDriveContentStreamingService
            googleDriveContentStreamingService;

    private final DriveOperationCapabilityGuard
            driveOperationCapabilityGuard;

    private final DriveExportFormatRegistry
            driveExportFormatRegistry;

    public DriveDownloadServiceImpl(
            GoogleDriveItemRepository googleDriveItemRepository,
            GoogleDriveFileMutationService googleDriveFileMutationService,
            GoogleDriveContentStreamingService googleDriveContentStreamingService,
            DriveOperationCapabilityGuard driveOperationCapabilityGuard,
            DriveExportFormatRegistry driveExportFormatRegistry
    ) {

        this.googleDriveItemRepository =
                googleDriveItemRepository;

        this.googleDriveFileMutationService =
                googleDriveFileMutationService;

        this.googleDriveContentStreamingService =
                googleDriveContentStreamingService;

        this.driveOperationCapabilityGuard =
                driveOperationCapabilityGuard;

        this.driveExportFormatRegistry =
                driveExportFormatRegistry;
    }

    @Override
    public DriveContentStreamResponse prepareDownload(
            String googleSubjectId,
            Long itemId
    ) {

        GoogleDriveItem item =
                requireOwnedItem(
                        googleSubjectId,
                        itemId
                );

        if (item.getCategory()
                == GoogleDriveItemCategory.FOLDER) {

            throw new DriveContentNotSupportedException(
                    "Folders cannot be downloaded directly. Folder download as ZIP will be implemented through the operation job engine."
            );
        }

        GoogleDriveConnection connection =
                requireConnection(
                        item
                );

        Long userId =
                requireUserId(
                        connection
                );

        GoogleDriveFileResponse remoteFile =
                googleDriveFileMutationService
                        .getFile(
                                connection.getId(),
                                userId,
                                item.getGoogleFileId()
                        );

        driveOperationCapabilityGuard
                .requireAllowed(
                        remoteFile.capabilities()
                                != null
                                ? remoteFile
                                        .capabilities()
                                        .canDownload()
                                : null,
                        "DOWNLOAD"
                );

        String remoteMimeType =
                normalizeContentType(
                        remoteFile.mimeType()
                );

        if (driveExportFormatRegistry
                .isGoogleNativeMimeType(
                        remoteMimeType
                )) {

            throw new DriveContentNotSupportedException(
                    "Google Workspace files must be exported using the export endpoint"
            );
        }

        String fileName =
                normalizeFileName(
                        remoteFile.name(),
                        item.getName()
                );

        Long contentLength =
                parseSize(
                        remoteFile.size()
                );

        Long connectionId =
                connection.getId();

        String googleFileId =
                item.getGoogleFileId();

        return new DriveContentStreamResponse(

                fileName,

                remoteMimeType,

                contentLength,

                outputStream ->
                        googleDriveContentStreamingService
                                .streamBlob(
                                        connectionId,
                                        userId,
                                        googleFileId,
                                        outputStream
                                )
        );
    }

    @Override
    public DriveContentStreamResponse prepareExport(
            String googleSubjectId,
            Long itemId,
            String exportMimeType
    ) {

        GoogleDriveItem item =
                requireOwnedItem(
                        googleSubjectId,
                        itemId
                );

        GoogleDriveConnection connection =
                requireConnection(
                        item
                );

        Long userId =
                requireUserId(
                        connection
                );

        GoogleDriveFileResponse remoteFile =
                googleDriveFileMutationService
                        .getFile(
                                connection.getId(),
                                userId,
                                item.getGoogleFileId()
                        );

        driveOperationCapabilityGuard
                .requireAllowed(
                        remoteFile.capabilities()
                                != null
                                ? remoteFile
                                        .capabilities()
                                        .canDownload()
                                : null,
                        "EXPORT"
                );

        String sourceMimeType =
                remoteFile.mimeType();

        DriveExportFormatResponse format =
                driveExportFormatRegistry
                        .resolve(
                                sourceMimeType,
                                exportMimeType
                        );

        String sourceName =
                normalizeFileName(
                        remoteFile.name(),
                        item.getName()
                );

        String exportedFileName =
                applyExtension(
                        sourceName,
                        format.extension()
                );

        Long connectionId =
                connection.getId();

        String googleFileId =
                item.getGoogleFileId();

        return new DriveContentStreamResponse(

                exportedFileName,

                format.mimeType(),

                null,

                outputStream ->
                        googleDriveContentStreamingService
                                .streamExport(
                                        connectionId,
                                        userId,
                                        googleFileId,
                                        format.mimeType(),
                                        outputStream
                                )
        );
    }

    @Override
    public DriveExportOptionsResponse getExportOptions(
            String googleSubjectId,
            Long itemId
    ) {

        GoogleDriveItem item =
                requireOwnedItem(
                        googleSubjectId,
                        itemId
                );

        String mimeType =
                item.getMimeType();

        return new DriveExportOptionsResponse(

                item.getId(),

                mimeType,

                driveExportFormatRegistry
                        .getFormats(
                                mimeType
                        )
        );
    }

    private GoogleDriveItem requireOwnedItem(
            String googleSubjectId,
            Long itemId
    ) {

        if (googleSubjectId == null
                || googleSubjectId.isBlank()) {

            throw new IllegalArgumentException(
                    "googleSubjectId is required"
            );
        }

        if (itemId == null) {

            throw new IllegalArgumentException(
                    "itemId is required"
            );
        }

        return googleDriveItemRepository
                .findOwnedItemForDetails(
                        itemId,
                        googleSubjectId
                )
                .orElseThrow(
                        () -> new DriveItemNotFoundException(
                                itemId
                        )
                );
    }

    private GoogleDriveConnection requireConnection(
            GoogleDriveItem item
    ) {

        if (item.getConnection() == null
                || item.getConnection().getId() == null) {

            throw new IllegalStateException(
                    "Drive item connection is missing"
            );
        }

        return item.getConnection();
    }

    private Long requireUserId(
            GoogleDriveConnection connection
    ) {

        if (connection.getUser() == null
                || connection.getUser().getId() == null) {

            throw new IllegalStateException(
                    "Application user is missing"
            );
        }

        return connection
                .getUser()
                .getId();
    }

    private String normalizeFileName(
            String remoteName,
            String localName
    ) {

        String name;

        if (remoteName != null
                && !remoteName.isBlank()) {

            name =
                    remoteName.trim();

        } else if (localName != null
                && !localName.isBlank()) {

            name =
                    localName.trim();

        } else {

            name =
                    "download";
        }

        return name
                .replace(
                        "/",
                        "_"
                )
                .replace(
                        "\\",
                        "_"
                );
    }

    private String applyExtension(
            String fileName,
            String extension
    ) {

        if (extension == null
                || extension.isBlank()) {

            return fileName;
        }

        if (fileName
                .toLowerCase()
                .endsWith(
                        extension.toLowerCase()
                )) {

            return fileName;
        }

        return fileName
                + extension;
    }

    private String normalizeContentType(
            String mimeType
    ) {

        if (mimeType == null
                || mimeType.isBlank()) {

            return MediaType
                    .APPLICATION_OCTET_STREAM_VALUE;
        }

        try {

            MediaType.parseMediaType(
                    mimeType
            );

            return mimeType;

        } catch (Exception exception) {

            return MediaType
                    .APPLICATION_OCTET_STREAM_VALUE;
        }
    }

    private Long parseSize(
            String size
    ) {

        if (size == null
                || size.isBlank()) {

            return null;
        }

        try {

            return Long.parseLong(
                    size
            );

        } catch (NumberFormatException exception) {

            return null;
        }
    }
}
