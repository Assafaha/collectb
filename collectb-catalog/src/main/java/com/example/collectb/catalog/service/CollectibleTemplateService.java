package com.example.collectb.catalog.service;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.collectb.catalog.entity.Collection;
import com.example.collectb.catalog.entity.CollectibleTemplate;
import com.example.collectb.catalog.model.CollectionStatus;
import com.example.collectb.catalog.model.TemplateStatus;
import com.example.collectb.catalog.repository.CollectionRepository;
import com.example.collectb.catalog.repository.CollectibleTemplateRepository;
import com.example.collectb.catalog.web.CreateTemplateRequest;
import com.example.collectb.catalog.web.UpdateTemplateRequest;

@Service
public class CollectibleTemplateService {

	private static final Logger log = LoggerFactory.getLogger(CollectibleTemplateService.class);
	private static final String BRAND_READ = "hasAuthority('TEMPLATE:READ') and "
			+ "@brandScopeAuthorizationService.hasBrandRole(authentication, #brandId, "
			+ "'BRAND_ADMIN', 'BRAND_EDITOR', 'BRAND_VIEWER')";
	private static final String BRAND_EDIT = "@brandScopeAuthorizationService.hasBrandRole(authentication, "
			+ "#brandId, 'BRAND_ADMIN', 'BRAND_EDITOR')";
	private static final String BRAND_ADMIN = "@brandScopeAuthorizationService.hasBrandRole(authentication, "
			+ "#brandId, 'BRAND_ADMIN')";
	private static final String STEP_UP = "@catalogStepUpAuthorizationService.hasRecentStepUp(authentication)";

	private final CollectibleTemplateRepository templateRepository;
	private final CollectionRepository collectionRepository;
	private final MediaAssetService mediaAssetService;
	private final BrandCatalogPolicy brandCatalogPolicy;

	public CollectibleTemplateService(CollectibleTemplateRepository templateRepository,
			CollectionRepository collectionRepository, MediaAssetService mediaAssetService,
			BrandCatalogPolicy brandCatalogPolicy) {
		this.templateRepository = templateRepository;
		this.collectionRepository = collectionRepository;
		this.mediaAssetService = mediaAssetService;
		this.brandCatalogPolicy = brandCatalogPolicy;
	}

	@PreAuthorize(BRAND_READ)
	@Transactional(readOnly = true)
	public List<CollectibleTemplate> list(UUID brandId, UUID collectionId) {
		findCollection(brandId, collectionId);
		return templateRepository.findByCollectionIdOrderByCreatedAtDesc(collectionId);
	}

	@PreAuthorize(BRAND_READ)
	@Transactional(readOnly = true)
	public CollectibleTemplate get(UUID brandId, UUID collectionId, UUID templateId) {
		findCollection(brandId, collectionId);
		return find(collectionId, templateId);
	}

	@PreAuthorize("hasAuthority('TEMPLATE:CREATE') and " + BRAND_EDIT)
	@Transactional
	public CollectibleTemplate create(UUID brandId, UUID collectionId, CreateTemplateRequest request,
			Authentication authentication) {
		brandCatalogPolicy.requireActiveBrand(brandId);
		Collection collection = findCollection(brandId, collectionId);
		requireCollectionAllowsTemplateChanges(collection);
		TemplateFields fields = validate(request.templateCode(), request.displayName(), request.rarity(),
				request.editionSize(), request.metadataSchemaVersion(), request.metadataUri(), request.metadataHash(),
				request.traits());
		if (templateRepository.existsByCollectionIdAndTemplateCode(collectionId, fields.code())) {
			throw new IllegalArgumentException("templateCode already in use for this collection: " + fields.code());
		}
		validateMediaAsset(brandId, request.mediaAssetId());
		UUID actorId = CatalogActor.userId(authentication);
		CollectibleTemplate template = new CollectibleTemplate(collectionId, request.mediaAssetId(), fields.code(),
				fields.displayName(), fields.rarity(), fields.editionSize(), fields.metadataSchemaVersion(),
				fields.metadataUri(), fields.metadataHash(), fields.traits(), actorId);
		CollectibleTemplate saved = templateRepository.save(template);
		log.info("Created template templateId={} collectionId={} brandId={} createdBy={}", saved.getTemplateId(),
				collectionId, brandId, actorId);
		return saved;
	}

	@PreAuthorize("hasAuthority('TEMPLATE:UPDATE') and " + BRAND_EDIT)
	@Transactional
	public CollectibleTemplate update(UUID brandId, UUID collectionId, UUID templateId,
			UpdateTemplateRequest request, Authentication authentication) {
		brandCatalogPolicy.requireActiveBrand(brandId);
		Collection collection = findCollection(brandId, collectionId);
		requireCollectionAllowsTemplateChanges(collection);
		CollectibleTemplate template = find(collectionId, templateId);
		requireStatus(template, TemplateStatus.DRAFT, TemplateStatus.CHANGES_REQUESTED);
		TemplateFields fields = validate(request.templateCode(), request.displayName(), request.rarity(),
				request.editionSize(), request.metadataSchemaVersion(), request.metadataUri(), request.metadataHash(),
				request.traits());
		if (templateRepository.existsByCollectionIdAndTemplateCodeAndTemplateIdNot(collectionId, fields.code(),
				templateId)) {
			throw new IllegalArgumentException("templateCode already in use for this collection: " + fields.code());
		}
		validateMediaAsset(brandId, request.mediaAssetId());
		template.update(request.mediaAssetId(), fields.code(), fields.displayName(), fields.rarity(),
				fields.editionSize(), fields.metadataSchemaVersion(), fields.metadataUri(), fields.metadataHash(),
				fields.traits(), CatalogActor.userId(authentication));
		return templateRepository.save(template);
	}

	@PreAuthorize("hasAuthority('TEMPLATE:DELETE') and " + BRAND_EDIT)
	@Transactional
	public void delete(UUID brandId, UUID collectionId, UUID templateId) {
		brandCatalogPolicy.requireActiveBrand(brandId);
		findCollection(brandId, collectionId);
		CollectibleTemplate template = find(collectionId, templateId);
		requireStatus(template, TemplateStatus.DRAFT);
		templateRepository.delete(template);
	}

	@PreAuthorize("hasAuthority('TEMPLATE:CLONE') and " + BRAND_EDIT)
	@Transactional
	public CollectibleTemplate clone(UUID brandId, UUID collectionId, UUID templateId, String templateCode,
			String displayName, Authentication authentication) {
		brandCatalogPolicy.requireActiveBrand(brandId);
		Collection collection = findCollection(brandId, collectionId);
		requireCollectionAllowsTemplateChanges(collection);
		CollectibleTemplate source = find(collectionId, templateId);
		String code = required(templateCode, "templateCode", 80);
		String name = required(displayName, "displayName", 200);
		if (templateRepository.existsByCollectionIdAndTemplateCode(collectionId, code)) {
			throw new IllegalArgumentException("templateCode already in use for this collection: " + code);
		}
		UUID actorId = CatalogActor.userId(authentication);
		return templateRepository.save(new CollectibleTemplate(collectionId, source.getMediaAssetId(), code, name,
				source.getRarity(), source.getEditionSize(), source.getMetadataSchemaVersion(), source.getMetadataUri(),
				source.getMetadataHash(), source.getTraits(), actorId));
	}

	@PreAuthorize("hasAuthority('TEMPLATE:SUBMIT_FOR_REVIEW') and " + BRAND_EDIT)
	@Transactional
	public CollectibleTemplate submitForReview(UUID brandId, UUID collectionId, UUID templateId) {
		brandCatalogPolicy.requireActiveBrand(brandId);
		findCollection(brandId, collectionId);
		CollectibleTemplate template = find(collectionId, templateId);
		requireStatus(template, TemplateStatus.DRAFT, TemplateStatus.CHANGES_REQUESTED);
		template.transition(TemplateStatus.READY_FOR_REVIEW, null, null, null);
		return templateRepository.save(template);
	}

	@PreAuthorize("hasAuthority('TEMPLATE:WITHDRAW_FROM_REVIEW') and " + BRAND_EDIT)
	@Transactional
	public CollectibleTemplate withdrawFromReview(UUID brandId, UUID collectionId, UUID templateId) {
		CollectibleTemplate template = findNested(brandId, collectionId, templateId);
		requireStatus(template, TemplateStatus.READY_FOR_REVIEW);
		template.transition(TemplateStatus.DRAFT, null, null, null);
		return templateRepository.save(template);
	}

	@PreAuthorize("hasAuthority('TEMPLATE:APPROVE') and " + BRAND_ADMIN + " and " + STEP_UP)
	@Transactional
	public CollectibleTemplate review(UUID brandId, UUID collectionId, UUID templateId, String decision,
			String comment, Authentication authentication) {
		Collection collection = findCollection(brandId, collectionId);
		requireStatus(collection, CollectionStatus.APPROVED, CollectionStatus.PUBLISHED);
		CollectibleTemplate template = find(collectionId, templateId);
		requireStatus(template, TemplateStatus.READY_FOR_REVIEW);
		UUID actorId = CatalogActor.userId(authentication);
		requireIndependentReviewer(template.getCreatedByUserId(), template.getUpdatedByUserId(), actorId);
		TemplateStatus next = parseReviewDecision(decision, comment);
		template.transition(next, actorId, Instant.now(), normalizeComment(comment));
		log.info("Reviewed template templateId={} decision={} reviewer={}", templateId, next, actorId);
		return templateRepository.save(template);
	}

	@PreAuthorize("hasAuthority('TEMPLATE:PUBLISH') and " + BRAND_ADMIN + " and " + STEP_UP)
	@Transactional
	public CollectibleTemplate publish(UUID brandId, UUID collectionId, UUID templateId, Authentication authentication) {
		brandCatalogPolicy.requireActiveBrand(brandId);
		Collection collection = findCollection(brandId, collectionId);
		requireStatus(collection, CollectionStatus.APPROVED, CollectionStatus.PUBLISHED);
		CollectibleTemplate template = find(collectionId, templateId);
		requireStatus(template, TemplateStatus.APPROVED);
		template.transition(TemplateStatus.PUBLISHED, CatalogActor.userId(authentication), Instant.now(), null);
		return templateRepository.save(template);
	}

	@PreAuthorize("hasAuthority('TEMPLATE:RETIRE') and " + BRAND_ADMIN + " and " + STEP_UP)
	@Transactional
	public CollectibleTemplate retire(UUID brandId, UUID collectionId, UUID templateId,
			Authentication authentication) {
		CollectibleTemplate template = findNested(brandId, collectionId, templateId);
		requireStatus(template, TemplateStatus.PUBLISHED);
		template.transition(TemplateStatus.RETIRED, CatalogActor.userId(authentication), Instant.now(), null);
		return templateRepository.save(template);
	}

	private CollectibleTemplate findNested(UUID brandId, UUID collectionId, UUID templateId) {
		findCollection(brandId, collectionId);
		return find(collectionId, templateId);
	}

	private Collection findCollection(UUID brandId, UUID collectionId) {
		return collectionRepository.findByCollectionIdAndBrandId(collectionId, brandId)
				.orElseThrow(() -> new CollectionNotFoundException(
						"No collection " + collectionId + " for brand " + brandId));
	}

	private CollectibleTemplate find(UUID collectionId, UUID templateId) {
		return templateRepository.findByTemplateIdAndCollectionId(templateId, collectionId)
				.orElseThrow(() -> new TemplateNotFoundException(
						"No template " + templateId + " for collection " + collectionId));
	}

	private void validateMediaAsset(UUID brandId, UUID mediaAssetId) {
		if (mediaAssetId != null) {
			mediaAssetService.requireForBrand(mediaAssetId, brandId);
		}
	}

	private static void requireCollectionAllowsTemplateChanges(Collection collection) {
		if (collection.getReleaseStatus() == CollectionStatus.PUBLISHED
				|| collection.getReleaseStatus() == CollectionStatus.RETIRED) {
			throw new IllegalStateException("Templates cannot be changed in a published or retired collection");
		}
	}

	private static TemplateStatus parseReviewDecision(String decision, String comment) {
		if ("APPROVE".equals(decision)) {
			return TemplateStatus.APPROVED;
		}
		if ("REQUEST_CHANGES".equals(decision)) {
			if (normalizeComment(comment) == null) {
				throw new IllegalArgumentException("A comment is required when requesting changes");
			}
			return TemplateStatus.CHANGES_REQUESTED;
		}
		throw new IllegalArgumentException("decision must be APPROVE or REQUEST_CHANGES");
	}

	private static void requireIndependentReviewer(UUID createdBy, UUID updatedBy, UUID reviewerId) {
		if (reviewerId.equals(createdBy) || reviewerId.equals(updatedBy)) {
			throw new AccessDeniedException("The creator or last editor cannot review their own template");
		}
	}

	private static void requireStatus(CollectibleTemplate template, TemplateStatus... allowed) {
		for (TemplateStatus status : allowed) {
			if (template.getPublishStatus() == status) {
				return;
			}
		}
		throw new IllegalStateException("Template status " + template.getPublishStatus()
				+ " does not allow this operation");
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

	private static TemplateFields validate(String code, String name, String rarity, int editionSize,
			String metadataSchemaVersion, String metadataUri, String metadataHash, Map<String, String> traits) {
		String normalizedCode = required(code, "templateCode", 80);
		String normalizedName = required(name, "displayName", 200);
		String normalizedRarity = optional(rarity, "rarity", 50);
		String normalizedSchema = required(metadataSchemaVersion, "metadataSchemaVersion", 20);
		String normalizedUri = required(metadataUri, "metadataUri", 2048);
		validateHttpsUri(normalizedUri);
		String normalizedHash = optional(metadataHash, "metadataHash", 128);
		if (normalizedHash != null && !normalizedHash.matches("(?i)[0-9a-f]{64}|[0-9a-f]{128}")) {
			throw new IllegalArgumentException("metadataHash must be a SHA-256 or SHA-512 hex digest");
		}
		if (editionSize <= 0) {
			throw new IllegalArgumentException("editionSize must be positive");
		}
		if (traits != null && traits.entrySet().stream()
				.anyMatch(entry -> entry.getKey() == null || entry.getKey().isBlank()
						|| entry.getValue() == null || entry.getValue().isBlank())) {
			throw new IllegalArgumentException("trait names and values must not be blank");
		}
		return new TemplateFields(normalizedCode, normalizedName, normalizedRarity, editionSize, normalizedSchema,
				normalizedUri, normalizedHash, traits);
	}

	private static void validateHttpsUri(String value) {
		try {
			URI uri = URI.create(value);
			String scheme = uri.getScheme();
			boolean validHttps = "https".equalsIgnoreCase(scheme) && uri.getHost() != null;
			boolean validS3 = "s3".equalsIgnoreCase(scheme) && uri.getHost() != null;
			if (!validHttps && !validS3) {
				throw new IllegalArgumentException("metadataUri must be an absolute HTTPS or S3 URI");
			}
		} catch (IllegalArgumentException ex) {
			if (ex.getMessage() != null && ex.getMessage().startsWith("metadataUri must be an absolute")) {
				throw ex;
			}
			throw new IllegalArgumentException("metadataUri must be a valid absolute HTTPS or S3 URI", ex);
		}
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

	private static String optional(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return required(value, field, maxLength);
	}

	private static String normalizeComment(String comment) {
		return comment == null || comment.isBlank() ? null : comment.trim();
	}

	private record TemplateFields(String code, String displayName, String rarity, int editionSize,
			String metadataSchemaVersion, String metadataUri, String metadataHash, Map<String, String> traits) {
	}
}
