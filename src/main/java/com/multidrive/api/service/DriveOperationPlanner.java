package com.multidrive.api.service;

import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.model.DriveOperationPlan;

public interface DriveOperationPlanner {

	DriveOperationPlan planMove(GoogleDriveItem sourceItem, GoogleDriveSource destinationSource);

	DriveOperationPlan planCopy(GoogleDriveItem sourceItem, GoogleDriveSource destinationSource);

}
