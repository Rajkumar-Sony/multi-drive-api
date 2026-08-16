package com.multidrive.api.service.impl;

import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.dto.DriveUploadRequest;
import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.entity.GoogleDriveSourceStatus;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.exception.DriveItemNotFoundException;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.repository.GoogleDriveSourceRepository;
import com.multidrive.api.service.DriveItemLookupService;
import com.multidrive.api.service.DriveOperationCapabilityGuard;
import com.multidrive.api.service.DriveOperationLocalStateService;
import com.multidrive.api.service.DriveUploadService;
import com.multidrive.api.service.GoogleDriveFileMutationService;
import com.multidrive.api.service.GoogleDriveResumableUploadService;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DriveUploadServiceImpl implements DriveUploadService {

	private final GoogleDriveItemRepository googleDriveItemRepository;

	private final GoogleDriveSourceRepository googleDriveSourceRepository;

	private final GoogleDriveFileMutationService googleDriveFileMutationService;

	private final GoogleDriveResumableUploadService googleDriveResumableUploadService;

	private final DriveOperationCapabilityGuard driveOperationCapabilityGuard;

	private final DriveOperationLocalStateService driveOperationLocalStateService;

	private final DriveItemLookupService driveItemLookupService;

	public DriveUploadServiceImpl(GoogleDriveItemRepository googleDriveItemRepository,
			GoogleDriveSourceRepository googleDriveSourceRepository,
			GoogleDriveFileMutationService googleDriveFileMutationService,
			GoogleDriveResumableUploadService googleDriveResumableUploadService,
			DriveOperationCapabilityGuard driveOperationCapabilityGuard,
			DriveOperationLocalStateService driveOperationLocalStateService,
			DriveItemLookupService driveItemLookupService) {

		this.googleDriveItemRepository = googleDriveItemRepository;

		this.googleDriveSourceRepository = googleDriveSourceRepository;

		this.googleDriveFileMutationService = googleDriveFileMutationService;

		this.googleDriveResumableUploadService = googleDriveResumableUploadService;

		this.driveOperationCapabilityGuard = driveOperationCapabilityGuard;

		this.driveOperationLocalStateService = driveOperationLocalStateService;

		this.driveItemLookupService = driveItemLookupService;
	}

	@Override
	public DriveItemDetailsResponse upload(String googleSubjectId, DriveUploadRequest request) {

		validateRequest(googleSubjectId, request);

		MultipartFile multipartFile = request.file();

		String fileName = resolveFileName(request.name(), multipartFile);

		String contentType = resolveContentType(multipartFile);

		UploadDestination destination = resolveDestination(googleSubjectId, request.sourceId(), request.parentItemId());

		GoogleDriveFileResponse remoteFile = googleDriveResumableUploadService.upload(destination.connectionId(),
				destination.userId(), destination.parentGoogleFileId(), fileName, contentType, multipartFile.getSize(),
				multipartFile::getInputStream);

		Long localItemId = driveOperationLocalStateService.createUploadedItem(destination.connectionId(),
				destination.sourceId(), destination.sourceType(), destination.driveId(), remoteFile);

		return driveItemLookupService.getItem(googleSubjectId, localItemId);
	}

	private UploadDestination resolveDestination(String googleSubjectId, Long sourceId, Long parentItemId) {

		if (parentItemId == null) {

			return resolveSourceRootDestination(googleSubjectId, sourceId);
		}

		return resolveFolderDestination(googleSubjectId, sourceId, parentItemId);
	}

	private UploadDestination resolveSourceRootDestination(String googleSubjectId, Long sourceId) {

		GoogleDriveSource source = googleDriveSourceRepository
			.findOwnedSourceForOperation(sourceId, googleSubjectId, GoogleDriveSourceStatus.ACTIVE)
			.orElseThrow(() -> new IllegalArgumentException("Active Google Drive source not found"));

		if (source.getRootFolderId() == null || source.getRootFolderId().isBlank()) {

			throw new IllegalStateException("Drive source root folder is missing");
		}

		GoogleDriveConnection connection = requireConnection(source);

		Long userId = requireUserId(connection);

		GoogleDriveFileResponse remoteParent = googleDriveFileMutationService.getFile(connection.getId(), userId,
				source.getRootFolderId());

		driveOperationCapabilityGuard.requireAllowed(
				remoteParent.capabilities() != null ? remoteParent.capabilities().canAddChildren() : null, "UPLOAD",
				"Cannot upload files to this Drive root");

		GoogleDriveItemSourceType sourceType = resolveSourceType(source);

		String driveId = sourceType == GoogleDriveItemSourceType.SHARED_DRIVE ? source.getGoogleDriveId() : null;

		return new UploadDestination(connection.getId(), userId, source.getId(), sourceType, driveId,
				source.getRootFolderId());
	}

	private UploadDestination resolveFolderDestination(String googleSubjectId, Long sourceId, Long parentItemId) {

		GoogleDriveItem parent = googleDriveItemRepository.findOwnedItemForDetails(parentItemId, googleSubjectId)
			.orElseThrow(() -> new DriveItemNotFoundException(parentItemId));

		if (parent.getCategory() != GoogleDriveItemCategory.FOLDER) {

			throw new IllegalArgumentException("parentItemId must reference a folder");
		}

		if (parent.isTrashed()) {

			throw new IllegalArgumentException("Cannot upload into a trashed folder");
		}

		GoogleDriveSource source = parent.getSource();

		if (source == null || source.getId() == null) {

			throw new IllegalStateException("Parent Drive source is missing");
		}

		if (!source.getId().equals(sourceId)) {

			throw new IllegalArgumentException("parentItemId does not belong to sourceId");
		}

		GoogleDriveConnection connection = requireConnection(parent);

		Long userId = requireUserId(connection);

		GoogleDriveFileResponse remoteParent = googleDriveFileMutationService.getFile(connection.getId(), userId,
				parent.getGoogleFileId());

		driveOperationCapabilityGuard.requireAllowed(
				remoteParent.capabilities() != null ? remoteParent.capabilities().canAddChildren() : null, "UPLOAD",
				"Cannot upload files to the selected folder");

		GoogleDriveItemSourceType sourceType = parent.getSourceType();

		String driveId = sourceType == GoogleDriveItemSourceType.SHARED_DRIVE ? source.getGoogleDriveId() : null;

		return new UploadDestination(connection.getId(), userId, source.getId(), sourceType, driveId,
				parent.getGoogleFileId());
	}

	private String resolveFileName(String requestedName, MultipartFile multipartFile) {

		if (requestedName != null && !requestedName.isBlank()) {

			return requestedName.trim();
		}

		String originalFilename = multipartFile.getOriginalFilename();

		String filename = StringUtils.getFilename(originalFilename);

		if (filename == null || filename.isBlank()) {

			return "upload.bin";
		}

		return filename;
	}

	private String resolveContentType(MultipartFile multipartFile) {

		String contentType = multipartFile.getContentType();

		if (contentType == null || contentType.isBlank()) {

			return MediaType.APPLICATION_OCTET_STREAM_VALUE;
		}

		try {

			MediaType.parseMediaType(contentType);

			return contentType;

		}
		catch (Exception exception) {

			return MediaType.APPLICATION_OCTET_STREAM_VALUE;
		}
	}

	private GoogleDriveItemSourceType resolveSourceType(GoogleDriveSource source) {

		if (source.getSourceType() == GoogleDriveSourceType.SHARED_DRIVE) {

			return GoogleDriveItemSourceType.SHARED_DRIVE;
		}

		return GoogleDriveItemSourceType.MY_DRIVE;
	}

	private GoogleDriveConnection requireConnection(GoogleDriveSource source) {

		if (source.getConnection() == null || source.getConnection().getId() == null) {

			throw new IllegalStateException("Drive source connection is missing");
		}

		return source.getConnection();
	}

	private GoogleDriveConnection requireConnection(GoogleDriveItem item) {

		if (item.getConnection() == null || item.getConnection().getId() == null) {

			throw new IllegalStateException("Drive item connection is missing");
		}

		return item.getConnection();
	}

	private Long requireUserId(GoogleDriveConnection connection) {

		if (connection.getUser() == null || connection.getUser().getId() == null) {

			throw new IllegalStateException("Application user is missing");
		}

		return connection.getUser().getId();
	}

	private void validateRequest(String googleSubjectId, DriveUploadRequest request) {

		if (googleSubjectId == null || googleSubjectId.isBlank()) {

			throw new IllegalArgumentException("googleSubjectId is required");
		}

		if (request == null) {

			throw new IllegalArgumentException("request is required");
		}

		if (request.sourceId() == null) {

			throw new IllegalArgumentException("sourceId is required");
		}

		if (request.file() == null) {

			throw new IllegalArgumentException("file is required");
		}
	}

	private record UploadDestination(

			Long connectionId,

			Long userId,

			Long sourceId,

			GoogleDriveItemSourceType sourceType,

			String driveId,

			String parentGoogleFileId) {
	}

}
