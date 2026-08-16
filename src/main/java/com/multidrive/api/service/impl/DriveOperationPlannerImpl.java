package com.multidrive.api.service.impl;

import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.model.DriveOperationPlan;
import com.multidrive.api.model.DriveOperationStrategyType;
import com.multidrive.api.service.DriveOperationPlanner;

import org.springframework.stereotype.Service;

@Service
public class DriveOperationPlannerImpl implements DriveOperationPlanner {

	@Override
	public DriveOperationPlan planMove(GoogleDriveItem sourceItem, GoogleDriveSource destinationSource) {

		validate(sourceItem, destinationSource);

		GoogleDriveSource source = sourceItem.getSource();

		if (source.getId().equals(destinationSource.getId())) {

			return new DriveOperationPlan(DriveOperationStrategyType.NATIVE_MOVE,
					"Source and destination belong to the same Drive source");
		}

		return new DriveOperationPlan(DriveOperationStrategyType.CROSS_SOURCE_TRANSFER,
				"Moving between different Drive sources requires the cross-source transfer engine");
	}

	@Override
	public DriveOperationPlan planCopy(GoogleDriveItem sourceItem, GoogleDriveSource destinationSource) {

		validate(sourceItem, destinationSource);

		GoogleDriveSource source = sourceItem.getSource();

		if (!source.getId().equals(destinationSource.getId())) {

			return new DriveOperationPlan(DriveOperationStrategyType.CROSS_SOURCE_TRANSFER,
					"Copying between different Drive sources requires the cross-source transfer engine");
		}

		if (sourceItem.getCategory() == GoogleDriveItemCategory.FOLDER) {

			return new DriveOperationPlan(DriveOperationStrategyType.RECURSIVE_FOLDER_COPY,
					"Folder copy requires recursive folder-tree processing");
		}

		return new DriveOperationPlan(DriveOperationStrategyType.NATIVE_COPY, "File can use Google Drive native copy");
	}

	private void validate(GoogleDriveItem sourceItem, GoogleDriveSource destinationSource) {

		if (sourceItem == null) {

			throw new IllegalArgumentException("sourceItem is required");
		}

		if (sourceItem.getSource() == null || sourceItem.getSource().getId() == null) {

			throw new IllegalStateException("Source Drive information is missing");
		}

		if (destinationSource == null || destinationSource.getId() == null) {

			throw new IllegalArgumentException("destinationSource is required");
		}
	}

}
