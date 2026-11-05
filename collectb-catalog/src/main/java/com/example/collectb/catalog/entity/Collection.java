package com.example.collectb.catalog.entity;

import java.time.Instant;
import java.util.UUID;

import com.example.collectb.catalog.model.CollectionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "collection")
public class Collection {

	@Id
	@GeneratedValue
	@Column(name = "collection_id")
	private UUID collectionId;

	@Column(name = "brand_id", nullable = false)
	private UUID brandId;

	@Column(name = "collection_code", nullable = false)
	private String collectionCode;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "description")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "release_status", nullable = false)
	private CollectionStatus releaseStatus;

	@Column(name = "release_start_at")
	private Instant releaseStartAt;

	@Column(name = "release_end_at")
	private Instant releaseEndAt;

	@Column(name = "created_by_user_id", nullable = false, updatable = false)
	private UUID createdByUserId;

	@Column(name = "updated_by_user_id")
	private UUID updatedByUserId;

	@Column(name = "reviewed_by_user_id")
	private UUID reviewedByUserId;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@Column(name = "review_comment")
	private String reviewComment;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Collection() {
	}

	public Collection(UUID brandId, String collectionCode, String name, String description, Instant releaseStartAt,
			Instant releaseEndAt, UUID actorId) {
		this.brandId = brandId;
		this.collectionCode = collectionCode;
		this.name = name;
		this.description = description;
		this.releaseStatus = CollectionStatus.DRAFT;
		this.releaseStartAt = releaseStartAt;
		this.releaseEndAt = releaseEndAt;
		this.createdByUserId = actorId;
		this.updatedByUserId = actorId;
	}

	@PrePersist
	protected void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = Instant.now();
	}

	public void update(String collectionCode, String name, String description, Instant releaseStartAt,
			Instant releaseEndAt, UUID actorId) {
		this.collectionCode = collectionCode;
		this.name = name;
		this.description = description;
		this.releaseStartAt = releaseStartAt;
		this.releaseEndAt = releaseEndAt;
		this.updatedByUserId = actorId;
	}

	public void transition(CollectionStatus status, UUID reviewerId, Instant reviewedAt, String reviewComment) {
		this.releaseStatus = status;
		this.reviewedByUserId = reviewerId;
		this.reviewedAt = reviewedAt;
		this.reviewComment = reviewComment;
	}

	public UUID getCollectionId() { return collectionId; }
	public UUID getBrandId() { return brandId; }
	public String getCollectionCode() { return collectionCode; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	public CollectionStatus getReleaseStatus() { return releaseStatus; }
	public Instant getReleaseStartAt() { return releaseStartAt; }
	public Instant getReleaseEndAt() { return releaseEndAt; }
	public UUID getCreatedByUserId() { return createdByUserId; }
	public UUID getUpdatedByUserId() { return updatedByUserId; }
	public UUID getReviewedByUserId() { return reviewedByUserId; }
	public Instant getReviewedAt() { return reviewedAt; }
	public String getReviewComment() { return reviewComment; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
