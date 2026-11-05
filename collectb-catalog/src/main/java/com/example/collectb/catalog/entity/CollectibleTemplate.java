package com.example.collectb.catalog.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.example.collectb.catalog.model.TemplateStatus;

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
@Table(name = "collectible_template")
public class CollectibleTemplate {

	@Id
	@GeneratedValue
	@Column(name = "template_id")
	private UUID templateId;

	@Column(name = "collection_id", nullable = false)
	private UUID collectionId;

	@Column(name = "media_asset_id")
	private UUID mediaAssetId;

	@Column(name = "template_code", nullable = false)
	private String templateCode;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	@Column(name = "rarity")
	private String rarity;

	@Column(name = "edition_size", nullable = false)
	private int editionSize;

	@Column(name = "metadata_schema_version", nullable = false)
	private String metadataSchemaVersion;

	@Column(name = "metadata_uri", nullable = false)
	private String metadataUri;

	@Column(name = "metadata_hash")
	private String metadataHash;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "traits", columnDefinition = "jsonb")
	private Map<String, String> traits;

	@Enumerated(EnumType.STRING)
	@Column(name = "publish_status", nullable = false)
	private TemplateStatus publishStatus;

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

	protected CollectibleTemplate() {
	}

	public CollectibleTemplate(UUID collectionId, UUID mediaAssetId, String templateCode, String displayName,
			String rarity, int editionSize, String metadataSchemaVersion, String metadataUri, String metadataHash,
			Map<String, String> traits, UUID actorId) {
		this.collectionId = collectionId;
		this.mediaAssetId = mediaAssetId;
		this.templateCode = templateCode;
		this.displayName = displayName;
		this.rarity = rarity;
		this.editionSize = editionSize;
		this.metadataSchemaVersion = metadataSchemaVersion;
		this.metadataUri = metadataUri;
		this.metadataHash = metadataHash;
		this.traits = traits == null ? null : Map.copyOf(traits);
		this.publishStatus = TemplateStatus.DRAFT;
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

	public void update(UUID mediaAssetId, String templateCode, String displayName, String rarity, int editionSize,
			String metadataSchemaVersion, String metadataUri, String metadataHash, Map<String, String> traits,
			UUID actorId) {
		this.mediaAssetId = mediaAssetId;
		this.templateCode = templateCode;
		this.displayName = displayName;
		this.rarity = rarity;
		this.editionSize = editionSize;
		this.metadataSchemaVersion = metadataSchemaVersion;
		this.metadataUri = metadataUri;
		this.metadataHash = metadataHash;
		this.traits = traits == null ? null : Map.copyOf(traits);
		this.updatedByUserId = actorId;
	}

	public void transition(TemplateStatus status, UUID reviewerId, Instant reviewedAt, String reviewComment) {
		this.publishStatus = status;
		this.reviewedByUserId = reviewerId;
		this.reviewedAt = reviewedAt;
		this.reviewComment = reviewComment;
	}

	public UUID getTemplateId() { return templateId; }
	public UUID getCollectionId() { return collectionId; }
	public UUID getMediaAssetId() { return mediaAssetId; }
	public String getTemplateCode() { return templateCode; }
	public String getDisplayName() { return displayName; }
	public String getRarity() { return rarity; }
	public int getEditionSize() { return editionSize; }
	public String getMetadataSchemaVersion() { return metadataSchemaVersion; }
	public String getMetadataUri() { return metadataUri; }
	public String getMetadataHash() { return metadataHash; }
	public Map<String, String> getTraits() { return traits == null ? null : Map.copyOf(traits); }
	public TemplateStatus getPublishStatus() { return publishStatus; }
	public UUID getCreatedByUserId() { return createdByUserId; }
	public UUID getUpdatedByUserId() { return updatedByUserId; }
	public UUID getReviewedByUserId() { return reviewedByUserId; }
	public Instant getReviewedAt() { return reviewedAt; }
	public String getReviewComment() { return reviewComment; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
