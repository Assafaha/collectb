package com.example.collectb.brand.web;

/**
 * Request body for {@code POST /api/v1/brands}. {@code brandCode} is a short, URL/code-safe
 * identifier (unique per design doc); {@code name} is the human-facing display name (also
 * unique, per the {@code uq_brand_name} DB constraint).
 */
public record CreateBrandRequest(String brandCode, String name, String description) {
}
