package com.multidrive.api.service;

import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.model.DriveOperationStrategyType;
import com.multidrive.api.service.impl.DriveOperationPlannerImpl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriveOperationPlannerImplTest {

	private final DriveOperationPlannerImpl planner = new DriveOperationPlannerImpl();

	@Test
	void plansNativeMoveWithinSameSource() {

		assertThat(planner.planMove(item(10L, GoogleDriveItemCategory.OTHER), source(10L)).strategyType())
			.isEqualTo(DriveOperationStrategyType.NATIVE_MOVE);
	}

	@Test
	void plansCrossSourceMoveForDifferentSources() {

		assertThat(planner.planMove(item(10L, GoogleDriveItemCategory.OTHER), source(20L)).strategyType())
			.isEqualTo(DriveOperationStrategyType.CROSS_SOURCE_TRANSFER);
	}

	@Test
	void plansFileCopyFolderCopyAndCrossSourceCopy() {

		assertThat(planner.planCopy(item(10L, GoogleDriveItemCategory.OTHER), source(10L)).strategyType())
			.isEqualTo(DriveOperationStrategyType.NATIVE_COPY);
		assertThat(planner.planCopy(item(10L, GoogleDriveItemCategory.FOLDER), source(10L)).strategyType())
			.isEqualTo(DriveOperationStrategyType.RECURSIVE_FOLDER_COPY);
		assertThat(planner.planCopy(item(10L, GoogleDriveItemCategory.OTHER), source(20L)).strategyType())
			.isEqualTo(DriveOperationStrategyType.CROSS_SOURCE_TRANSFER);
	}

	@Test
	void rejectsMissingInputsAndSourceIdentity() {

		assertThatThrownBy(() -> planner.planMove(null, source(10L))).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("sourceItem is required");

		assertThatThrownBy(() -> planner.planMove(new GoogleDriveItem(), source(10L)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Source Drive information is missing");

		assertThatThrownBy(() -> planner.planMove(item(10L, GoogleDriveItemCategory.OTHER), source(null)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("destinationSource is required");
	}

	private GoogleDriveItem item(Long sourceId, GoogleDriveItemCategory category) {

		GoogleDriveItem item = new GoogleDriveItem();

		item.setSource(source(sourceId));
		item.setCategory(category);

		return item;
	}

	private GoogleDriveSource source(Long sourceId) {

		GoogleDriveSource source = new GoogleDriveSource();

		source.setId(sourceId);

		return source;
	}

}
