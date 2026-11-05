package com.example.collectb.catalog.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "media_asset")
public class MediaAsset {

	@Id
	@GeneratedValue
	@Column(name = "media_asset_id")
	private UUID mediaAssetId;

	@Column(name = "brand_id")
	private UUID brandId;

	@Column(name = "asset_type", nullable = false)
	private String assetType;

	@Column(name = "storage_uri", nullable = false)
	private String storageUri;

	@Column(name = "cdn_uri")
	private String cdnUri;

	@Column(name = "content_type")
	private String contentType;

	@Column(name = "checksum", nullable = false)
	private String checksum;

	@Column(name = "file_size_bytes")
	private Long fileSizeBytes;

	@Column(name = "created_by_user_id")
	private UUID createdByUserId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected MediaAsset() {
	}

	public MediaAsset(UUID brandId, String assetType, String storageUri, String cdnUri, String contentType,
			String checksum, Long fileSizeBytes, UUID createdByUserId) {
		this.brandId = brandId;
		this.assetType = assetType;
		this.storageUri = storageUri;
		this.cdnUri = cdnUri;
		this.contentType = contentType;
		this.checksum = checksum;
		this.fileSizeBytes = fileSizeBytes;
		this.createdByUserId = createdByUserId;
	}

	@PrePersist
	protected void onCreate() {
		createdAt = Instant.now();
	}

	public UUID getMediaAssetId() { return mediaAssetId; }
	public UUID getBrandId() { return brandId; }
	public String getAssetType() { return assetType; }
	public String getStorageUri() { return storageUri; }
	public String getCdnUri() { return cdnUri; }
	public String getContentType() { return contentType; }
	public String getChecksum() { return checksum; }
	public Long getFileSizeBytes() { return fileSizeBytes; }
	public UUID getCreatedByUserId() { return createdByUserId; }
	public Instant getCreatedAt() { return createdAt; }
}
