package com.example.collectb.catalog.web;

import java.time.Instant;
import java.util.UUID;

import com.example.collectb.catalog.entity.Collection;

public record CollectionResponse(UUID collectionId, UUID brandId, String collectionCode, String name,
		String description, String releaseStatus, Instant releaseStartAt, Instant releaseEndAt, UUID createdByUserId,
		UUID updatedByUserId, UUID reviewedByUserId, Instant reviewedAt, String reviewComment, Instant createdAt,
		Instant updatedAt) {

	public static CollectionResponse from(Collection collection) {
		return new CollectionResponse(collection.getCollectionId(), collection.getBrandId(),
				collection.getCollectionCode(), collection.getName(), collection.getDescription(),
				collection.getReleaseStatus().name(), collection.getReleaseStartAt(), collection.getReleaseEndAt(),
				collection.getCreatedByUserId(), collection.getUpdatedByUserId(), collection.getReviewedByUserId(),
				collection.getReviewedAt(), collection.getReviewComment(), collection.getCreatedAt(),
				collection.getUpdatedAt());
	}
}
