package com.multidrive.api.service;

import com.multidrive.api.dto.DriveContentStreamResponse;
import com.multidrive.api.dto.DriveExportOptionsResponse;
import com.multidrive.api.dto.GoogleDriveFileCapabilitiesResponse;
import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.exception.DriveContentNotSupportedException;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.service.impl.DriveDownloadServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriveDownloadServiceImplTest {

	private static final String GOOGLE_SUBJECT_ID = "google-subject-123";

	private static final Long USER_ID = 42L;

	@Mock
	private GoogleDriveItemRepository googleDriveItemRepository;

	@Mock
	private GoogleDriveFileMutationService googleDriveFileMutationService;

	@Mock
	private GoogleDriveContentStreamingService googleDriveContentStreamingService;

	private DriveDownloadServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new DriveDownloadServiceImpl(googleDriveItemRepository, googleDriveFileMutationService,
				googleDriveContentStreamingService, new DriveOperationCapabilityGuard(),
				new DriveExportFormatRegistry());
	}

	@Test
	void prepareDownloadBuildsBlobStreamResponseForNonNativeFile() throws Exception {

		GoogleDriveItem item = item(123L, GoogleDriveItemCategory.OTHER, "local.pdf");

		when(googleDriveItemRepository.findOwnedItemForDetails(123L, GOOGLE_SUBJECT_ID)).thenReturn(Optional.of(item));
		when(googleDriveFileMutationService.getFile(20L, USER_ID, "file-id"))
			.thenReturn(remoteFile("remote/name.pdf", "application/pdf", "2048", false, downloadableCapabilities()));

		DriveContentStreamResponse response = service.prepareDownload(GOOGLE_SUBJECT_ID, 123L);

		assertThat(response.fileName()).isEqualTo("remote_name.pdf");
		assertThat(response.contentType()).isEqualTo("application/pdf");
		assertThat(response.contentLength()).isEqualTo(2048L);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		response.writer().writeTo(outputStream);

		verify(googleDriveContentStreamingService).streamBlob(20L, USER_ID, "file-id", outputStream);
	}

	@Test
	void prepareDownloadRejectsFoldersAndGoogleNativeFiles() {

		GoogleDriveItem folder = item(123L, GoogleDriveItemCategory.FOLDER, "Folder");
		GoogleDriveItem googleDoc = item(124L, GoogleDriveItemCategory.DOCUMENT, "Doc");

		when(googleDriveItemRepository.findOwnedItemForDetails(123L, GOOGLE_SUBJECT_ID))
			.thenReturn(Optional.of(folder));
		when(googleDriveItemRepository.findOwnedItemForDetails(124L, GOOGLE_SUBJECT_ID))
			.thenReturn(Optional.of(googleDoc));
		when(googleDriveFileMutationService.getFile(20L, USER_ID, "file-id")).thenReturn(
				remoteFile("Doc", DriveExportFormatRegistry.GOOGLE_DOCUMENT, null, false, downloadableCapabilities()));

		assertThatThrownBy(() -> service.prepareDownload(GOOGLE_SUBJECT_ID, 123L))
			.isInstanceOf(DriveContentNotSupportedException.class)
			.hasMessageContaining("Folders cannot be downloaded directly");

		assertThatThrownBy(() -> service.prepareDownload(GOOGLE_SUBJECT_ID, 124L))
			.isInstanceOf(DriveContentNotSupportedException.class)
			.hasMessage("Google Workspace files must be exported using the export endpoint");

		verifyNoInteractions(googleDriveContentStreamingService);
	}

	@Test
	void prepareExportUsesRequestedFormatAndStreamsExport() throws Exception {

		GoogleDriveItem item = item(123L, GoogleDriveItemCategory.DOCUMENT, "Local Doc");

		when(googleDriveItemRepository.findOwnedItemForDetails(123L, GOOGLE_SUBJECT_ID)).thenReturn(Optional.of(item));
		when(googleDriveFileMutationService.getFile(20L, USER_ID, "file-id")).thenReturn(remoteFile("Remote Doc",
				DriveExportFormatRegistry.GOOGLE_DOCUMENT, null, false, downloadableCapabilities()));

		DriveContentStreamResponse response = service.prepareExport(GOOGLE_SUBJECT_ID, 123L, "application/pdf");

		assertThat(response.fileName()).isEqualTo("Remote Doc.pdf");
		assertThat(response.contentType()).isEqualTo("application/pdf");
		assertThat(response.contentLength()).isNull();

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		response.writer().writeTo(outputStream);

		verify(googleDriveContentStreamingService).streamExport(20L, USER_ID, "file-id", "application/pdf",
				outputStream);
	}

	@Test
	void getExportOptionsReturnsConfiguredFormatsForLocalMimeType() {

		GoogleDriveItem item = item(123L, GoogleDriveItemCategory.DOCUMENT, "Doc");

		item.setMimeType(DriveExportFormatRegistry.GOOGLE_SPREADSHEET);

		when(googleDriveItemRepository.findOwnedItemForDetails(123L, GOOGLE_SUBJECT_ID)).thenReturn(Optional.of(item));

		DriveExportOptionsResponse response = service.getExportOptions(GOOGLE_SUBJECT_ID, 123L);

		assertThat(response.itemId()).isEqualTo(123L);
		assertThat(response.sourceMimeType()).isEqualTo(DriveExportFormatRegistry.GOOGLE_SPREADSHEET);
		assertThat(response.formats()).extracting("mimeType").contains("application/pdf", "text/csv");
	}

	private GoogleDriveItem item(Long itemId, GoogleDriveItemCategory category, String name) {

		com.multidrive.api.entity.User user = new com.multidrive.api.entity.User();
		GoogleDriveConnection connection = new GoogleDriveConnection();
		GoogleDriveItem item = new GoogleDriveItem();

		ReflectionTestUtils.setField(user, "id", USER_ID);
		ReflectionTestUtils.setField(connection, "id", 20L);
		connection.setUser(user);
		item.setId(itemId);
		item.setConnection(connection);
		item.setGoogleFileId("file-id");
		item.setName(name);
		item.setMimeType("application/pdf");
		item.setCategory(category);
		item.setSourceType(GoogleDriveItemSourceType.MY_DRIVE);

		return item;
	}

	private GoogleDriveFileResponse remoteFile(String name, String mimeType, String size, boolean trashed,
			GoogleDriveFileCapabilitiesResponse capabilities) {

		return new GoogleDriveFileResponse("file-id", name, mimeType, null, null, List.of("parent-id"), null, null,
				null, size, null, trashed, false, capabilities);
	}

	private GoogleDriveFileCapabilitiesResponse downloadableCapabilities() {

		return new GoogleDriveFileCapabilitiesResponse(null, null, null, null, null, null, null, true, null, null, null,
				null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
				null, null, null, null, null, null, null, null);
	}

}
