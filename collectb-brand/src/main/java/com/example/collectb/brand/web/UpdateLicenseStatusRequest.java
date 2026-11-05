package com.example.collectb.brand.web;

/** Request body for {@code PATCH /api/v1/admin/brands/{brandId}/license-status}. {@code
 *  licenseStatus} must be one of {@link com.example.collectb.brand.model.BrandLicenseStatus}'s
 *  names (e.g. {@code "ACTIVE"}). */
public record UpdateLicenseStatusRequest(String licenseStatus) {
}
