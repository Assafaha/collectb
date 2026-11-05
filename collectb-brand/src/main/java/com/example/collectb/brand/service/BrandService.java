package com.example.collectb.brand.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.collectb.brand.entity.Brand;
import com.example.collectb.brand.model.BrandLicenseStatus;
import com.example.collectb.brand.repository.BrandRepository;
import com.example.collectb.brand.web.CreateBrandRequest;

/**
 * Brand creation/lookup, the entry point of the product-definition layer (design doc: "Nothing
 * can be issued as an ADC until this layer exists"). Brand creation is a platform-level,
 * privileged operation (only {@code PLATFORM_ADMIN} holds {@code PLATFORM_BRAND:MANAGE} per the
 * seeded role/permission catalog) - once a brand exists, day-to-day collection/template editing
 * is delegated to brand-scoped roles ({@code BRAND_ADMIN}/{@code BRAND_EDITOR}), enforced at that
 * later layer, not here.
 */
@Service
public class BrandService {

	private static final Logger log = LoggerFactory.getLogger(BrandService.class);

	private final BrandRepository brandRepository;

	public BrandService(BrandRepository brandRepository) {
		this.brandRepository = brandRepository;
	}

	@PreAuthorize("hasAuthority('PLATFORM_BRAND:MANAGE')")
	@Transactional
	public Brand createBrand(CreateBrandRequest request, Authentication authentication) {
		return createBrand(request, resolveUserId(authentication));
	}

	/**
	 * Overload taking the acting user's id directly, for callers that already know it from a
	 * non-Bearer-token context (e.g. the platform-api admin dashboard, which resolves it from
	 * the session's {@code PlatformUserPrincipal} - a class this module deliberately does not
	 * depend on). Still enforces the same {@code PLATFORM_BRAND:MANAGE} authorization, since it
	 * is invoked as a distinct proxied bean method, not a same-class self-invocation.
	 */
	@PreAuthorize("hasAuthority('PLATFORM_BRAND:MANAGE')")
	@Transactional
	public Brand createBrand(CreateBrandRequest request, UUID actingUserId) {
		if (request.brandCode() == null || request.brandCode().isBlank()) {
			throw new IllegalArgumentException("brandCode is required");
		}
		if (request.name() == null || request.name().isBlank()) {
			throw new IllegalArgumentException("name is required");
		}
		if (brandRepository.existsByBrandCode(request.brandCode())) {
			throw new IllegalArgumentException("brandCode already in use: " + request.brandCode());
		}
		if (brandRepository.existsByName(request.name())) {
			throw new IllegalArgumentException("name already in use: " + request.name());
		}

		Brand brand = new Brand(request.brandCode(), request.name(), request.description(), actingUserId);
		Brand saved = brandRepository.save(brand);
		log.info("Created brand brandId={} brandCode={} createdBy={}", saved.getBrandId(), saved.getBrandCode(),
				actingUserId);
		return saved;
	}

	@Transactional(readOnly = true)
	public List<Brand> listBrands() {
		return brandRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Brand getBrand(UUID brandId) {
		return brandRepository.findById(brandId)
				.orElseThrow(() -> new BrandNotFoundException("No brand for id " + brandId));
	}

	/**
	 * Transitions a brand's licensing lifecycle status (e.g. {@code DRAFT -> ACTIVE} to approve
	 * it for publishing collections/templates, or {@code ACTIVE -> SUSPENDED}/{@code
	 * TERMINATED}), per design doc section on {@code brand.license_status}. A privileged,
	 * platform-level operation - same permission as creating a brand in the first place.
	 */
	@PreAuthorize("hasAuthority('PLATFORM_BRAND:MANAGE')")
	@Transactional
	public Brand updateLicenseStatus(UUID brandId, BrandLicenseStatus newStatus) {
		Brand brand = getBrand(brandId);
		BrandLicenseStatus previousStatus = brand.getLicenseStatus();
		brand.setLicenseStatus(newStatus);
		Brand saved = brandRepository.save(brand);
		log.info("Updated brand license status brandId={} brandCode={} {} -> {}", saved.getBrandId(),
				saved.getBrandCode(), previousStatus, newStatus);
		return saved;
	}

	/**
	 * Resolves the acting user's {@code platform_user.user_id} from the current
	 * {@link Authentication}, without depending on collectb-identity's
	 * {@code PlatformUserPrincipal} class. Bearer-token requests (the expected path for this
	 * endpoint) carry a {@code user_id} claim set identically by both the direct REST login
	 * endpoint and the OAuth2/PKCE flow; any other authentication type falls back to parsing
	 * {@link Authentication#getName()} as a UUID.
	 */
	private static UUID resolveUserId(Authentication authentication) {
		if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
			String userId = jwtAuthentication.getToken().getClaimAsString("user_id");
			if (userId != null) {
				return UUID.fromString(userId);
			}
		}
		if (authentication instanceof AbstractAuthenticationToken) {
			return UUID.fromString(authentication.getName());
		}
		throw new IllegalStateException("Unable to resolve acting user id from authentication");
	}
}
