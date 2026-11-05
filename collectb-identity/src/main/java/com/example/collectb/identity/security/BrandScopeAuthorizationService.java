package com.example.collectb.identity.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Answers "does this caller hold one of the given roles for this specific BRAND scope",
 * regardless of whether the caller authenticated via the browser session ({@link
 * PlatformUserPrincipal}, which already carries its {@code role_assignment} scope grants) or a
 * Bearer JWT ({@link JwtAuthenticationToken}, whose {@code scopes} claim - populated by {@code
 * JwtClaimsCustomizerConfig} - carries the same information).
 * <p>
 * Exposed as the bean {@code brandScopeAuthorizationService} so it can be referenced from
 * {@code @PreAuthorize} SpEL expressions in other modules (e.g. {@code collectb-brand}) purely
 * by bean name, without those modules needing a compile-time dependency on {@code
 * collectb-identity}.
 * <p>
 * This is what lets a {@code BRAND_ADMIN} self-manage only their own brand's users, while a
 * {@code PLATFORM_ADMIN} (who holds the flat {@code PLATFORM_IDENTITY:MANAGE} permission) can
 * still manage any brand - the two checks are combined with {@code or} at each call site.
 */
@Component("brandScopeAuthorizationService")
public class BrandScopeAuthorizationService {

	private static final String BRAND_SCOPE_TYPE = "BRAND";

	/** True if the caller holds any of {@code roleNames} scoped to {@code brandId}. */
	public boolean hasBrandRole(Authentication authentication, UUID brandId, String... roleNames) {
		if (authentication == null || brandId == null || roleNames.length == 0) {
			return false;
		}
		Set<String> allowedRoleNames = Set.of(roleNames);
		return brandScopeGrants(authentication).stream()
				.anyMatch(grant -> brandId.equals(grant.scopeId()) && allowedRoleNames.contains(grant.roleName()));
	}

	private List<PlatformUserPrincipal.ScopeGrant> brandScopeGrants(Authentication authentication) {
		if (authentication.getPrincipal() instanceof PlatformUserPrincipal principal) {
			return principal.getScopeGrants().stream()
					.filter(grant -> BRAND_SCOPE_TYPE.equals(grant.scopeType()))
					.toList();
		}
		if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
			return scopeGrantsFromJwtClaim(jwtAuthentication);
		}
		return List.of();
	}

	@SuppressWarnings("unchecked")
	private List<PlatformUserPrincipal.ScopeGrant> scopeGrantsFromJwtClaim(JwtAuthenticationToken jwtAuthentication) {
		Object rawScopes = jwtAuthentication.getToken().getClaim("scopes");
		if (!(rawScopes instanceof List<?> scopes)) {
			return List.of();
		}
		List<PlatformUserPrincipal.ScopeGrant> grants = new ArrayList<>();
		for (Object entry : scopes) {
			if (!(entry instanceof Map<?, ?> scopeMap)) {
				continue;
			}
			Map<String, Object> scope = (Map<String, Object>) scopeMap;
			if (!BRAND_SCOPE_TYPE.equals(scope.get("scope_type"))) {
				continue;
			}
			Object roleName = scope.get("role");
			Object scopeId = scope.get("scope_id");
			if (roleName instanceof String role && scopeId instanceof String scopeIdString && !scopeIdString.isBlank()) {
				grants.add(new PlatformUserPrincipal.ScopeGrant(role, BRAND_SCOPE_TYPE, UUID.fromString(scopeIdString)));
			}
		}
		return grants;
	}
}
