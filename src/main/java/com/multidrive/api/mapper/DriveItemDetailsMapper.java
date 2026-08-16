package com.multidrive.api.mapper;

import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveSource;

import org.springframework.stereotype.Component;

@Component
public class DriveItemDetailsMapper {

	private final DriveItemCapabilityResponseMapper driveItemCapabilityResponseMapper;

	public DriveItemDetailsMapper(DriveItemCapabilityResponseMapper driveItemCapabilityResponseMapper) {

		this.driveItemCapabilityResponseMapper = driveItemCapabilityResponseMapper;
	}

	public DriveItemDetailsResponse toResponse(GoogleDriveItem item) {

		if (item == null) {

			throw new IllegalArgumentException("Drive item is required");
		}

		GoogleDriveConnection connection = item.getConnection();

		GoogleDriveSource source = item.getSource();

		if (connection == null || source == null) {

			throw new IllegalStateException("Drive item source information is incomplete");
		}

		return new DriveItemDetailsResponse(

				item.getId(),

				connection.getId(),

				connection.getGoogleEmail(),

				source.getId(),

				source.getName(),

				item.getSourceType(),

				source.getGoogleDriveId(),

				source.getRootFolderId(),

				item.getGoogleFileId(),

				item.getParentId(),

				item.getName(),

				item.getMimeType(),

				item.getCategory(),

				item.getCategory() == GoogleDriveItemCategory.FOLDER,

				item.isTrashed(),

				item.getWebViewLink(),

				item.getThumbnailLink(),

				item.getIconLink(),

				item.getSizeBytes(),

				item.getGoogleCreatedTime(),

				item.getGoogleModifiedTime(),

				driveItemCapabilityResponseMapper.toResponse(item.getCapabilities()));
	}

}