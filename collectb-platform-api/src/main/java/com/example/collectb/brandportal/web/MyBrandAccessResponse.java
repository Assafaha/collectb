package com.example.collectb.brandportal.web;

import java.time.Instant;
import java.util.UUID;

/** One brand the calling user has BRAND-scoped role access to, for the "My Brands" screen. */
public record MyBrandAccessResponse(
		UUID brandId,
		String brandCode,
		String name,
		String licenseStatus,
		String myRoleName,
		Instant createdAt) {
}
