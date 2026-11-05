package com.example.collectb.brand.web;

import java.time.Instant;
import java.util.UUID;

import com.example.collectb.brand.entity.Brand;

/** Response body for brand endpoints. */
public record BrandResponse(
		UUID brandId,
		String brandCode,
		String name,
		String description,
		String licenseStatus,
		UUID createdByUserId,
		Instant createdAt,
		Instant updatedAt) {

	public static BrandResponse from(Brand brand) {
		return new BrandResponse(
				brand.getBrandId(),
				brand.getBrandCode(),
				brand.getName(),
				brand.getDescription(),
				brand.getLicenseStatus().name(),
				brand.getCreatedByUserId(),
				brand.getCreatedAt(),
				brand.getUpdatedAt());
	}
}
