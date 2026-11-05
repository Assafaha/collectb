package com.example.collectb.catalog.web;

import java.util.Map;
import java.util.UUID;

public record CreateTemplateRequest(String templateCode, String displayName, String rarity, int editionSize,
		String metadataSchemaVersion, String metadataUri, String metadataHash, UUID mediaAssetId,
		Map<String, String> traits) {
}
