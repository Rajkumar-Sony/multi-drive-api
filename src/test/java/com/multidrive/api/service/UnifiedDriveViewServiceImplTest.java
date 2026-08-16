package com.multidrive.api.service;

import com.multidrive.api.dto.UnifiedDriveItemsPageResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCapabilities;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.mapper.DriveItemCapabilityResponseMapper;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.service.impl.UnifiedDriveViewServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnifiedDriveViewServiceImplTest {

	@Mock
	private GoogleDriveItemRepository googleDriveItemRepository;

	private UnifiedDriveViewServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new UnifiedDriveViewServiceImpl(googleDriveItemRepository, new DriveItemCapabilityResponseMapper());
	}

	@Test
	@SuppressWarnings("unchecked")
	void getDashboardUsesDefaultPagingAndMapsItemResponseWithoutExposingEntities() {

		GoogleDriveItem item = item(GoogleDriveItemCategory.IMAGE);

		when(googleDriveItemRepository.findAll(org.mockito.ArgumentMatchers.any(Specification.class),
				org.mockito.ArgumentMatchers.any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(item)));

		UnifiedDriveItemsPageResponse response = service.getDashboard(42L, 20L, " parent-1 ", " Photo ", null, null);

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		verify(googleDriveItemRepository).findAll(org.mockito.ArgumentMatchers.any(Specification.class),
				pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
		assertThat(pageableCaptor.getValue().getSort().getOrderFor("googleModifiedTime").getDirection().isDescending())
			.isTrue();
		assertThat(response.items()).hasSize(1);
		assertThat(response.items().getFirst().id()).isEqualTo(100L);
		assertThat(response.items().getFirst().connectionId()).isEqualTo(20L);
		assertThat(response.items().getFirst().accountEmail()).isEqualTo("user@example.com");
		assertThat(response.items().getFirst().sourceId()).isEqualTo(10L);
		assertThat(response.items().getFirst().sourceName()).isEqualTo("My Drive");
		assertThat(response.items().getFirst().googleFileId()).isEqualTo("file-1");
		assertThat(response.items().getFirst().capabilities().edit()).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	void categoryViewsUseRequestedPagingBounds() {

		when(googleDriveItemRepository.findAll(org.mockito.ArgumentMatchers.any(Specification.class),
				org.mockito.ArgumentMatchers.any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(25).withPage(2), 75));

		UnifiedDriveItemsPageResponse response = service.getDocs(42L, null, null, null, 2, 25);

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		verify(googleDriveItemRepository).findAll(org.mockito.ArgumentMatchers.any(Specification.class),
				pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(25);
		assertThat(response.page()).isEqualTo(2);
		assertThat(response.size()).isEqualTo(25);
		assertThat(response.totalElements()).isEqualTo(75);
		assertThat(response.totalPages()).isEqualTo(3);
	}

	@Test
	void viewsValidateUserIdAndPageBounds() {

		assertThatThrownBy(() -> service.getDashboard(null, null, null, null, null, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("userId is required");
		assertThatThrownBy(() -> service.getGallery(42L, null, null, null, -1, 50))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("page must be 0 or greater");
		assertThatThrownBy(() -> service.getVideos(42L, null, null, null, 0, 201))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("size must be between 1 and 200");
	}

	private GoogleDriveItem item(GoogleDriveItemCategory category) {

		GoogleDriveConnection connection = new GoogleDriveConnection();
		ReflectionTestUtils.setField(connection, "id", 20L);
		connection.setGoogleEmail("user@example.com");

		GoogleDriveSource source = new GoogleDriveSource();
		ReflectionTestUtils.setField(source, "id", 10L);
		source.setName("My Drive");

		GoogleDriveItemCapabilities capabilities = new GoogleDriveItemCapabilities();
		capabilities.setCanEdit(true);

		GoogleDriveItem item = new GoogleDriveItem();
		ReflectionTestUtils.setField(item, "id", 100L);
		item.setConnection(connection);
		item.setSource(source);
		item.setGoogleFileId("file-1");
		item.setName("Photo.png");
		item.setMimeType("image/png");
		item.setCategory(category);
		item.setSourceType(GoogleDriveItemSourceType.MY_DRIVE);
		item.setParentId("parent-1");
		item.setWebViewLink("web");
		item.setThumbnailLink("thumb");
		item.setIconLink("icon");
		item.setSizeBytes(1024L);
		item.setGoogleCreatedTime(Instant.parse("2026-08-16T10:00:00Z"));
		item.setGoogleModifiedTime(Instant.parse("2026-08-16T11:00:00Z"));
		item.setCapabilities(capabilities);

		return item;
	}

}
