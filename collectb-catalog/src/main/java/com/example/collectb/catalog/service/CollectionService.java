package com.example.collectb.catalog.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.collectb.catalog.entity.Collection;
import com.example.collectb.catalog.model.CollectionStatus;
import com.example.collectb.catalog.repository.CollectionRepository;
import com.example.collectb.catalog.repository.CollectibleTemplateRepository;
import com.example.collectb.catalog.web.CreateCollectionRequest;
import com.example.collectb.catalog.web.UpdateCollectionRequest;

@Service
public class CollectionService {

	private static final Logger log = LoggerFactory.getLogger(CollectionService.class);
	private static final String BRAND_READ = "hasAuthority('COLLECTION:READ') and "
			+ "@brandScopeAuthorizationService.hasBrandRole(authentication, #brandId, "
			+ "'BRAND_ADMIN', 'BRAND_EDITOR', 'BRAND_VIEWER')";
	private static final String BRAND_EDIT = "@brandScopeAuthorizationService.hasBrandRole(authentication, "
			+ "#brandId, 'BRAND_ADMIN', 'BRAND_EDITOR')";
	private static final String BRAND_ADMIN = "@brandScopeAuthorizationService.hasBrandRole(authentication, "
			+ "#brandId, 'BRAND_ADMIN')";

	private final CollectionRepository collectionRepository;
	private final CollectibleTemplateRepository templateRepository;
	private final BrandCatalogPolicy brandCatalogPolicy;

	public CollectionService(CollectionRepository collectionRepository,
			CollectibleTemplateRepository templateRepository, BrandCatalogPolicy brandCatalogPolicy) {
		this.collectionRepository = collectionRepository;
		this.templateRepository = templateRepository;
		this.brandCatalogPolicy = brandCatalogPolicy;
	}

	@PreAuthorize(BRAND_READ)
	@Transactional(readOnly = true)
	public List<Collection> list(UUID brandId) {
		return collectionRepository.findByBrandIdOrderByCreatedAtDesc(brandId);
	}

	@PreAuthorize(BRAND_READ)
	@Transactional(readOnly = true)
	public Collection get(UUID brandId, UUID collectionId) {
		return find(brandId, collectionId);
	}

	@PreAuthorize("hasAuthority('COLLECTION:CREATE') and " + BRAND_EDIT)
	@Transactional
	public Collection create(UUID brandId, CreateCollectionRequest request, Authentication authentication) {
		brandCatalogPolicy.requireActiveBrand(brandId);
		CollectionFields fields = validate(request.collectionCode(), request.name(), request.description(),
				request.releaseStartAt(), request.releaseEndAt());
		if (collectionRepository.existsByBrandIdAndCollectionCode(brandId, fields.code())) {
			throw new IllegalArgumentException("collectionCode already in use for this brand: " + fields.code());
		}
		UUID actorId = CatalogActor.userId(authentication);
		Collection collection = new Collection(brandId, fields.code(), fields.name(), fields.description(),
				fields.releaseStartAt(), fields.releaseEndAt(), actorId);
		Collection saved = collectionRepository.save(collection);
		log.info("Created collection collectionId={} brandId={} createdBy={}", saved.getCollectionId(), brandId,
				actorId);
		return saved;
	}

	@PreAuthorize("hasAuthority('COLLECTION:UPDATE') and " + BRAND_EDIT)
	@Transactional
	public Collection update(UUID brandId, UUID collectionId, UpdateCollectionRequest request,
			Authentication authentication) {
		brandCatalogPolicy.requireActiveBrand(brandId);
		Collection collection = find(brandId, collectionId);
		requireStatus(collection, CollectionStatus.DRAFT, CollectionStatus.CHANGES_REQUESTED);
		CollectionFields fields = validate(request.collectionCode(), request.name(), request.description(),
				request.releaseStartAt(), request.releaseEndAt());
		if (collectionRepository.existsByBrandIdAndCollectionCodeAndCollectionIdNot(brandId, fields.code(),
				collectionId)) {
			throw new IllegalArgumentException("collectionCode already in use for this brand: " + fields.code());
		}
		collection.update(fields.code(), fields.name(), fields.description(), fields.releaseStartAt(),
				fields.releaseEndAt(), CatalogActor.userId(authentication));
		return collectionRepository.save(collection);
	}

	@PreAuthorize("hasAuthority('COLLECTION:DELETE') and " + BRAND_EDIT)
	@Transactional
	public void delete(UUID brandId, UUID collectionId) {
		brandCatalogPolicy.requireActiveBrand(brandId);
		Collection collection = find(brandId, collectionId);
		requireStatus(collection, CollectionStatus.DRAFT);
		if (templateRepository.existsByCollectionId(collectionId)) {
			throw new IllegalStateException("A collection containing templates cannot be deleted");
		}
		collectionRepository.delete(collection);
	}

	@PreAuthorize("hasAuthority('COLLECTION:SUBMIT_FOR_REVIEW') and " + BRAND_EDIT)
	@Transactional
	public Collection submitForReview(UUID brandId, UUID collectionId) {
		brandCatalogPolicy.requireActiveBrand(brandId);
		Collection collection = find(brandId, collectionId);
		requireStatus(collection, CollectionStatus.DRAFT, CollectionStatus.CHANGES_REQUESTED);
		collection.transition(CollectionStatus.READY_FOR_REVIEW, null, null, null);
		return collectionRepository.save(collection);
	}

	@PreAuthorize("hasAuthority('COLLECTION:WITHDRAW_FROM_REVIEW') and " + BRAND_EDIT)
	@Transactional
	public Collection withdrawFromReview(UUID brandId, UUID collectionId) {
		Collection collection = find(brandId, collectionId);
		requireStatus(collection, CollectionStatus.READY_FOR_REVIEW);
		collection.transition(CollectionStatus.DRAFT, null, null, null);
		return collectionRepository.save(collection);
	}

	@PreAuthorize("hasAuthority('COLLECTION:APPROVE') and " + BRAND_ADMIN + " and "
			+ "@catalogStepUpAuthorizationService.hasRecentStepUp(authentication)")
	@Transactional
	public Collection review(UUID brandId, UUID collectionId, String decision, String comment,
			Authentication authentication) {
		Collection collection = find(brandId, collectionId);
		requireStatus(collection, CollectionStatus.READY_FOR_REVIEW);
		UUID actorId = CatalogActor.userId(authentication);
		requireIndependentReviewer(collection.getCreatedByUserId(), collection.getUpdatedByUserId(), actorId);
		CollectionStatus next = parseReviewDecision(decision, comment);
		collection.transition(next, actorId, Instant.now(), normalizeComment(comment));
		log.info("Reviewed collection collectionId={} decision={} reviewer={}", collectionId, next, actorId);
		return collectionRepository.save(collection);
	}

	@PreAuthorize("hasAuthority('COLLECTION:PUBLISH') and " + BRAND_ADMIN + " and "
			+ "@catalogStepUpAuthorizationService.hasRecentStepUp(authentication)")
	@Transactional
	public Collection publish(UUID brandId, UUID collectionId, Authentication authentication) {
		brandCatalogPolicy.requireActiveBrand(brandId);
		Collection collection = find(brandId, collectionId);
		requireStatus(collection, CollectionStatus.APPROVED);
		if (!templateRepository.existsByCollectionIdAndPublishStatus(collectionId,
				com.example.collectb.catalog.model.TemplateStatus.PUBLISHED)) {
			throw new IllegalStateException("A collection needs a published template before it can be published");
		}
		collection.transition(CollectionStatus.PUBLISHED, CatalogActor.userId(authentication), Instant.now(), null);
		return collectionRepository.save(collection);
	}

	@PreAuthorize("hasAuthority('COLLECTION:RETIRE') and " + BRAND_ADMIN + " and "
			+ "@catalogStepUpAuthorizationService.hasRecentStepUp(authentication)")
	@Transactional
	public Collection retire(UUID brandId, UUID collectionId, Authentication authentication) {
		Collection collection = find(brandId, collectionId);
		requireStatus(collection, CollectionStatus.PUBLISHED);
		collection.transition(CollectionStatus.RETIRED, CatalogActor.userId(authentication), Instant.now(), null);
		return collectionRepository.save(collection);
	}

	private Collection find(UUID brandId, UUID collectionId) {
		return collectionRepository.findByCollectionIdAndBrandId(collectionId, brandId)
				.orElseThrow(() -> new CollectionNotFoundException(
						"No collection " + collectionId + " for brand " + brandId));
	}

	private static CollectionStatus parseReviewDecision(String decision, String comment) {
		if ("APPROVE".equals(decision)) {
			return CollectionStatus.APPROVED;
		}
		if ("REQUEST_CHANGES".equals(decision)) {
			if (normalizeComment(comment) == null) {
				throw new IllegalArgumentException("A comment is required when requesting changes");
			}
			return CollectionStatus.CHANGES_REQUESTED;
		}
		throw new IllegalArgumentException("decision must be APPROVE or REQUEST_CHANGES");
	}

	private static void requireIndependentReviewer(UUID createdBy, UUID updatedBy, UUID reviewerId) {
		if (reviewerId.equals(createdBy) || reviewerId.equals(updatedBy)) {
			throw new AccessDeniedException("The creator or last editor cannot review their own catalog content");
		}
	}

	private static void requireStatus(Collection collection, CollectionStatus... allowed) {
		for (CollectionStatus status : allowed) {
			if (collection.getReleaseStatus() == status) {
				return;
			}
		}
		throw new IllegalStateException("Collection status " + collection.getReleaseStatus()
				+ " does not allow this operation");
	}

	private static CollectionFields validate(String code, String name, String description, Instant start, Instant end) {
		String normalizedCode = required(code, "collectionCode", 80);
		String normalizedName = required(name, "name", 200);
		if (description != null && description.length() > 10_000) {
			throw new IllegalArgumentException("description must not exceed 10000 characters");
		}
		if (start != null && end != null && !end.isAfter(start)) {
			throw new IllegalArgumentException("releaseEndAt must be after releaseStartAt");
		}
		return new CollectionFields(normalizedCode, normalizedName, description, start, end);
	}

	private static String required(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " is required");
		}
		String normalized = value.trim();
		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters");
		}
		return normalized;
	}

	private static String normalizeComment(String comment) {
		return comment == null || comment.isBlank() ? null : comment.trim();
	}

	private record CollectionFields(String code, String name, String description, Instant releaseStartAt,
			Instant releaseEndAt) {
	}
}
