package com.example.collectb.brand.web;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.collectb.brand.entity.Brand;
import com.example.collectb.brand.service.BrandNotFoundException;
import com.example.collectb.brand.service.BrandService;

/**
 * Brand endpoints, per design doc "Brand and Catalog: the product-definition layer". Reading
 * the brand directory ({@code GET}) is public (see
 * {@code AuthorizationServerConfig#defaultSecurityFilterChain}); creating a brand
 * ({@code POST}) requires an authenticated caller holding the {@code PLATFORM_BRAND:MANAGE}
 * permission (enforced by {@link BrandService#createBrand}'s {@code @PreAuthorize}).
 */
@RestController
public class BrandController {

	private static final Logger log = LoggerFactory.getLogger(BrandController.class);

	private final BrandService brandService;

	public BrandController(BrandService brandService) {
		this.brandService = brandService;
	}

	@PostMapping("/api/v1/brands")
	public ResponseEntity<BrandResponse> createBrand(@RequestBody CreateBrandRequest request,
			Authentication authentication) {
		Brand brand = brandService.createBrand(request, authentication);
		return ResponseEntity.status(HttpStatus.CREATED).body(BrandResponse.from(brand));
	}

	@GetMapping("/api/v1/brands")
	public List<BrandResponse> listBrands() {
		return brandService.listBrands().stream().map(BrandResponse::from).toList();
	}

	@GetMapping("/api/v1/brands/{brandId}")
	public BrandResponse getBrand(@PathVariable UUID brandId) {
		return BrandResponse.from(brandService.getBrand(brandId));
	}

	@ExceptionHandler(BrandNotFoundException.class)
	public ResponseEntity<String> handleNotFound(BrandNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
	}

	@ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
	public ResponseEntity<String> handleBadRequest(RuntimeException ex) {
		log.warn("Request rejected: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
	}
}
