package com.example.collectb.catalog.web;

public record CreateMediaAssetRequest(String assetType, String storageUri, String cdnUri, String contentType,
		String checksum, Long fileSizeBytes) {
}
