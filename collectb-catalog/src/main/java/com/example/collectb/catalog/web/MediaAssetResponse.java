package com.example.collectb.catalog.web;

import java.time.Instant;
import java.util.UUID;

import com.example.collectb.catalog.entity.MediaAsset;

public record MediaAssetResponse(UUID mediaAssetId, UUID brandId, String assetType, String storageUri,
		String cdnUri, String contentType, String checksum, Long fileSizeBytes, Instant createdAt) {

	public static MediaAssetResponse from(MediaAsset mediaAsset) {
		return new MediaAssetResponse(mediaAsset.getMediaAssetId(), mediaAsset.getBrandId(),
				mediaAsset.getAssetType(), mediaAsset.getStorageUri(), mediaAsset.getCdnUri(),
				mediaAsset.getContentType(), mediaAsset.getChecksum(), mediaAsset.getFileSizeBytes(),
				mediaAsset.getCreatedAt());
	}
}
