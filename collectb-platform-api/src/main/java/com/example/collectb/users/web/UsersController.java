package com.example.collectb.users.web;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.collectb.brand.entity.Brand;
import com.example.collectb.brand.service.BrandNotFoundException;
import com.example.collectb.brand.service.BrandService;
import com.example.collectb.brand.web.BrandResponse;
import com.example.collectb.identity.entity.Role;
import com.example.collectb.identity.entity.RoleAssignment;
import com.example.collectb.identity.security.PlatformUserPrincipal.ScopeGrant;
import com.example.collectb.identity.service.PlatformUserOverviewRow;
import com.example.collectb.identity.service.PlatformUserSummary;
import com.example.collectb.identity.service.RoleAssignmentAdminService;
import com.example.collectb.identity.service.UserRoleAssignmentView;

/**
 * JSON REST API backing the Angular admin dashboard's "Users" screen. A {@code PLATFORM_ADMIN}
 * (holding {@code PLATFORM_IDENTITY:MANAGE}) sees every registered platform user exactly once -
 * including users who only hold a PLATFORM-scoped role (e.g. a fellow admin) or no role grant at
 * all yet, and never duplicated even if they hold several assignments - via
 * {@link RoleAssignmentAdminService#listAllPlatformUsersOverview}; a
 * {@code BRAND_ADMIN} sees one row per user with only assignments in managed Brands summarized.
 * <p>
 * Deliberately reuses {@link RoleAssignmentAdminService}'s existing brand-scoped
 * {@code assignBrandRole}/{@code revokeBrandAssignment} methods for mutations (their
 * {@code @PreAuthorize} rules already accept either a platform admin or the target brand's own
 * {@code BRAND_ADMIN}), so this controller only adds the aggregated *read* side plus a thin
 * grant/revoke pass-through - it does not duplicate any authorization logic.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UsersController {

	private static final Logger log = LoggerFactory.getLogger(UsersController.class);

	private static final String PLATFORM_IDENTITY_MANAGE = "PLATFORM_IDENTITY:MANAGE";

	private final BrandService brandService;
	private final RoleAssignmentAdminService roleAssignmentAdminService;

	public UsersController(BrandService brandService, RoleAssignmentAdminService roleAssignmentAdminService) {
		this.brandService = brandService;
		this.roleAssignmentAdminService = roleAssignmentAdminService;
	}

	@GetMapping
	public List<PlatformUserRowResponse> listUsers(Authentication authentication) {
		if (isPlatformAdmin(authentication)) {
			return roleAssignmentAdminService.listAllPlatformUsersOverview().stream()
					.map(this::toRowResponse)
					.toList();
		}
		List<PlatformUserRowResponse> visibleAssignments = roleAssignmentAdminService
				.listBrandUserAssignmentsForManagedBrands(authentication).stream()
				.<PlatformUserRowResponse>mapMulti((view, consumer) -> {
					try {
						Brand brand = brandService.getBrand(view.scopeId());
						consumer.accept(new PlatformUserRowResponse(view.roleAssignmentId(), view.userId(),
								view.userEmail(), view.userDisplayName(), brand.getBrandId(), brand.getBrandCode(),
								brand.getName(), view.roleName(), view.assignedAt()));
					} catch (BrandNotFoundException ex) {
						// Defensive: a role_assignment can reference a brand_id that no longer
						// resolves to an actual brand row - skip it rather than failing the
						// whole table for one stale grant (mirrors BrandPortalController#myBrands).
						log.warn("Skipping stale brand scope grant for missing brandId={}", view.scopeId());
					}
				})
				.toList();
		return visibleAssignments.stream()
				.collect(Collectors.groupingBy(PlatformUserRowResponse::userId, LinkedHashMap::new,
						Collectors.toList()))
				.values().stream()
				.map(assignments -> {
					PlatformUserRowResponse user = assignments.getFirst();
					List<UserRoleAssignmentView> views = assignments.stream()
							.map(assignment -> new UserRoleAssignmentView(assignment.roleAssignmentId(),
									assignment.roleName(), "BRAND", "BRAND", assignment.brandId(),
									assignment.assignedAt()))
							.toList();
					return toRowResponse(new PlatformUserOverviewRow(user.userId(), user.userEmail(),
							user.userDisplayName(), views));
				})
				.toList();
	}

	/** Converts a {@link PlatformUserOverviewRow} (one user, every active assignment it holds)
	 *  into the API's flat row shape: {@code roleAssignmentId}/{@code brandId} are always
	 *  {@code null} here (there may be zero, one, or several underlying assignments, so there is
	 *  no single one to point at or revoke inline any more - see the "Details" dialog,
	 *  {@link #getUserDetail}, for that), {@code roleName} is a human-readable summary of every
	 *  assignment (e.g. {@code "BRAND_ADMIN @ Acme, PLATFORM_ADMIN"}), and {@code assignedAt} is
	 *  the most recent of them. A user with no active assignment at all still gets a row, with
	 *  {@code roleName}/{@code assignedAt} both {@code null}. */
	private PlatformUserRowResponse toRowResponse(PlatformUserOverviewRow row) {
		if (row.assignments().isEmpty()) {
			return new PlatformUserRowResponse(null, row.userId(), row.userEmail(), row.userDisplayName(), null,
					null, null, null, null);
		}
		String roleSummary = row.assignments().stream()
				.map(this::describeAssignment)
				.collect(Collectors.joining(", "));
		Instant latestAssignedAt = row.assignments().stream()
				.map(UserRoleAssignmentView::assignedAt)
				.max(Comparator.naturalOrder())
				.orElse(null);
		return new PlatformUserRowResponse(null, row.userId(), row.userEmail(), row.userDisplayName(), null, null,
				null, roleSummary, latestAssignedAt);
	}

	/** One assignment's human-readable form for {@link #toRowResponse}'s summary, e.g.
	 *  {@code "BRAND_ADMIN @ Acme"} for a BRAND-scoped role or just {@code "PLATFORM_ADMIN"} for
	 *  any other scope (which never has a meaningful name to append). */
	private String describeAssignment(UserRoleAssignmentView view) {
		if ("BRAND".equals(view.scopeType()) && view.scopeId() != null) {
			try {
				return view.roleName() + " @ " + brandService.getBrand(view.scopeId()).getName();
			} catch (BrandNotFoundException ex) {
				log.warn("Skipping stale brand scope grant for missing brandId={}", view.scopeId());
				return view.roleName() + " @ (deleted brand)";
			}
		}
		return view.roleName();
	}

	/** Brands the caller may grant/revoke users on, for the "Users" screen's brand picker: every
	 *  brand for a platform admin, or only the brand(s) the caller is themselves BRAND_ADMIN of. */
	@GetMapping("/manageable-brands")
	public List<BrandResponse> manageableBrands(Authentication authentication) {
		if (isPlatformAdmin(authentication)) {
			return brandService.listBrands().stream().map(BrandResponse::from).toList();
		}
		List<ScopeGrant> myBrandAdminGrants = roleAssignmentAdminService.listMyBrandAccess(authentication).stream()
				.filter(grant -> "BRAND_ADMIN".equals(grant.roleName()))
				.toList();
		return myBrandAdminGrants.stream()
				.<BrandResponse>mapMulti((grant, consumer) -> {
					try {
						consumer.accept(BrandResponse.from(brandService.getBrand(grant.scopeId())));
					} catch (BrandNotFoundException ex) {
						log.warn("Skipping stale brand scope grant for missing brandId={}", grant.scopeId());
					}
				})
				.toList();
	}

	@GetMapping("/assignable-roles")
	public List<String> listAssignableRoles(@RequestParam UUID brandId) {
		return roleAssignmentAdminService.listAssignableBrandRoleNames(brandId);
	}

	/** The full role catalog (every scope type), for the Users detail view's "add role" picker. */
	@GetMapping("/roles")
	public List<RoleCatalogEntryResponse> listRoleCatalog() {
		return roleAssignmentAdminService.listAllRoles().stream()
				.map(role -> new RoleCatalogEntryResponse(role.getRoleName(), role.getRoleType(),
						role.getDescription(), role.isPrivileged()))
				.toList();
	}

	/** Creates a brand-new platform user (platform admin only) with no role assignment yet - for
	 *  actor types that are not self-registered via {@code /api/v1/auth/register} (COLLECTOR
	 *  only). Returns a one-time temporary password the admin must hand to the new user, since
	 *  there is no invitation/email-verification flow yet. */
	@PostMapping("/accounts")
	public ResponseEntity<CreateUserResponse> createUser(@RequestBody CreateUserRequest request) {
		var result = roleAssignmentAdminService.createUser(request.email(), request.displayName(),
				request.actorType());
		var user = result.user();
		return ResponseEntity.status(HttpStatus.CREATED).body(new CreateUserResponse(user.getUserId(),
				user.getEmail(), user.getDisplayName(), user.getActorType().name(), result.tempPassword()));
	}

	/** Edits an existing user's identity fields (display name, actor type, account status) -
	 *  platform admin only. Does not touch role assignments. */
	@PutMapping("/{userId}")
	public UserDetailResponse updateUser(@PathVariable UUID userId, @RequestBody UpdateUserRequest request,
			Authentication authentication) {
		roleAssignmentAdminService.updateUser(userId, request.displayName(), request.actorType(),
				request.accountStatus());
		return getUserDetail(userId, authentication);
	}

	/** User detail with all scopes for self-view/platform identity administrators, otherwise
	 *  restricted to the caller's managed Brands, including the derived permission set. */
	@GetMapping("/{userId}")
	public UserDetailResponse getUserDetail(@PathVariable UUID userId, Authentication authentication) {
		PlatformUserSummary summary = roleAssignmentAdminService.getUserSummary(userId, authentication);
		List<RoleAssignmentDetailResponse> roleAssignments = roleAssignmentAdminService
				.listAssignmentsForUser(userId, authentication).stream()
				.map(this::toDetailResponse)
				.toList();
		List<String> permissions = roleAssignmentAdminService.listEffectivePermissionKeysForUser(userId, authentication);
		return new UserDetailResponse(summary.userId(), summary.email(), summary.displayName(),
				summary.actorType(), summary.accountStatus(), roleAssignments, permissions);
	}

	/** Grants {@code request.roleName()} to {@code userId} at {@code request.scopeType()}
	 *  (any scope, not just BRAND - see {@code RoleAssignmentAdminService#assignRoleToUser}). */
	@PostMapping("/{userId}/roles")
	public ResponseEntity<RoleAssignmentDetailResponse> assignRole(@PathVariable UUID userId,
			@RequestBody AssignRoleRequest request, Authentication authentication) {
		RoleAssignment assignment = roleAssignmentAdminService.assignRoleToUser(userId, request.roleName(),
				request.scopeType(), request.scopeId(), authentication);
		UserRoleAssignmentView view = roleAssignmentAdminService.listAssignmentsForUser(userId, authentication).stream()
				.filter(v -> v.roleAssignmentId().equals(assignment.getRoleAssignmentId()))
				.findFirst()
				.orElseThrow();
		return ResponseEntity.status(HttpStatus.CREATED).body(toDetailResponse(view));
	}

	/** Revokes one of {@code userId}'s role assignments (any scope). The underlying service
	 *  refuses to let a caller revoke their own assignments, to avoid accidental self-lockout. */
	@PostMapping("/{userId}/roles/{roleAssignmentId}/revoke")
	public ResponseEntity<Void> revokeRole(@PathVariable UUID userId, @PathVariable UUID roleAssignmentId,
			Authentication authentication) {
		roleAssignmentAdminService.revokeUserAssignment(userId, roleAssignmentId, authentication);
		return ResponseEntity.noContent().build();
	}

	/** Converts a {@link UserRoleAssignmentView} into the API's row shape, resolving the brand
	 *  name as {@code scopeLabel} when the assignment is BRAND-scoped. */
	private RoleAssignmentDetailResponse toDetailResponse(UserRoleAssignmentView view) {
		String scopeLabel = null;
		if ("BRAND".equals(view.scopeType()) && view.scopeId() != null) {
			try {
				scopeLabel = brandService.getBrand(view.scopeId()).getName();
			} catch (BrandNotFoundException ex) {
				scopeLabel = "(deleted brand)";
			}
		}
		return new RoleAssignmentDetailResponse(view.roleAssignmentId(), view.roleName(), view.roleType(),
				view.scopeType(), view.scopeId(), scopeLabel, view.assignedAt());
	}

	@PostMapping
	public ResponseEntity<PlatformUserRowResponse> grantUser(@RequestBody GrantUserRequest request,
			Authentication authentication) {
		var assignment = roleAssignmentAdminService.assignBrandRole(request.brandId(), request.email(),
				request.roleName(), authentication);
		Brand brand = brandService.getBrand(request.brandId());
		var view = roleAssignmentAdminService.listAssignmentViewsForBrand(request.brandId()).stream()
				.filter(v -> v.roleAssignmentId().equals(assignment.getRoleAssignmentId()))
				.findFirst()
				.orElseThrow();
		return ResponseEntity.status(HttpStatus.CREATED).body(new PlatformUserRowResponse(view.roleAssignmentId(),
				view.userId(), view.userEmail(), view.userDisplayName(), brand.getBrandId(), brand.getBrandCode(),
				brand.getName(), view.roleName(), view.assignedAt()));
	}

	@PostMapping("/{assignmentId}/revoke")
	public ResponseEntity<Void> revokeUser(@PathVariable UUID assignmentId, @RequestParam UUID brandId) {
		roleAssignmentAdminService.revokeBrandAssignment(brandId, assignmentId);
		return ResponseEntity.noContent().build();
	}

	private static boolean isPlatformAdmin(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.anyMatch(PLATFORM_IDENTITY_MANAGE::equals);
	}

	@ExceptionHandler(BrandNotFoundException.class)
	public ResponseEntity<String> handleNotFound(BrandNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
	}

	@ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
	public ResponseEntity<String> handleBadRequest(RuntimeException ex) {
		log.warn("Users API request rejected: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
	}
}
