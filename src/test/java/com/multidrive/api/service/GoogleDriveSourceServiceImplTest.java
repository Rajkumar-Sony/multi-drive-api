package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveRootResponse;
import com.multidrive.api.dto.GoogleDriveSourceResponse;
import com.multidrive.api.dto.GoogleSharedDriveResponse;
import com.multidrive.api.dto.GoogleSharedDrivesResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.entity.GoogleDriveSourceStatus;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.mapper.GoogleDriveCapabilityMapper;
import com.multidrive.api.repository.GoogleDriveConnectionRepository;
import com.multidrive.api.repository.GoogleDriveSourceRepository;
import com.multidrive.api.service.impl.GoogleDriveSourceServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleDriveSourceServiceImplTest {

	@Mock
	private GoogleDriveConnectionRepository googleDriveConnectionRepository;

	@Mock
	private GoogleDriveSourceRepository googleDriveSourceRepository;

	@Mock
	private GoogleDriveService googleDriveService;

	private GoogleDriveSourceServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new GoogleDriveSourceServiceImpl(googleDriveConnectionRepository, googleDriveSourceRepository,
				googleDriveService, new GoogleDriveCapabilityMapper(), new TestTransactionManager());
	}

	@Test
	@SuppressWarnings("unchecked")
	void refreshSourcesPersistsMyDriveAndUniqueSharedDrivesThenMarksStaleSharedDrivesInactive() {

		GoogleDriveConnection connection = new GoogleDriveConnection();
		GoogleDriveSource activeSource = new GoogleDriveSource();

		when(googleDriveConnectionRepository.findByIdAndUserId(20L, 42L)).thenReturn(Optional.of(connection));
		when(googleDriveService.getMyDriveRoot(20L, 42L))
			.thenReturn(new GoogleDriveRootResponse("root-id", " ", "2026-08-16T12:00:00Z", null));
		when(googleDriveService.getSharedDrives(20L, 42L, 100, null)).thenReturn(new GoogleSharedDrivesResponse(
				List.of(sharedDrive("drive-a", "Team A", "2026-08-16T12:00:00Z"), sharedDrive(" ", "Ignored", null)),
				"page-2"));
		when(googleDriveService.getSharedDrives(20L, 42L, 100, "page-2"))
			.thenReturn(new GoogleSharedDrivesResponse(List.of(sharedDrive("drive-a", "Team A Duplicate", null),
					sharedDrive("drive-b", null, "not-a-timestamp")), null));
		when(googleDriveSourceRepository.findAllByConnection_Id(20L)).thenReturn(List.of());
		when(googleDriveSourceRepository.markUndiscoveredSharedDriveSourcesInactive(
				org.mockito.ArgumentMatchers.eq(20L),
				org.mockito.ArgumentMatchers.eq(GoogleDriveSourceType.SHARED_DRIVE),
				org.mockito.ArgumentMatchers.eq(GoogleDriveSourceStatus.ACTIVE),
				org.mockito.ArgumentMatchers.eq(GoogleDriveSourceStatus.INACTIVE),
				org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
			.thenReturn(1);
		when(googleDriveSourceRepository.findAllByConnection_IdAndStatusOrderBySourceTypeAscNameAsc(20L,
				GoogleDriveSourceStatus.ACTIVE))
			.thenReturn(List.of(activeSource));

		assertThat(service.refreshSources(20L, 42L)).containsExactly(activeSource);

		ArgumentCaptor<List<GoogleDriveSource>> sourcesCaptor = ArgumentCaptor.forClass(List.class);

		verify(googleDriveSourceRepository).saveAll(sourcesCaptor.capture());
		verify(googleDriveSourceRepository).flush();

		assertThat(sourcesCaptor.getValue()).hasSize(3);
		assertThat(sourcesCaptor.getValue().get(0).getSourceType()).isEqualTo(GoogleDriveSourceType.MY_DRIVE);
		assertThat(sourcesCaptor.getValue().get(0).getName()).isEqualTo("My Drive");
		assertThat(sourcesCaptor.getValue()).extracting(GoogleDriveSource::getGoogleDriveId)
			.contains(null, "drive-a", "drive-b");
		assertThat(sourcesCaptor.getValue()).allMatch(source -> source.getStatus() == GoogleDriveSourceStatus.ACTIVE);
	}

	@Test
	void activeSourceLookupsValidateIdsAndDelegateToRepository() {

		GoogleDriveSourceResponse sourceResponse = new GoogleDriveSourceResponse(10L, 20L, "user@example.com",
				GoogleDriveSourceType.MY_DRIVE, null, "root-id", "My Drive", GoogleDriveSourceStatus.ACTIVE,
				LocalDateTime.of(2026, 8, 16, 12, 0));

		when(googleDriveSourceRepository.findSourceResponsesByUserIdAndStatus(42L, GoogleDriveSourceStatus.ACTIVE))
			.thenReturn(List.of(sourceResponse));
		when(googleDriveSourceRepository.findSourceResponsesByUserIdAndConnectionIdAndStatus(42L, 20L,
				GoogleDriveSourceStatus.ACTIVE))
			.thenReturn(List.of(sourceResponse));

		assertThat(service.getActiveSourceResponses(42L)).containsExactly(sourceResponse);
		assertThat(service.getActiveSourceResponses(42L, 20L)).containsExactly(sourceResponse);

		assertThatThrownBy(() -> service.getActiveSourceResponses(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("userId is required");
		assertThatThrownBy(() -> service.getActiveSourceResponses(42L, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("connectionId is required");
	}

	@Test
	void refreshSourcesRejectsMissingConnection() {

		when(googleDriveConnectionRepository.findByIdAndUserId(20L, 42L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.refreshSources(20L, 42L)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Google Drive connection not found");
	}

	private GoogleSharedDriveResponse sharedDrive(String id, String name, String createdTime) {

		return new GoogleSharedDriveResponse(id, name, false, createdTime, null, null);
	}

	private static final class TestTransactionManager extends AbstractPlatformTransactionManager {

		@Override
		protected Object doGetTransaction() {

			return new Object();
		}

		@Override
		protected void doBegin(Object transaction, TransactionDefinition definition) {
		}

		@Override
		protected void doCommit(DefaultTransactionStatus status) {
		}

		@Override
		protected void doRollback(DefaultTransactionStatus status) {
		}

	}

}
