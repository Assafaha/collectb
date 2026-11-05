package com.example.collectb.identity.security;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.collectb.identity.model.AccountStatus;
import com.example.collectb.identity.model.ActorType;

/**
 * {@link UserDetails} implementation backed by a {@code platform_user} row, carrying the extra
 * identity context (user id, actor type, granted role names, granted permission keys, per-scope
 * role grants, MFA status) needed to populate custom JWT claims per design doc section 2.2.3
 * ("Include stable claims for user_id, actor_type, role names, permission keys, scope_type,
 * scope_id, authentication strength, and correlation ID.").
 * <p>
 * Authorities combine both role names (prefixed {@code ROLE_}, the Spring Security convention
 * for {@code hasRole(...)} checks) and raw permission keys (e.g. {@code COLLECTION:UPDATE},
 * usable with {@code hasAuthority(...)}), so method security can be expressed either way.
 */
public class PlatformUserPrincipal implements UserDetails {

	/**
	 * One {@code role_assignment} row's scope, e.g. {@code (BRAND_ADMIN, BRAND, <brand_id>)}.
	 * A user may hold several of these at once (design doc 2.2.2: "one user may hold several
	 * scoped roles, such as a collector account plus brand access for one or more brands"),
	 * so resource servers must check the specific scope grant relevant to the resource being
	 * accessed, not just the flat role/permission set.
	 */
	public record ScopeGrant(String roleName, String scopeType, UUID scopeId) {
	}

	private final UUID userId;
	private final String email;
	private final String passwordHash;
	private final ActorType actorType;
	private final AccountStatus accountStatus;
	private final Set<String> roleNames;
	private final Set<String> permissionKeys;
	private final List<ScopeGrant> scopeGrants;
	/** True if the user's active PASSWORD credential has MFA enabled. Currently a stub signal
	 *  only (surfaced as an "mfa_required" JWT claim) - actual TOTP/SMS challenge verification
	 *  is not yet implemented; see the TODO on JwtClaimsCustomizerConfig. */
	private final boolean mfaEnabled;

	public PlatformUserPrincipal(UUID userId, String email, String passwordHash, ActorType actorType,
			AccountStatus accountStatus, Set<String> roleNames, Set<String> permissionKeys,
			List<ScopeGrant> scopeGrants, boolean mfaEnabled) {
		this.userId = userId;
		this.email = email;
		this.passwordHash = passwordHash;
		this.actorType = actorType;
		this.accountStatus = accountStatus;
		this.roleNames = roleNames;
		this.permissionKeys = permissionKeys;
		this.scopeGrants = scopeGrants;
		this.mfaEnabled = mfaEnabled;
	}

	public UUID getUserId() {
		return userId;
	}

	public ActorType getActorType() {
		return actorType;
	}

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}

	public Set<String> getRoleNames() {
		return roleNames;
	}

	public Set<String> getPermissionKeys() {
		return permissionKeys;
	}

	public List<ScopeGrant> getScopeGrants() {
		return scopeGrants;
	}

	public boolean isMfaEnabled() {
		return mfaEnabled;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Stream.concat(roleNames.stream().map(name -> "ROLE_" + name), permissionKeys.stream())
				.map(SimpleGrantedAuthority::new)
				.toList();
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return accountStatus != AccountStatus.DELETED;
	}

	@Override
	public boolean isAccountNonLocked() {
		return accountStatus != AccountStatus.LOCKED;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return accountStatus == AccountStatus.ACTIVE;
	}
}
