package com.example.collectb.brandportal.web;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.collectb.brand.entity.Brand;
import com.example.collectb.brand.service.BrandNotFoundException;
import com.example.collectb.brand.service.BrandService;
import com.example.collectb.identity.entity.PlatformUser;
import com.example.collectb.identity.repository.PlatformUserRepository;
import com.example.collectb.identity.security.PlatformUserPrincipal.ScopeGrant;
import com.example.collectb.identity.service.RoleAssignmentAdminService;
import com.example.collectb.catalog.security.CatalogStepUpAuthorizationService;

/**
 * JSON REST API backing the Angular Brand Portal: lets a {@code BRAND_ADMIN}/{@code
 * BRAND_EDITOR}/{@code BRAND_VIEWER} see which brand(s) they have access to, and lets a {@code
 * BRAND_ADMIN} self-manage users on their own brand (grant/revoke), without needing the
 * platform-wide {@code PLATFORM_IDENTITY:MANAGE} permission - authorization is enforced per
 * brand by {@link RoleAssignmentAdminService}'s brand-scoped methods (see {@code
 * BrandScopeAuthorizationService}).
 * <p>
 * Lives in {@code collectb-platform-api}, not either bounded-context module, for the same reason
 * as {@code AdminBrandApiController}: it composes Brand (collectb-brand) with role-assignment
 * (collectb-identity) data, and the Platform API is the module that already assembles both.
 */
@RestController
public class BrandPortalController {

	private static final Logger log = LoggerFactory.getLogger(BrandPortalController.class);

	private final BrandService brandService;
	private final RoleAssignmentAdminService roleAssignmentAdminService;
	private final PlatformUserRepository platformUserRepository;
	private final CatalogStepUpAuthorizationService catalogStepUpAuthorizationService;

	public BrandPortalController(BrandService brandService, RoleAssignmentAdminService roleAssignmentAdminService,
			PlatformUserRepository platformUserRepository, CatalogStepUpAuthorizationService catalogStepUpAuthorizationService) {
		this.brandService = brandService;
		this.roleAssignmentAdminService = roleAssignmentAdminService;
		this.platformUserRepository = platformUserRepository;
		this.catalogStepUpAuthorizationService = catalogStepUpAuthorizationService;
	}

	/** The calling user's own profile, for the portal's account/profile view (top-right icon).
	 *  Looks up the {@code platform_user} row fresh from the DB (rather than trusting only the
	 *  JWT's claims) so email/display name are always current even for long-lived access tokens,
	 *  but derives roles/permissions from the {@link Authentication}'s granted authorities so
	 *  they're accurate for both the browser-session and Bearer-token flows alike. */
	@GetMapping("/api/v1/me")
	public MeResponse me(Authentication authentication) {
		UUID userId = RoleAssignmentAdminService.resolveActingUserId(authentication);
		PlatformUser user = platformUserRepository.findById(userId)
				.orElseThrow(() -> new IllegalStateException("No platform_user for userId " + userId));

		List<String> roles = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(authority -> authority.startsWith("ROLE_"))
				.map(authority -> authority.substring("ROLE_".length()))
				.toList();
		// Permission keys are always "RESOURCE:ACTION" style (e.g. COLLECTION:UPDATE); this
		// excludes the internal auth-strength markers also carried as authorities (e.g.
		// FACTOR_PASSWORD/FACTOR_BEARER, added by FactorGrantedAuthority) since those aren't
		// permissions a user would recognize on a profile screen.
		List<String> permissions = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(authority -> !authority.startsWith("ROLE_") && authority.contains(":"))
				.toList();

		String authStrength = authentication instanceof JwtAuthenticationToken jwt
				? jwt.getToken().getClaimAsString("auth_strength") : "pwd";
		return new MeResponse(user.getUserId(), user.getEmail(), user.getDisplayName(),
				user.getActorType().name(), roles, permissions, authStrength,
				catalogStepUpAuthorizationService.isMfaRequired());
	}

	@GetMapping("/api/v1/me/brands")
	public List<MyBrandAccessResponse> myBrands(Authentication authentication) {
		List<ScopeGrant> myBrandGrants = roleAssignmentAdminService.listMyBrandAccess(authentication);
		return myBrandGrants.stream()
				.<MyBrandAccessResponse>mapMulti((grant, consumer) -> {
					try {
						Brand brand = brandService.getBrand(grant.scopeId());
						consumer.accept(new MyBrandAccessResponse(brand.getBrandId(), brand.getBrandCode(),
								brand.getName(), brand.getLicenseStatus().name(), grant.roleName(),
								brand.getCreatedAt()));
					} catch (BrandNotFoundException ex) {
						// Defensive: a role_assignment can reference a brand_id that no longer
						// (or never did) resolve to an actual `brand` row - skip it rather than
						// failing the whole "my brands" list for one stale/placeholder grant.
						log.warn("Skipping stale brand scope grant for missing brandId={}", grant.scopeId());
					}
				})
				.toList();
	}

	@GetMapping("/api/v1/brands/{brandId}/users")
	public List<BrandUserResponse> listBrandUsers(@PathVariable UUID brandId) {
		return roleAssignmentAdminService.listAssignmentViewsForBrand(brandId).stream()
				.map(BrandUserResponse::from)
				.toList();
	}

	@GetMapping("/api/v1/brands/{brandId}/users/assignable-roles")
	public List<String> listAssignableRoles(@PathVariable UUID brandId) {
		return roleAssignmentAdminService.listAssignableBrandRoleNames(brandId);
	}

	@PostMapping("/api/v1/brands/{brandId}/users")
	public ResponseEntity<BrandUserResponse> grantBrandUser(@PathVariable UUID brandId,
			@RequestBody GrantBrandUserRequest request, Authentication authentication) {
		var assignment = roleAssignmentAdminService.assignBrandRole(brandId, request.email(), request.roleName(),
				authentication);
		var view = roleAssignmentAdminService.listAssignmentViewsForBrand(brandId).stream()
				.filter(v -> v.roleAssignmentId().equals(assignment.getRoleAssignmentId()))
				.findFirst()
				.orElseThrow();
		return ResponseEntity.status(HttpStatus.CREATED).body(BrandUserResponse.from(view));
	}

	@PostMapping("/api/v1/brands/{brandId}/users/{assignmentId}/revoke")
	public ResponseEntity<Void> revokeBrandUser(@PathVariable UUID brandId, @PathVariable UUID assignmentId) {
		roleAssignmentAdminService.revokeBrandAssignment(brandId, assignmentId);
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(BrandNotFoundException.class)
	public ResponseEntity<String> handleNotFound(BrandNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
	}

	@ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
	public ResponseEntity<String> handleBadRequest(RuntimeException ex) {
		log.warn("Brand portal request rejected: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
	}
}
