package com.example.collectb.catalog.web;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.example.collectb.catalog.entity.CollectibleTemplate;

public record TemplateResponse(UUID templateId, UUID collectionId, UUID mediaAssetId, String templateCode,
		String displayName, String rarity, int editionSize, String metadataSchemaVersion, String metadataUri,
		String metadataHash, Map<String, String> traits, String publishStatus, UUID createdByUserId,
		UUID updatedByUserId, UUID reviewedByUserId, Instant reviewedAt, String reviewComment, Instant createdAt,
		Instant updatedAt) {

	public static TemplateResponse from(CollectibleTemplate template) {
		return new TemplateResponse(template.getTemplateId(), template.getCollectionId(), template.getMediaAssetId(),
				template.getTemplateCode(), template.getDisplayName(), template.getRarity(), template.getEditionSize(),
				template.getMetadataSchemaVersion(), template.getMetadataUri(), template.getMetadataHash(),
				template.getTraits(), template.getPublishStatus().name(), template.getCreatedByUserId(),
				template.getUpdatedByUserId(), template.getReviewedByUserId(), template.getReviewedAt(),
				template.getReviewComment(), template.getCreatedAt(), template.getUpdatedAt());
	}
}
