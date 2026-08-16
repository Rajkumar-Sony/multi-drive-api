package com.multidrive.api.service;

import com.multidrive.api.dto.DriveCopyRequest;
import com.multidrive.api.dto.DriveCreateFolderRequest;
import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.dto.DriveMoveRequest;
import com.multidrive.api.dto.DriveRenameRequest;

public interface DriveOperationService {

	DriveItemDetailsResponse createFolder(String googleSubjectId, DriveCreateFolderRequest request);

	DriveItemDetailsResponse rename(String googleSubjectId, Long itemId, DriveRenameRequest request);

	DriveItemDetailsResponse move(String googleSubjectId, Long itemId, DriveMoveRequest request);

	DriveItemDetailsResponse copy(String googleSubjectId, Long itemId, DriveCopyRequest request);

	DriveItemDetailsResponse trash(String googleSubjectId, Long itemId);

	DriveItemDetailsResponse restore(String googleSubjectId, Long itemId);

	void permanentlyDelete(String googleSubjectId, Long itemId);

}
