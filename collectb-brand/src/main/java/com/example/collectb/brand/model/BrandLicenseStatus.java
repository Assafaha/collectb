package com.example.collectb.brand.model;

/**
 * Lifecycle status of a {@code brand}'s licensing agreement with the platform. Mapped as an
 * enum (via {@code EnumType.STRING}) for type safety even though the {@code license_status}
 * column has no DB {@code CHECK} constraint yet - keep this in sync if one is added later.
 * <ul>
 *   <li>{@code DRAFT} - brand record created but not yet approved to publish collections.</li>
 *   <li>{@code ACTIVE} - licensed and allowed to publish collections/templates.</li>
 *   <li>{@code SUSPENDED} - temporarily blocked (e.g. compliance hold) without deleting data.</li>
 *   <li>{@code TERMINATED} - license ended; brand and its catalog become read-only/archived.</li>
 * </ul>
 */
public enum BrandLicenseStatus {
	DRAFT,
	ACTIVE,
	SUSPENDED,
	TERMINATED
}
