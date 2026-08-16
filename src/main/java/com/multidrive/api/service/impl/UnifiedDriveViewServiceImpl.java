package com.multidrive.api.service.impl;

import com.multidrive.api.dto.UnifiedDriveItemResponse;
import com.multidrive.api.dto.UnifiedDriveItemsPageResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.mapper.DriveItemCapabilityResponseMapper;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.service.UnifiedDriveViewService;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class UnifiedDriveViewServiceImpl implements UnifiedDriveViewService {

	private static final int DEFAULT_PAGE = 0;

	private static final int DEFAULT_PAGE_SIZE = 50;

	private static final int MAX_PAGE_SIZE = 200;

	private final GoogleDriveItemRepository googleDriveItemRepository;

	private final DriveItemCapabilityResponseMapper driveItemCapabilityResponseMapper;

	public UnifiedDriveViewServiceImpl(GoogleDriveItemRepository googleDriveItemRepository,

			DriveItemCapabilityResponseMapper driveItemCapabilityResponseMapper) {

		this.googleDriveItemRepository = googleDriveItemRepository;

		this.driveItemCapabilityResponseMapper = driveItemCapabilityResponseMapper;
	}

	@Override
	@Transactional(readOnly = true)
	public UnifiedDriveItemsPageResponse getDashboard(Long userId, Long connectionId, String parentId,
			String searchQuery, Integer page, Integer size) {

		validateUserId(userId);

		Pageable pageable = createPageable(page, size);

		Specification<GoogleDriveItem> specification = buildSpecification(userId, null, connectionId, parentId,
				searchQuery);

		Page<GoogleDriveItem> itemPage = googleDriveItemRepository.findAll(specification, pageable);

		return toPageResponse(itemPage);
	}

	@Override
	@Transactional(readOnly = true)
	public UnifiedDriveItemsPageResponse getDocs(Long userId, Long connectionId, String parentId, String searchQuery,
			Integer page, Integer size) {

		return getItemsByCategory(userId, GoogleDriveItemCategory.DOCUMENT, connectionId, parentId, searchQuery, page,
				size);
	}

	@Override
	@Transactional(readOnly = true)
	public UnifiedDriveItemsPageResponse getGallery(Long userId, Long connectionId, String parentId, String searchQuery,
			Integer page, Integer size) {

		return getItemsByCategory(userId, GoogleDriveItemCategory.IMAGE, connectionId, parentId, searchQuery, page,
				size);
	}

	@Override
	@Transactional(readOnly = true)
	public UnifiedDriveItemsPageResponse getVideos(Long userId, Long connectionId, String parentId, String searchQuery,
			Integer page, Integer size) {

		return getItemsByCategory(userId, GoogleDriveItemCategory.VIDEO, connectionId, parentId, searchQuery, page,
				size);
	}

	private UnifiedDriveItemsPageResponse getItemsByCategory(Long userId, GoogleDriveItemCategory category,
			Long connectionId, String parentId, String searchQuery, Integer page, Integer size) {

		validateUserId(userId);

		Pageable pageable = createPageable(page, size);

		Specification<GoogleDriveItem> specification = buildSpecification(userId, category, connectionId, parentId,
				searchQuery);

		Page<GoogleDriveItem> itemPage = googleDriveItemRepository.findAll(specification, pageable);

		return toPageResponse(itemPage);
	}

	private Specification<GoogleDriveItem> buildSpecification(Long userId, GoogleDriveItemCategory category,
			Long connectionId, String parentId, String searchQuery) {

		String normalizedParentId = normalizeFilter(parentId);

		String normalizedSearchQuery = normalizeSearchQuery(searchQuery);

		return (root, query, criteriaBuilder) -> {

			/*
			 * Fetch only to-one associations required by the response.
			 *
			 * Do not add fetch joins to the count query.
			 */
			if (!isCountQuery(query.getResultType())) {

				root.fetch("connection", JoinType.INNER);

				root.fetch("source", JoinType.INNER);
			}

			List<Predicate> predicates = new ArrayList<>();

			predicates.add(criteriaBuilder.equal(root.get("connection").get("user").get("id"), userId));

			predicates.add(criteriaBuilder.isFalse(root.<Boolean>get("trashed")));

			if (category != null) {

				predicates.add(criteriaBuilder.equal(root.get("category"), category));
			}

			if (connectionId != null) {

				predicates.add(criteriaBuilder.equal(root.get("connection").get("id"), connectionId));
			}

			if (normalizedParentId != null) {

				predicates.add(criteriaBuilder.equal(root.get("parentId"), normalizedParentId));
			}

			if (normalizedSearchQuery != null) {

				predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("name")),
						"%" + normalizedSearchQuery + "%"));
			}

			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private boolean isCountQuery(Class<?> resultType) {

		return Long.class.equals(resultType) || long.class.equals(resultType);
	}

	private Pageable createPageable(Integer page, Integer size) {

		int requestedPage = page == null ? DEFAULT_PAGE : page;

		int requestedSize = size == null ? DEFAULT_PAGE_SIZE : size;

		if (requestedPage < 0) {

			throw new IllegalArgumentException("page must be 0 or greater");
		}

		if (requestedSize < 1 || requestedSize > MAX_PAGE_SIZE) {

			throw new IllegalArgumentException("size must be between 1 and 200");
		}

		Sort sort = Sort.by(Sort.Direction.DESC, "googleModifiedTime").and(Sort.by(Sort.Direction.DESC, "id"));

		return PageRequest.of(requestedPage, requestedSize, sort);
	}

	private UnifiedDriveItemsPageResponse toPageResponse(Page<GoogleDriveItem> itemPage) {

		List<UnifiedDriveItemResponse> items = itemPage.getContent().stream().map(this::toResponse).toList();

		return new UnifiedDriveItemsPageResponse(items, itemPage.getNumber(), itemPage.getSize(),
				itemPage.getTotalElements(), itemPage.getTotalPages(), itemPage.isFirst(), itemPage.isLast());
	}

	private UnifiedDriveItemResponse toResponse(GoogleDriveItem item) {

		GoogleDriveConnection connection = item.getConnection();

		GoogleDriveSource source = item.getSource();

		return new UnifiedDriveItemResponse(

				item.getId(),

				connection.getId(),

				connection.getGoogleEmail(),

				source.getId(),

				source.getName(),

				item.getGoogleFileId(),

				item.getName(),

				item.getMimeType(),

				item.getCategory(),

				item.getSourceType(),

				item.getParentId(),

				item.getDriveId(),

				item.getWebViewLink(),

				item.getThumbnailLink(),

				item.getIconLink(),

				item.getSizeBytes(),

				item.getGoogleCreatedTime(),

				item.getGoogleModifiedTime(),

				driveItemCapabilityResponseMapper.toResponse(item.getCapabilities()));
	}

	private String normalizeFilter(String value) {

		if (value == null) {
			return null;
		}

		String normalized = value.trim();

		if (normalized.isEmpty()) {
			return null;
		}

		return normalized;
	}

	private String normalizeSearchQuery(String value) {

		String normalized = normalizeFilter(value);

		if (normalized == null) {
			return null;
		}

		return normalized.toLowerCase(Locale.ROOT);
	}

	private void validateUserId(Long userId) {

		if (userId == null) {

			throw new IllegalArgumentException("userId is required");
		}
	}

}