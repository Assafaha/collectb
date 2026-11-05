package com.example.collectb.catalog.service;

import java.util.UUID;

/** Cross-context check exposed for the Platform API to implement using the Brand module. */
public interface BrandCatalogPolicy {

	void requireActiveBrand(UUID brandId);
}
