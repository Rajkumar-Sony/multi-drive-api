package com.multidrive.api.service;

import com.multidrive.api.dto.DriveOperationJobResponse;
import com.multidrive.api.dto.DriveOperationJobSubmitRequest;
import com.multidrive.api.dto.DriveOperationJobsPageResponse;
import com.multidrive.api.entity.DriveConflictStrategy;
import com.multidrive.api.entity.DriveOperationItem;
import com.multidrive.api.entity.DriveOperationItemStatus;
import com.multidrive.api.entity.DriveOperationJob;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.entity.DriveOperationType;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.entity.GoogleDriveSourceStatus;
import com.multidrive.api.exception.DriveOperationJobNotFoundException;
import com.multidrive.api.model.DriveOperationPlan;
import com.multidrive.api.model.DriveOperationStrategyType;
import com.multidrive.api.repository.DriveOperationItemRepository;
import com.multidrive.api.repository.DriveOperationJobRepository;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.repository.GoogleDriveSourceRepository;
import com.multidrive.api.service.impl.DriveOperationJobServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriveOperationJobServiceImplTest {

	private static final String GOOGLE_SUBJECT_ID = "google-subject-123";

	@Mock
	private DriveOperationJobRepository driveOperationJobRepository;

	@Mock
	private DriveOperationItemRepository driveOperationItemRepository;

	@Mock
	private GoogleDriveItemRepository googleDriveItemRepository;

	@Mock
	private GoogleDriveSourceRepository googleDriveSourceRepository;

	@Mock
	private DriveOperationPlanner driveOperationPlanner;

	private DriveOperationJobServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new DriveOperationJobServiceImpl(driveOperationJobRepository, driveOperationItemRepository,
				googleDriveItemRepository, googleDriveSourceRepository, driveOperationPlanner,
				new TestTransactionManager());
	}

	@Test
	void submitReturnsExistingJobForMatchingIdempotencyKeyBeforeCreatingNewJob() {

		DriveOperationJobResponse response = response(10L, DriveOperationJobStatus.QUEUED);

		when(driveOperationJobRepository.findResponseByGoogleSubjectIdAndIdempotencyKey(GOOGLE_SUBJECT_ID,
				"submit-key"))
			.thenReturn(Optional.of(response));

		DriveOperationJobSubmitRequest request = new DriveOperationJobSubmitRequest(DriveOperationType.COPY, 123L, 30L,
				null, null, null);

		assertThat(service.submit(GOOGLE_SUBJECT_ID, " submit-key ", request)).isSameAs(response);

		verifyNoInteractions(googleDriveItemRepository, googleDriveSourceRepository, driveOperationPlanner,
				driveOperationItemRepository);
	}

	@Test
	void submitCreatesQueuedJobWithDefaultsAndRootDestination() {

		GoogleDriveConnection connection = connection(20L);
		GoogleDriveSource source = source(30L, connection);
		GoogleDriveItem item = item(123L, source, connection);
		DriveOperationJobResponse response = response(10L, DriveOperationJobStatus.QUEUED);

		when(driveOperationJobRepository.findResponseByGoogleSubjectIdAndIdempotencyKey(GOOGLE_SUBJECT_ID,
				"submit-key"))
			.thenReturn(Optional.empty());
		when(googleDriveItemRepository.findOwnedItemForDetails(123L, GOOGLE_SUBJECT_ID)).thenReturn(Optional.of(item));
		when(googleDriveSourceRepository.findOwnedSourceForOperation(30L, GOOGLE_SUBJECT_ID,
				GoogleDriveSourceStatus.ACTIVE))
			.thenReturn(Optional.of(source));
		when(driveOperationPlanner.planCopy(item, source))
			.thenReturn(new DriveOperationPlan(DriveOperationStrategyType.NATIVE_COPY, "Native copy"));
		when(driveOperationJobRepository.saveAndFlush(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
			DriveOperationJob job = invocation.getArgument(0);
			ReflectionTestUtils.setField(job, "id", 10L);
			return job;
		});
		when(driveOperationJobRepository.findResponseByIdAndGoogleSubjectId(10L, GOOGLE_SUBJECT_ID))
			.thenReturn(Optional.of(response));

		DriveOperationJobSubmitRequest request = new DriveOperationJobSubmitRequest(DriveOperationType.COPY, 123L, 30L,
				null, " Report Copy.pdf ", null);

		assertThat(service.submit(GOOGLE_SUBJECT_ID, " submit-key ", request)).isSameAs(response);

		ArgumentCaptor<DriveOperationJob> jobCaptor = ArgumentCaptor.forClass(DriveOperationJob.class);
		ArgumentCaptor<DriveOperationItem> itemCaptor = ArgumentCaptor.forClass(DriveOperationItem.class);

		verify(driveOperationJobRepository).saveAndFlush(jobCaptor.capture());
		verify(driveOperationItemRepository).save(itemCaptor.capture());

		assertThat(jobCaptor.getValue().getStatus()).isEqualTo(DriveOperationJobStatus.QUEUED);
		assertThat(jobCaptor.getValue().getConflictStrategy()).isEqualTo(DriveConflictStrategy.KEEP_BOTH);
		assertThat(jobCaptor.getValue().getStrategyType()).isEqualTo(DriveOperationStrategyType.NATIVE_COPY);
		assertThat(jobCaptor.getValue().getDestinationParentGoogleFileId()).isEqualTo("root");
		assertThat(jobCaptor.getValue().getRequestedName()).isEqualTo("Report Copy.pdf");
		assertThat(jobCaptor.getValue().getIdempotencyKey()).isEqualTo("submit-key");
		assertThat(itemCaptor.getValue().getStatus()).isEqualTo(DriveOperationItemStatus.QUEUED);
		assertThat(itemCaptor.getValue().getDestinationParentGoogleFileId()).isEqualTo("root");
	}

	@Test
	void getJobsNormalizesPaginationAndRejectsInvalidBounds() {

		DriveOperationJobResponse response = response(10L, DriveOperationJobStatus.QUEUED);

		when(driveOperationJobRepository.findResponsesByGoogleSubjectId(
				org.mockito.ArgumentMatchers.eq(GOOGLE_SUBJECT_ID), org.mockito.ArgumentMatchers.any()))
			.thenReturn(new PageImpl<>(List.of(response)));

		DriveOperationJobsPageResponse page = service.getJobs(GOOGLE_SUBJECT_ID, null, null, null);

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		verify(driveOperationJobRepository).findResponsesByGoogleSubjectId(
				org.mockito.ArgumentMatchers.eq(GOOGLE_SUBJECT_ID), pageableCaptor.capture());

		assertThat(page.items()).containsExactly(response);
		assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);

		assertThatThrownBy(() -> service.getJobs(GOOGLE_SUBJECT_ID, null, -1, 20))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("page must be greater than or equal to 0");

		assertThatThrownBy(() -> service.getJobs(GOOGLE_SUBJECT_ID, null, 0, 101))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("size must be between 1 and 100");
	}

	@Test
	void getJobRejectsInvalidInputAndMissingJob() {

		assertThatThrownBy(() -> service.getJob(" ", 10L)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("googleSubjectId is required");

		assertThatThrownBy(() -> service.getJob(GOOGLE_SUBJECT_ID, null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("jobId is required");

		when(driveOperationJobRepository.findResponseByIdAndGoogleSubjectId(10L, GOOGLE_SUBJECT_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getJob(GOOGLE_SUBJECT_ID, 10L))
			.isInstanceOf(DriveOperationJobNotFoundException.class)
			.hasMessage("Drive operation job not found: 10");
	}

	@Test
	void cancelMarksQueuedJobCancelledAndLeavesTerminalJobUnchanged() {

		DriveOperationJob queuedJob = new DriveOperationJob();

		queuedJob.setStatus(DriveOperationJobStatus.QUEUED);

		DriveOperationJob terminalJob = new DriveOperationJob();

		terminalJob.setStatus(DriveOperationJobStatus.COMPLETED);

		when(driveOperationJobRepository.findByIdAndUser_GoogleSubjectId(10L, GOOGLE_SUBJECT_ID))
			.thenReturn(Optional.of(queuedJob));
		when(driveOperationJobRepository.findResponseByIdAndGoogleSubjectId(10L, GOOGLE_SUBJECT_ID))
			.thenReturn(Optional.of(response(10L, DriveOperationJobStatus.CANCELLED)));
		when(driveOperationJobRepository.findByIdAndUser_GoogleSubjectId(11L, GOOGLE_SUBJECT_ID))
			.thenReturn(Optional.of(terminalJob));
		when(driveOperationJobRepository.findResponseByIdAndGoogleSubjectId(11L, GOOGLE_SUBJECT_ID))
			.thenReturn(Optional.of(response(11L, DriveOperationJobStatus.COMPLETED)));

		assertThat(service.cancel(GOOGLE_SUBJECT_ID, 10L).status()).isEqualTo(DriveOperationJobStatus.CANCELLED);
		assertThat(queuedJob.getCancelRequested()).isTrue();
		assertThat(queuedJob.getStatus()).isEqualTo(DriveOperationJobStatus.CANCELLED);
		assertThat(queuedJob.getCompletedAt()).isNotNull();

		assertThat(service.cancel(GOOGLE_SUBJECT_ID, 11L).status()).isEqualTo(DriveOperationJobStatus.COMPLETED);
		assertThat(terminalJob.getCancelRequested()).isNull();
		assertThat(terminalJob.getStatus()).isEqualTo(DriveOperationJobStatus.COMPLETED);
	}

	private DriveOperationJobResponse response(Long jobId, DriveOperationJobStatus status) {

		LocalDateTime now = LocalDateTime.of(2026, 8, 16, 12, 0);

		return new DriveOperationJobResponse(jobId, DriveOperationType.COPY, DriveOperationStrategyType.NATIVE_COPY,
				status, DriveConflictStrategy.KEEP_BOTH, 123L, "Report.pdf", "application/pdf", 30L, 30L, null, "root",
				null, null, null, 1L, 0L, 0L, 1024L, 0L, 0, 3, status == DriveOperationJobStatus.CANCELLED, null, null,
				null, null, status == DriveOperationJobStatus.CANCELLED ? now : null, now, now);
	}

	private GoogleDriveConnection connection(Long connectionId) {

		GoogleDriveConnection connection = org.mockito.Mockito.mock(GoogleDriveConnection.class);

		when(connection.getId()).thenReturn(connectionId);
		when(connection.getUser()).thenReturn(org.mockito.Mockito.mock(com.multidrive.api.entity.User.class));

		return connection;
	}

	private GoogleDriveSource source(Long sourceId, GoogleDriveConnection connection) {

		GoogleDriveSource source = org.mockito.Mockito.mock(GoogleDriveSource.class);

		when(source.getId()).thenReturn(sourceId);
		when(source.getConnection()).thenReturn(connection);
		when(source.getRootFolderId()).thenReturn(null);

		return source;
	}

	private GoogleDriveItem item(Long itemId, GoogleDriveSource source, GoogleDriveConnection connection) {

		GoogleDriveItem item = org.mockito.Mockito.mock(GoogleDriveItem.class);

		when(item.getId()).thenReturn(itemId);
		when(item.getConnection()).thenReturn(connection);
		when(item.getSource()).thenReturn(source);
		when(item.getGoogleFileId()).thenReturn("google-file-id");
		when(item.getName()).thenReturn("Report.pdf");
		when(item.getMimeType()).thenReturn("application/pdf");
		when(item.getParentId()).thenReturn("old-parent");
		when(item.getSizeBytes()).thenReturn(1024L);
		when(item.isTrashed()).thenReturn(false);

		return item;
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
