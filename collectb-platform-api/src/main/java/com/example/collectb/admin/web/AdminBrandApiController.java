package com.example.collectb.admin.web;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.collectb.brandportal.web.BrandUserResponse;
import com.example.collectb.brandportal.web.GrantBrandUserRequest;
import com.example.collectb.brand.entity.Brand;
import com.example.collectb.brand.model.BrandLicenseStatus;
import com.example.collectb.brand.service.BrandNotFoundException;
import com.example.collectb.brand.service.BrandService;
import com.example.collectb.brand.web.BrandResponse;
import com.example.collectb.brand.web.CreateBrandRequest;
import com.example.collectb.brand.web.UpdateLicenseStatusRequest;
import com.example.collectb.identity.service.RoleAssignmentAdminService;

/**
 * JSON REST API backing the Angular admin dashboard section: lets a {@code PLATFORM_ADMIN}
 * (holding {@code PLATFORM_BRAND:MANAGE}/{@code PLATFORM_IDENTITY:MANAGE}) create brands,
 * approve/suspend/terminate a brand's license, and manage user access on *any* brand - unlike
 * {@code BrandPortalController}, which restricts a {@code BRAND_ADMIN} to only their own brand.
 * <p>
 * Deliberately does not reuse {@code BrandPortalController}'s brand-scoped methods
 * ({@code listAssignmentViewsForBrand}/{@code assignBrandRole}/{@code revokeBrandAssignment}),
 * which also accept a brand-scoped {@code BRAND_ADMIN}; this controller only ever calls the
 * platform-admin-only variants ({@code listAssignmentViewsForScope}/{@code assignRole}/
 * {@code revokeAssignment}), so it can never be reached by a brand-scoped user regardless of
 * how the underlying service methods evolve.
 * <p>
 * This is the sole admin UI backend now - the earlier server-rendered Thymeleaf + htmx dashboard
 * ({@code AdminBrandController} at {@code /admin/brands}) has been removed in favor of the
 * Angular admin-portal's {@code admin/brands} routes, which call these endpoints directly.
 */
@RestController
@RequestMapping("/api/v1/admin/brands")
public class AdminBrandApiController {

	private static final Logger log = LoggerFactory.getLogger(AdminBrandApiController.class);

	private final BrandService brandService;
	private final RoleAssignmentAdminService roleAssignmentAdminService;

	public AdminBrandApiController(BrandService brandService, RoleAssignmentAdminService roleAssignmentAdminService) {
		this.brandService = brandService;
		this.roleAssignmentAdminService = roleAssignmentAdminService;
	}

	@GetMapping
	public List<BrandResponse> listBrands() {
		return brandService.listBrands().stream().map(BrandResponse::from).toList();
	}

	@PostMapping
	public ResponseEntity<BrandResponse> createBrand(@RequestBody CreateBrandRequest request,
			Authentication authentication) {
		// Uses the UUID overload (not BrandService.createBrand(request, authentication)) because
		// that overload's own Authentication-based resolution only handles Bearer-token requests
		// correctly; this controller is reached via the browser session flow (PlatformUserPrincipal),
		// which RoleAssignmentAdminService.resolveActingUserId already knows how to handle.
		UUID actingUserId = RoleAssignmentAdminService.resolveActingUserId(authentication);
		Brand brand = brandService.createBrand(request, actingUserId);
		return ResponseEntity.status(HttpStatus.CREATED).body(BrandResponse.from(brand));
	}

	@GetMapping("/{brandId}")
	public BrandResponse getBrand(@PathVariable UUID brandId) {
		return BrandResponse.from(brandService.getBrand(brandId));
	}

	@PatchMapping("/{brandId}/license-status")
	public BrandResponse updateLicenseStatus(@PathVariable UUID brandId,
			@RequestBody UpdateLicenseStatusRequest request) {
		BrandLicenseStatus newStatus;
		try {
			newStatus = BrandLicenseStatus.valueOf(request.licenseStatus());
		} catch (IllegalArgumentException | NullPointerException ex) {
			throw new IllegalArgumentException("licenseStatus must be one of "
					+ List.of(BrandLicenseStatus.values()) + ", got: " + request.licenseStatus());
		}
		return BrandResponse.from(brandService.updateLicenseStatus(brandId, newStatus));
	}

	@GetMapping("/{brandId}/users")
	public List<BrandUserResponse> listBrandUsers(@PathVariable UUID brandId) {
		return roleAssignmentAdminService.listAssignmentViewsForScope("BRAND", brandId).stream()
				.map(BrandUserResponse::from)
				.toList();
	}

	@GetMapping("/{brandId}/users/assignable-roles")
	public List<String> listAssignableRoles() {
		return roleAssignmentAdminService.listRoleNamesByType("BRAND");
	}

	@PostMapping("/{brandId}/users")
	public ResponseEntity<BrandUserResponse> grantBrandUser(@PathVariable UUID brandId,
			@RequestBody GrantBrandUserRequest request, Authentication authentication) {
		var assignment = roleAssignmentAdminService.assignRole(request.email(), request.roleName(), "BRAND", brandId,
				authentication);
		var view = roleAssignmentAdminService.listAssignmentViewsForScope("BRAND", brandId).stream()
				.filter(v -> v.roleAssignmentId().equals(assignment.getRoleAssignmentId()))
				.findFirst()
				.orElseThrow();
		return ResponseEntity.status(HttpStatus.CREATED).body(BrandUserResponse.from(view));
	}

	@PostMapping("/{brandId}/users/{assignmentId}/revoke")
	public ResponseEntity<Void> revokeBrandUser(@PathVariable UUID assignmentId) {
		roleAssignmentAdminService.revokeAssignment(assignmentId);
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(BrandNotFoundException.class)
	public ResponseEntity<String> handleNotFound(BrandNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
	}

	@ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
	public ResponseEntity<String> handleBadRequest(RuntimeException ex) {
		log.warn("Admin brand API request rejected: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
	}
}
