package com.example.collectb.catalog.web;

import java.time.Instant;

public record CreateCollectionRequest(String collectionCode, String name, String description, Instant releaseStartAt,
		Instant releaseEndAt) {
}
