package com.example.collectb.catalog.web;

import java.time.Instant;

public record UpdateCollectionRequest(String collectionCode, String name, String description, Instant releaseStartAt,
		Instant releaseEndAt) {
}
