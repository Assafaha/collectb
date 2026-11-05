package com.example.collectb.catalog.web;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.collectb.catalog.entity.Collection;
import com.example.collectb.catalog.entity.CollectibleTemplate;
import com.example.collectb.catalog.entity.MediaAsset;
import com.example.collectb.catalog.service.CollectionNotFoundException;
import com.example.collectb.catalog.service.CollectionService;
import com.example.collectb.catalog.service.CollectibleTemplateService;
import com.example.collectb.catalog.service.MediaAssetNotFoundException;
import com.example.collectb.catalog.service.MediaAssetService;
import com.example.collectb.catalog.service.TemplateNotFoundException;

@RestController
@RequestMapping("/api/v1/brands/{brandId}")
public class CatalogController {

	private static final Logger log = LoggerFactory.getLogger(CatalogController.class);

	private final CollectionService collectionService;
	private final CollectibleTemplateService templateService;
	private final MediaAssetService mediaAssetService;

	public CatalogController(CollectionService collectionService, CollectibleTemplateService templateService,
			MediaAssetService mediaAssetService) {
		this.collectionService = collectionService;
		this.templateService = templateService;
		this.mediaAssetService = mediaAssetService;
	}

	@GetMapping("/collections")
	public List<CollectionResponse> listCollections(@PathVariable UUID brandId) {
		return collectionService.list(brandId).stream().map(CollectionResponse::from).toList();
	}

	@PostMapping("/collections")
	public ResponseEntity<CollectionResponse> createCollection(@PathVariable UUID brandId,
			@RequestBody CreateCollectionRequest request, Authentication authentication) {
		Collection collection = collectionService.create(brandId, request, authentication);
		return ResponseEntity.status(HttpStatus.CREATED).body(CollectionResponse.from(collection));
	}

	@GetMapping("/collections/{collectionId}")
	public CollectionResponse getCollection(@PathVariable UUID brandId, @PathVariable UUID collectionId) {
		return CollectionResponse.from(collectionService.get(brandId, collectionId));
	}

	@PatchMapping("/collections/{collectionId}")
	public CollectionResponse updateCollection(@PathVariable UUID brandId, @PathVariable UUID collectionId,
			@RequestBody UpdateCollectionRequest request, Authentication authentication) {
		return CollectionResponse.from(collectionService.update(brandId, collectionId, request, authentication));
	}

	@DeleteMapping("/collections/{collectionId}")
	public ResponseEntity<Void> deleteCollection(@PathVariable UUID brandId, @PathVariable UUID collectionId) {
		collectionService.delete(brandId, collectionId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/collections/{collectionId}/submit-for-review")
	public CollectionResponse submitCollection(@PathVariable UUID brandId, @PathVariable UUID collectionId) {
		return CollectionResponse.from(collectionService.submitForReview(brandId, collectionId));
	}

	@PostMapping("/collections/{collectionId}/withdraw-from-review")
	public CollectionResponse withdrawCollection(@PathVariable UUID brandId, @PathVariable UUID collectionId) {
		return CollectionResponse.from(collectionService.withdrawFromReview(brandId, collectionId));
	}

	@PostMapping("/collections/{collectionId}/review")
	public CollectionResponse reviewCollection(@PathVariable UUID brandId, @PathVariable UUID collectionId,
			@RequestBody CollectionReviewRequest request, Authentication authentication) {
		return CollectionResponse.from(collectionService.review(brandId, collectionId, request.decision(),
				request.comment(), authentication));
	}

	@PostMapping("/collections/{collectionId}/publish")
	public CollectionResponse publishCollection(@PathVariable UUID brandId, @PathVariable UUID collectionId,
			Authentication authentication) {
		return CollectionResponse.from(collectionService.publish(brandId, collectionId, authentication));
	}

	@PostMapping("/collections/{collectionId}/retire")
	public CollectionResponse retireCollection(@PathVariable UUID brandId, @PathVariable UUID collectionId,
			Authentication authentication) {
		return CollectionResponse.from(collectionService.retire(brandId, collectionId, authentication));
	}

	@GetMapping("/collections/{collectionId}/templates")
	public List<TemplateResponse> listTemplates(@PathVariable UUID brandId, @PathVariable UUID collectionId) {
		return templateService.list(brandId, collectionId).stream().map(TemplateResponse::from).toList();
	}

	@PostMapping("/collections/{collectionId}/templates")
	public ResponseEntity<TemplateResponse> createTemplate(@PathVariable UUID brandId,
			@PathVariable UUID collectionId, @RequestBody CreateTemplateRequest request,
			Authentication authentication) {
		CollectibleTemplate template = templateService.create(brandId, collectionId, request, authentication);
		return ResponseEntity.status(HttpStatus.CREATED).body(TemplateResponse.from(template));
	}

	@GetMapping("/collections/{collectionId}/templates/{templateId}")
	public TemplateResponse getTemplate(@PathVariable UUID brandId, @PathVariable UUID collectionId,
			@PathVariable UUID templateId) {
		return TemplateResponse.from(templateService.get(brandId, collectionId, templateId));
	}

	@PatchMapping("/collections/{collectionId}/templates/{templateId}")
	public TemplateResponse updateTemplate(@PathVariable UUID brandId, @PathVariable UUID collectionId,
			@PathVariable UUID templateId, @RequestBody UpdateTemplateRequest request,
			Authentication authentication) {
		return TemplateResponse.from(templateService.update(brandId, collectionId, templateId, request,
				authentication));
	}

	@DeleteMapping("/collections/{collectionId}/templates/{templateId}")
	public ResponseEntity<Void> deleteTemplate(@PathVariable UUID brandId, @PathVariable UUID collectionId,
			@PathVariable UUID templateId) {
		templateService.delete(brandId, collectionId, templateId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/collections/{collectionId}/templates/{templateId}/clone")
	public ResponseEntity<TemplateResponse> cloneTemplate(@PathVariable UUID brandId,
			@PathVariable UUID collectionId, @PathVariable UUID templateId, @RequestBody CloneTemplateRequest request,
			Authentication authentication) {
		CollectibleTemplate clone = templateService.clone(brandId, collectionId, templateId, request.templateCode(),
				request.displayName(), authentication);
		return ResponseEntity.status(HttpStatus.CREATED).body(TemplateResponse.from(clone));
	}

	@PostMapping("/collections/{collectionId}/templates/{templateId}/submit-for-review")
	public TemplateResponse submitTemplate(@PathVariable UUID brandId, @PathVariable UUID collectionId,
			@PathVariable UUID templateId) {
		return TemplateResponse.from(templateService.submitForReview(brandId, collectionId, templateId));
	}

	@PostMapping("/collections/{collectionId}/templates/{templateId}/withdraw-from-review")
	public TemplateResponse withdrawTemplate(@PathVariable UUID brandId, @PathVariable UUID collectionId,
			@PathVariable UUID templateId) {
		return TemplateResponse.from(templateService.withdrawFromReview(brandId, collectionId, templateId));
	}

	@PostMapping("/collections/{collectionId}/templates/{templateId}/review")
	public TemplateResponse reviewTemplate(@PathVariable UUID brandId, @PathVariable UUID collectionId,
			@PathVariable UUID templateId, @RequestBody TemplateReviewRequest request,
			Authentication authentication) {
		return TemplateResponse.from(templateService.review(brandId, collectionId, templateId, request.decision(),
				request.comment(), authentication));
	}

	@PostMapping("/collections/{collectionId}/templates/{templateId}/publish")
	public TemplateResponse publishTemplate(@PathVariable UUID brandId, @PathVariable UUID collectionId,
			@PathVariable UUID templateId, Authentication authentication) {
		return TemplateResponse.from(templateService.publish(brandId, collectionId, templateId, authentication));
	}

	@PostMapping("/collections/{collectionId}/templates/{templateId}/retire")
	public TemplateResponse retireTemplate(@PathVariable UUID brandId, @PathVariable UUID collectionId,
			@PathVariable UUID templateId, Authentication authentication) {
		return TemplateResponse.from(templateService.retire(brandId, collectionId, templateId, authentication));
	}

	@GetMapping("/media-assets")
	public List<MediaAssetResponse> listMediaAssets(@PathVariable UUID brandId) {
		return mediaAssetService.list(brandId).stream().map(MediaAssetResponse::from).toList();
	}

	@PostMapping("/media-assets")
	public ResponseEntity<MediaAssetResponse> registerMediaAsset(@PathVariable UUID brandId,
			@RequestBody CreateMediaAssetRequest request, Authentication authentication) {
		MediaAsset mediaAsset = mediaAssetService.register(brandId, request, authentication);
		return ResponseEntity.status(HttpStatus.CREATED).body(MediaAssetResponse.from(mediaAsset));
	}

	@ExceptionHandler({CollectionNotFoundException.class, TemplateNotFoundException.class,
			MediaAssetNotFoundException.class})
	public ResponseEntity<String> handleNotFound(RuntimeException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
		log.warn("Catalog request rejected: {}", ex.getMessage());
		return ResponseEntity.badRequest().body(ex.getMessage());
	}

	@ExceptionHandler({IllegalStateException.class, DataIntegrityViolationException.class})
	public ResponseEntity<String> handleConflict(RuntimeException ex) {
		log.warn("Catalog state conflict: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
	}
}
