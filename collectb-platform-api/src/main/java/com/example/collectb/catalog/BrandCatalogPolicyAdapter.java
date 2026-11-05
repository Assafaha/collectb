package com.example.collectb.catalog;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.collectb.brand.model.BrandLicenseStatus;
import com.example.collectb.brand.service.BrandService;
import com.example.collectb.catalog.service.BrandCatalogPolicy;

@Component
public class BrandCatalogPolicyAdapter implements BrandCatalogPolicy {

	private final BrandService brandService;

	public BrandCatalogPolicyAdapter(BrandService brandService) {
		this.brandService = brandService;
	}

	@Override
	public void requireActiveBrand(UUID brandId) {
		if (brandService.getBrand(brandId).getLicenseStatus() != BrandLicenseStatus.ACTIVE) {
			throw new IllegalStateException("Brand must have an ACTIVE license to change catalog content");
		}
	}
}
