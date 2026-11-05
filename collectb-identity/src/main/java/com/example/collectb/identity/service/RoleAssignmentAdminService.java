package com.example.collectb.identity.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.collectb.identity.entity.IdentityCredential;
import com.example.collectb.identity.entity.PlatformUser;
import com.example.collectb.identity.entity.Role;
import com.example.collectb.identity.entity.RoleAssignment;
import com.example.collectb.identity.model.AccountStatus;
import com.example.collectb.identity.model.ActorType;
import com.example.collectb.identity.model.CredentialProvider;
import com.example.collectb.identity.model.CredentialStatus;
import com.example.collectb.identity.model.CredentialType;
import com.example.collectb.identity.repository.IdentityCredentialRepository;
import com.example.collectb.identity.repository.PermissionRepository;
import com.example.collectb.identity.repository.PlatformUserRepository;
import com.example.collectb.identity.repository.RoleAssignmentRepository;
import com.example.collectb.identity.repository.RoleRepository;
import com.example.collectb.identity.security.PlatformUserPrincipal;

/**
 * Admin-facing role-assignment management: grants/revokes scoped roles (e.g. {@code BRAND_ADMIN}
 * for a given {@code brand_id}) to platform users, per design doc section 2.2.2's
 * {@code role_assignment} model ("what can this actor do, and in which scope"). Backs the
 * administrator dashboard's "manage brand users" screen, but is deliberately scope-agnostic
 * (any {@code scopeType}/{@code scopeId} pair) so it also works for future scopes beyond BRAND.
 * <p>
 * All mutating operations require {@code PLATFORM_IDENTITY:MANAGE}, the privileged permission
 * seeded only for {@code PLATFORM_ADMIN} (see {@code insert.sql}).
 */
@Service
public class RoleAssignmentAdminService {

	private static final Logger log = LoggerFactory.getLogger(RoleAssignmentAdminService.class);

	private static final String ACTIVE_STATUS = "ACTIVE";
	private static final String REVOKED_STATUS = "REVOKED";

	private static final SecureRandom TEMP_PASSWORD_RANDOM = new SecureRandom();

	private final PlatformUserRepository platformUserRepository;
	private final RoleRepository roleRepository;
	private final RoleAssignmentRepository roleAssignmentRepository;
	private final PermissionRepository permissionRepository;
	private final IdentityCredentialRepository identityCredentialRepository;
	private final PasswordEncoder passwordEncoder;

	public RoleAssignmentAdminService(PlatformUserRepository platformUserRepository, RoleRepository roleRepository,
			RoleAssignmentRepository roleAssignmentRepository, PermissionRepository permissionRepository,
			IdentityCredentialRepository identityCredentialRepository, PasswordEncoder passwordEncoder) {
		this.platformUserRepository = platformUserRepository;
		this.roleRepository = roleRepository;
		this.roleAssignmentRepository = roleAssignmentRepository;
		this.permissionRepository = permissionRepository;
		this.identityCredentialRepository = identityCredentialRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/** Role names available for a given scope type (e.g. {@code "BRAND"} -&gt;
	 *  {@code BRAND_VIEWER}/{@code BRAND_EDITOR}/{@code BRAND_ADMIN}), for populating a role
	 *  picker in the dashboard. */
	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE')")
	@Transactional(readOnly = true)
	public List<String> listRoleNamesByType(String roleType) {
		return roleRepository.findByRoleType(roleType).stream().map(Role::getRoleName).toList();
	}

	/** The full role catalog (every {@code role_type}, not just {@code BRAND}), for the per-user
	 *  "add role" picker in the Users detail view - unlike {@link #listRoleNamesByType}, the
	 *  caller does not need to already know which scope type they want. */
	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE')")
	@Transactional(readOnly = true)
	public List<Role> listAllRoles() {
		return roleRepository.findAll();
	}

	/** Identity header for self-view, platform identity administrators, or users with an active
	 *  assignment in a Brand the caller manages. */
	@PreAuthorize("isAuthenticated()")
	@Transactional(readOnly = true)
	public PlatformUserSummary getUserSummary(UUID userId, Authentication authentication) {
		visibleAssignments(userId, authentication);
		PlatformUser user = platformUserRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("No platform_user with id " + userId));
		return new PlatformUserSummary(user.getUserId(), user.getEmail(), user.getDisplayName(),
				user.getActorType().name(), user.getAccountStatus().name());
	}

	/**
	 * Creates a brand-new {@code platform_user} directly from the admin dashboard's "Add user"
	 * form, for actor types that are not self-registered via {@code /api/v1/auth/register}
	 * (which only ever creates {@code COLLECTOR}s) - e.g. a {@code SUPPORT_USER} or a second
	 * {@code PLATFORM_ADMIN}. Since there is no invitation/email-verification flow yet, a random
	 * temporary password is generated and returned once in {@link CreatedUserResult} so the
	 * admin can hand it to the new user out-of-band; the account is created {@code ACTIVE} (not
	 * {@code PENDING_VERIFICATION}) so it can log in with that password immediately. The new
	 * user starts with no role assignments - grant one afterwards via {@link #assignRoleToUser}.
	 */
	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE')")
	@Transactional
	public CreatedUserResult createUser(String email, String displayName, String actorTypeName) {
		String normalizedEmail = normalizeEmail(email);
		if (displayName == null || displayName.isBlank()) {
			throw new IllegalArgumentException("displayName is required");
		}
		ActorType actorType = parseActorType(actorTypeName);
		if (platformUserRepository.findByEmail(normalizedEmail).isPresent()) {
			throw new IllegalStateException("A user with email " + normalizedEmail + " already exists");
		}

		PlatformUser user = new PlatformUser(normalizedEmail, displayName, actorType, AccountStatus.ACTIVE);
		PlatformUser savedUser = platformUserRepository.save(user);

		String tempPassword = generateTempPassword();
		IdentityCredential credential = new IdentityCredential(savedUser.getUserId(), CredentialProvider.LOCAL,
				CredentialType.PASSWORD, CredentialStatus.ACTIVE);
		credential.setPasswordHash(passwordEncoder.encode(tempPassword));
		credential.setPasswordAlgorithm("argon2id");
		credential.setMfaEnabled(false);
		identityCredentialRepository.save(credential);

		log.info("Created platform_user email={} actorType={}", normalizedEmail, actorType);
		return new CreatedUserResult(savedUser, tempPassword);
	}

	/** Edits an existing user's own identity fields (display name, actor type, account status) -
	 *  for the admin dashboard's "Edit user" dialog. Does not touch role assignments; use
	 *  {@link #assignRoleToUser}/{@link #revokeUserAssignment} for those. */
	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE')")
	@Transactional
	public PlatformUser updateUser(UUID userId, String displayName, String actorTypeName, String accountStatusName) {
		PlatformUser user = platformUserRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("No platform_user with id " + userId));
		if (displayName == null || displayName.isBlank()) {
			throw new IllegalArgumentException("displayName is required");
		}
		user.setDisplayName(displayName);
		user.setActorType(parseActorType(actorTypeName));
		user.setAccountStatus(parseAccountStatus(accountStatusName));
		log.info("Updated platform_user id={} actorType={} accountStatus={}", userId, user.getActorType(),
				user.getAccountStatus());
		return platformUserRepository.save(user);
	}

	private static ActorType parseActorType(String actorTypeName) {
		try {
			return ActorType.valueOf(actorTypeName);
		} catch (IllegalArgumentException | NullPointerException ex) {
			throw new IllegalArgumentException("Unknown actorType " + actorTypeName);
		}
	}

	private static AccountStatus parseAccountStatus(String accountStatusName) {
		try {
			return AccountStatus.valueOf(accountStatusName);
		} catch (IllegalArgumentException | NullPointerException ex) {
			throw new IllegalArgumentException("Unknown accountStatus " + accountStatusName);
		}
	}

	private static String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("email is required");
		}
		String normalized = email.trim();
		if (!normalized.contains("@")) {
			throw new IllegalArgumentException("email must be valid");
		}
		return normalized.toLowerCase(Locale.ROOT);
	}

	/** Generates a random URL-safe temporary password for {@link #createUser}. */
	private static String generateTempPassword() {
		byte[] bytes = new byte[18];
		TEMP_PASSWORD_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** Result of {@link #createUser}: the new user plus the one-time temporary password. */
	public record CreatedUserResult(PlatformUser user, String tempPassword) {
	}

	/**
	 * Active role assignments visible to the caller: all scopes for self-view/platform identity
	 * administrators, otherwise only managed Brand scopes. Unlike
	 * {@link #listAllPlatformUsersOverview} (one row per user, summarized for the flat "Users"
	 * table), this is the detailed per-assignment list for one specific user at once.
	 */
	@PreAuthorize("isAuthenticated()")
	@Transactional(readOnly = true)
	public List<UserRoleAssignmentView> listAssignmentsForUser(UUID userId, Authentication authentication) {
		List<RoleAssignment> assignments = visibleAssignments(userId, authentication);
		if (assignments.isEmpty()) {
			return List.of();
		}
		Map<UUID, Role> rolesById = roleRepository
				.findByRoleIdIn(assignments.stream().map(RoleAssignment::getRoleId).distinct().toList())
				.stream()
				.collect(Collectors.toMap(Role::getRoleId, r -> r));
		return assignments.stream()
				.map(assignment -> {
					Role role = rolesById.get(assignment.getRoleId());
					return new UserRoleAssignmentView(assignment.getRoleAssignmentId(),
							role != null ? role.getRoleName() : "(unknown role)",
							role != null ? role.getRoleType() : "(unknown)", assignment.getScopeType(),
							assignment.getScopeId(), assignment.getAssignedAt());
				})
				.toList();
	}

	/**
	 * The distinct set of permission keys granted (via each held role's {@code role_permission}
	 * rows) by a single user's active role assignments - the Users detail view's read-only
	 * "effective permissions" list. There is no per-user permission override in this schema;
	 * permissions always flow through roles (see design doc section 2.2.2), so this is simply the
	 * union of the permissions from assignments visible to this caller.
	 */
	@PreAuthorize("isAuthenticated()")
	@Transactional(readOnly = true)
	public List<String> listEffectivePermissionKeysForUser(UUID userId, Authentication authentication) {
		List<UUID> roleIds = visibleAssignments(userId, authentication).stream()
				.map(RoleAssignment::getRoleId)
				.distinct()
				.toList();
		if (roleIds.isEmpty()) {
			return List.of();
		}
		return permissionRepository.findPermissionKeysByRoleIdIn(roleIds).stream().sorted().toList();
	}

	private List<RoleAssignment> visibleAssignments(UUID userId, Authentication authentication) {
		List<RoleAssignment> assignments = roleAssignmentRepository.findByUserIdAndStatus(userId, ACTIVE_STATUS);
		if (authentication.getAuthorities().stream()
				.anyMatch(authority -> "PLATFORM_IDENTITY:MANAGE".equals(authority.getAuthority()))
				|| userId.equals(resolveActingUserId(authentication))) {
			return assignments;
		}
		List<UUID> managedBrandIds = managedBrandIds(authentication);
		List<RoleAssignment> visible = assignments.stream()
				.filter(assignment -> "BRAND".equals(assignment.getScopeType())
						&& managedBrandIds.contains(assignment.getScopeId()))
				.toList();
		if (visible.isEmpty()) {
			throw new AccessDeniedException("User details are restricted to your account and managed brands");
		}
		return visible;
	}

	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE')")
	@Transactional(readOnly = true)
	public List<ScopedRoleAssignmentView> listAssignmentViewsForScope(String scopeType, UUID scopeId) {
		return assignmentViewsForScope(scopeType, scopeId);
	}

	/**
	 * Every registered platform user exactly once, each carrying the full list of whatever active
	 * role assignment(s) they hold across every scope - for the platform admin's full "Users"
	 * table. Unlike filtering on only BRAND-scoped role assignments (which would silently hide
	 * users who hold e.g. only a PLATFORM-scoped role, like a fellow PLATFORM_ADMIN, or no role
	 * grant at all yet), this starts from {@code platform_user} itself so nobody is missing - and
	 * unlike an earlier version of this method, a user with several active assignments (e.g.
	 * {@code BRAND_ADMIN} on two brands) is *not* duplicated into one row per assignment; the API
	 * layer summarizes the whole list into a single display row instead.
	 */
	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE')")
	@Transactional(readOnly = true)
	public List<PlatformUserOverviewRow> listAllPlatformUsersOverview() {
		Map<UUID, List<RoleAssignment>> assignmentsByUser = roleAssignmentRepository
				.findByStatus(ACTIVE_STATUS).stream()
				.collect(Collectors.groupingBy(RoleAssignment::getUserId));
		Map<UUID, Role> rolesById = roleRepository.findAll().stream()
				.collect(Collectors.toMap(Role::getRoleId, r -> r));

		List<PlatformUserOverviewRow> rows = new java.util.ArrayList<>();
		for (PlatformUser user : platformUserRepository.findAll()) {
			List<RoleAssignment> assignments = assignmentsByUser.getOrDefault(user.getUserId(), List.of());
			List<UserRoleAssignmentView> views = assignments.stream()
					.map(assignment -> {
						Role role = rolesById.get(assignment.getRoleId());
						return new UserRoleAssignmentView(assignment.getRoleAssignmentId(),
								role != null ? role.getRoleName() : "(unknown role)",
								role != null ? role.getRoleType() : "(unknown)", assignment.getScopeType(),
								assignment.getScopeId(), assignment.getAssignedAt());
					})
					.sorted(java.util.Comparator.comparing(UserRoleAssignmentView::assignedAt))
					.toList();
			rows.add(new PlatformUserOverviewRow(user.getUserId(), user.getEmail(), user.getDisplayName(), views));
		}
		return rows;
	}

	/**
	 * All active BRAND-scoped role assignments across every brand the caller is themselves a
	 * {@code BRAND_ADMIN} of, for the same "users" table when the caller is a brand admin rather
	 * than a platform admin (may manage more than one brand, so this is a union across all of
	 * them, not just a single {@code brandId}).
	 */
	@PreAuthorize("isAuthenticated()")
	@Transactional(readOnly = true)
	public List<ScopedRoleAssignmentView> listBrandUserAssignmentsForManagedBrands(Authentication authentication) {
		List<UUID> managedBrandIds = managedBrandIds(authentication);
		if (managedBrandIds.isEmpty()) {
			return List.of();
		}
		return toViews(roleAssignmentRepository.findByScopeTypeAndScopeIdInAndStatus("BRAND", managedBrandIds,
				ACTIVE_STATUS));
	}

	private List<UUID> managedBrandIds(Authentication authentication) {
		UUID userId = resolveActingUserId(authentication);
		Role brandAdminRole = roleRepository.findByRoleName("BRAND_ADMIN").orElse(null);
		if (brandAdminRole == null) {
			return List.of();
		}
		return roleAssignmentRepository
				.findByUserIdAndScopeTypeAndStatus(userId, "BRAND", ACTIVE_STATUS).stream()
				.filter(assignment -> brandAdminRole.getRoleId().equals(assignment.getRoleId()))
				.map(RoleAssignment::getScopeId)
				.distinct()
				.toList();
	}

	/**
	 * Brand self-service variant of {@link #listAssignmentViewsForScope}: usable by a {@code
	 * BRAND_ADMIN} for their own brand (checked via {@code brandScopeAuthorizationService}), in
	 * addition to a platform admin. Backs the Angular brand portal's "users with access" screen.
	 */
	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE') "
			+ "or @brandScopeAuthorizationService.hasBrandRole(authentication, #brandId, 'BRAND_ADMIN')")
	@Transactional(readOnly = true)
	public List<ScopedRoleAssignmentView> listAssignmentViewsForBrand(UUID brandId) {
		return assignmentViewsForScope("BRAND", brandId);
	}

	/** Brand-scoped role names assignable from the brand portal's role picker. */
	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE') "
			+ "or @brandScopeAuthorizationService.hasBrandRole(authentication, #brandId, 'BRAND_ADMIN')")
	@Transactional(readOnly = true)
	public List<String> listAssignableBrandRoleNames(UUID brandId) {
		return roleRepository.findByRoleType("BRAND").stream().map(Role::getRoleName).toList();
	}

	private List<ScopedRoleAssignmentView> assignmentViewsForScope(String scopeType, UUID scopeId) {
		return toViews(roleAssignmentRepository.findByScopeTypeAndScopeIdAndStatus(scopeType, scopeId, ACTIVE_STATUS));
	}

	private List<ScopedRoleAssignmentView> toViews(List<RoleAssignment> assignments) {
		if (assignments.isEmpty()) {
			return List.of();
		}

		Map<UUID, PlatformUser> usersById = platformUserRepository
				.findAllById(assignments.stream().map(RoleAssignment::getUserId).distinct().toList())
				.stream()
				.collect(Collectors.toMap(PlatformUser::getUserId, u -> u));
		Map<UUID, Role> rolesById = roleRepository
				.findByRoleIdIn(assignments.stream().map(RoleAssignment::getRoleId).distinct().toList())
				.stream()
				.collect(Collectors.toMap(Role::getRoleId, r -> r));

		return assignments.stream()
				.map(assignment -> {
					PlatformUser user = usersById.get(assignment.getUserId());
					Role role = rolesById.get(assignment.getRoleId());
					return new ScopedRoleAssignmentView(
							assignment.getRoleAssignmentId(),
							assignment.getUserId(),
							user != null ? user.getEmail() : "(unknown user)",
							user != null ? user.getDisplayName() : "",
							role != null ? role.getRoleName() : "(unknown role)",
							assignment.getAssignedAt(),
							assignment.getScopeId());
				})
				.toList();
	}

	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE')")
	@Transactional
	public RoleAssignment assignRole(String userEmail, String roleName, String scopeType, UUID scopeId,
			Authentication authentication) {
		PlatformUser user = platformUserRepository.findByEmail(userEmail)
				.orElseThrow(() -> new IllegalArgumentException("No platform_user with email " + userEmail));
		return doAssignRole(user, roleName, scopeType, scopeId, authentication);
	}

	/**
	 * Generic variant of {@link #assignRole} addressed by {@code userId} rather than email - for
	 * the admin dashboard's per-user "roles and permissions" detail view, where the user is
	 * already resolved (as opposed to the brand users screens, which grant by typed-in email).
	 * Unlike {@link #assignBrandRole}, this accepts *any* {@code roleType}/{@code scopeType}
	 * (PLATFORM, BRAND, COLLECTOR, SUPPORT, COMPLIANCE, SERVICE) - {@code doAssignRole} still
	 * enforces that {@code roleName}'s own {@code role_type} matches the requested
	 * {@code scopeType}, and that a BRAND scope always carries a {@code scopeId} while every
	 * other scope type never does.
	 */
	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE')")
	@Transactional
	public RoleAssignment assignRoleToUser(UUID userId, String roleName, String scopeType, UUID scopeId,
			Authentication authentication) {
		PlatformUser user = platformUserRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("No platform_user with id " + userId));
		return doAssignRole(user, roleName, scopeType, scopeId, authentication);
	}

	/**
	 * Brand self-service variant of {@link #assignRole}: usable by a {@code BRAND_ADMIN} for
	 * their own brand. Restricted to {@code BRAND}-type roles (i.e. {@code BRAND_VIEWER}/{@code
	 * BRAND_EDITOR}/{@code BRAND_ADMIN}) so a brand admin can never use this path to grant
	 * themselves or anyone else a platform-wide role.
	 */
	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE') "
			+ "or @brandScopeAuthorizationService.hasBrandRole(authentication, #brandId, 'BRAND_ADMIN')")
	@Transactional
	public RoleAssignment assignBrandRole(UUID brandId, String userEmail, String roleName,
			Authentication authentication) {
		PlatformUser user = platformUserRepository.findByEmail(userEmail)
				.orElseThrow(() -> new IllegalArgumentException("No platform_user with email " + userEmail));
		return doAssignRole(user, roleName, "BRAND", brandId, authentication);
	}

	/**
	 * Shared grant logic for every {@code assignRole*} entry point above: validates that
	 * {@code roleName} actually exists and belongs to {@code scopeType} (so e.g. a BRAND-typed
	 * role can never be granted at PLATFORM scope or vice versa), that a BRAND scope always
	 * carries a {@code scopeId} while every other scope type never does, and that the user does
	 * not already hold this exact role/scope combination before inserting the new
	 * {@code role_assignment} row.
	 */
	private RoleAssignment doAssignRole(PlatformUser user, String roleName, String scopeType, UUID scopeId,
			Authentication authentication) {
		Role role = roleRepository.findByRoleName(roleName)
				.orElseThrow(() -> new IllegalArgumentException("No role named " + roleName));
		if (!role.getRoleType().equals(scopeType)) {
			throw new IllegalArgumentException(
					roleName + " is a " + role.getRoleType() + "-scoped role, not " + scopeType);
		}
		if ("BRAND".equals(scopeType) && scopeId == null) {
			throw new IllegalArgumentException("BRAND-scoped roles require a brandId");
		}
		if (!"BRAND".equals(scopeType) && scopeId != null) {
			throw new IllegalArgumentException(scopeType + "-scoped roles do not take a scopeId");
		}

		boolean alreadyActive = roleAssignmentRepository
				.findByUserIdAndRoleIdAndScopeTypeAndScopeId(user.getUserId(), role.getRoleId(), scopeType, scopeId)
				.filter(existing -> ACTIVE_STATUS.equals(existing.getStatus()))
				.isPresent();
		if (alreadyActive) {
			throw new IllegalStateException(user.getEmail() + " already has " + roleName + " in this scope");
		}

		UUID assignedBy = resolveActingUserId(authentication);
		RoleAssignment assignment = new RoleAssignment(user.getUserId(), role.getRoleId(), scopeType, scopeId,
				ACTIVE_STATUS, assignedBy);
		RoleAssignment saved = roleAssignmentRepository.save(assignment);
		log.info("Granted role={} to userEmail={} scopeType={} scopeId={} assignedBy={}", roleName, user.getEmail(),
				scopeType, scopeId, assignedBy);
		return saved;
	}

	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE')")
	@Transactional
	public void revokeAssignment(UUID roleAssignmentId) {
		doRevoke(roleAssignmentId);
	}

	/**
	 * Generic variant of {@link #revokeAssignment} for the admin dashboard's per-user detail
	 * view: verifies the assignment actually belongs to {@code userId} before revoking (defense
	 * in depth against a UI bug passing a mismatched id), and - since this can revoke a
	 * PLATFORM-scoped role just as easily as a BRAND one - refuses to let a caller revoke their
	 * *own* role assignments at all, to make it impossible to accidentally lock yourself out of
	 * the admin dashboard. Revoking another platform admin's access is still allowed (by design,
	 * for offboarding), just never your own.
	 */
	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE')")
	@Transactional
	public void revokeUserAssignment(UUID userId, UUID roleAssignmentId, Authentication authentication) {
		if (userId.equals(resolveActingUserId(authentication))) {
			throw new IllegalStateException(
					"You cannot revoke your own role assignments - ask another platform admin to do it");
		}
		RoleAssignment assignment = roleAssignmentRepository.findById(roleAssignmentId)
				.orElseThrow(() -> new IllegalArgumentException("No role_assignment for id " + roleAssignmentId));
		if (!userId.equals(assignment.getUserId())) {
			throw new IllegalArgumentException(
					"No role_assignment for id " + roleAssignmentId + " for user " + userId);
		}
		doRevoke(roleAssignmentId);
	}

	/**
	 * Brand self-service variant of {@link #revokeAssignment}: usable by a {@code BRAND_ADMIN}
	 * for their own brand. Verifies the assignment actually belongs to {@code brandId} before
	 * revoking, so a brand admin cannot revoke an assignment in a different brand simply by
	 * guessing/enumerating {@code roleAssignmentId} values.
	 */
	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE') "
			+ "or @brandScopeAuthorizationService.hasBrandRole(authentication, #brandId, 'BRAND_ADMIN')")
	@Transactional
	public void revokeBrandAssignment(UUID brandId, UUID roleAssignmentId) {
		RoleAssignment assignment = roleAssignmentRepository.findById(roleAssignmentId)
				.orElseThrow(() -> new IllegalArgumentException("No role_assignment for id " + roleAssignmentId));
		if (!"BRAND".equals(assignment.getScopeType()) || !brandId.equals(assignment.getScopeId())) {
			throw new IllegalArgumentException("No role_assignment for id " + roleAssignmentId + " in brand " + brandId);
		}
		doRevoke(roleAssignmentId);
	}

	private void doRevoke(UUID roleAssignmentId) {
		RoleAssignment assignment = roleAssignmentRepository.findById(roleAssignmentId)
				.orElseThrow(() -> new IllegalArgumentException("No role_assignment for id " + roleAssignmentId));
		assignment.setStatus(REVOKED_STATUS);
		roleAssignmentRepository.save(assignment);
		log.info("Revoked role_assignment_id={} userId={} scopeType={} scopeId={}", roleAssignmentId,
				assignment.getUserId(), assignment.getScopeType(), assignment.getScopeId());
	}

	/**
	 * Lists the calling user's own active BRAND-scoped role grants (brand id + role name), for
	 * the Angular brand portal's "which brands can I access" landing screen. Any authenticated
	 * user may call this for themselves - it exposes no other user's data.
	 */
	@PreAuthorize("isAuthenticated()")
	@Transactional(readOnly = true)
	public List<PlatformUserPrincipal.ScopeGrant> listMyBrandAccess(Authentication authentication) {
		UUID userId = resolveActingUserId(authentication);
		return roleAssignmentRepository.findByUserIdAndScopeTypeAndStatus(userId, "BRAND", ACTIVE_STATUS).stream()
				.map(assignment -> {
					Role role = roleRepository.findById(assignment.getRoleId()).orElse(null);
					return new PlatformUserPrincipal.ScopeGrant(role != null ? role.getRoleName() : "(unknown role)",
							"BRAND", assignment.getScopeId());
				})
				.toList();
	}

	/**
	 * Resolves the acting admin's {@code user_id}, handling both the browser session flow (
	 * {@link PlatformUserPrincipal}, used by the Thymeleaf admin dashboard) and Bearer-token
	 * requests ({@link JwtAuthenticationToken}, in case this service is later exposed as a JSON
	 * API), falling back to parsing {@link Authentication#getName()} as a UUID.
	 */
	public static UUID resolveActingUserId(Authentication authentication) {
		if (authentication.getPrincipal() instanceof PlatformUserPrincipal principal) {
			return principal.getUserId();
		}
		if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
			String userId = jwtAuthentication.getToken().getClaimAsString("user_id");
			if (userId != null) {
				return UUID.fromString(userId);
			}
		}
		return UUID.fromString(authentication.getName());
	}
}
