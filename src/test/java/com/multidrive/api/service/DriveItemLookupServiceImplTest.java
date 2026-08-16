package com.multidrive.api.service;

import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.exception.DriveItemNotFoundException;
import com.multidrive.api.mapper.DriveItemDetailsMapper;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.service.impl.DriveItemLookupServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriveItemLookupServiceImplTest {

	private static final String GOOGLE_SUBJECT_ID = "google-subject-123";

	@Mock
	private GoogleDriveItemRepository googleDriveItemRepository;

	@Mock
	private DriveItemDetailsMapper driveItemDetailsMapper;

	private DriveItemLookupServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new DriveItemLookupServiceImpl(googleDriveItemRepository, driveItemDetailsMapper);
	}

	@Test
	void getItemLoadsOwnedItemAndMapsResponse() {

		GoogleDriveItem item = new GoogleDriveItem();
		DriveItemDetailsResponse response = itemResponse();

		when(googleDriveItemRepository.findOwnedItemForDetails(10L, GOOGLE_SUBJECT_ID)).thenReturn(Optional.of(item));
		when(driveItemDetailsMapper.toResponse(item)).thenReturn(response);

		assertThat(service.getItem(GOOGLE_SUBJECT_ID, 10L)).isSameAs(response);

		verify(googleDriveItemRepository).findOwnedItemForDetails(10L, GOOGLE_SUBJECT_ID);
		verify(driveItemDetailsMapper).toResponse(item);
	}

	@Test
	void getItemRejectsInvalidInputBeforeRepositoryLookup() {

		assertThatThrownBy(() -> service.getItem(" ", 10L)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("googleSubjectId is required");

		assertThatThrownBy(() -> service.getItem(GOOGLE_SUBJECT_ID, null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("itemId is required");

		verifyNoInteractions(googleDriveItemRepository, driveItemDetailsMapper);
	}

	@Test
	void getItemThrowsNotFoundForMissingOrUnauthorizedItem() {

		when(googleDriveItemRepository.findOwnedItemForDetails(10L, GOOGLE_SUBJECT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getItem(GOOGLE_SUBJECT_ID, 10L)).isInstanceOf(DriveItemNotFoundException.class)
			.hasMessage("Drive item not found: 10");

		verifyNoInteractions(driveItemDetailsMapper);
	}

	private DriveItemDetailsResponse itemResponse() {

		return new DriveItemDetailsResponse(10L, 1L, "user@example.com", 30L, "My Drive",
				GoogleDriveItemSourceType.MY_DRIVE, null, "root", "google-file-id", "parent-google-id", "Report.pdf",
				"application/pdf", GoogleDriveItemCategory.OTHER, false, false, null, null, null, 1024L, null, null,
				null);
	}

}
