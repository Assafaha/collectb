package com.example.collectb.identity.config;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

import com.example.collectb.identity.security.PlatformUserPrincipal;

/**
 * Populates custom JWT claims for both access tokens and OIDC ID tokens, per design doc
 * section 2.2.3 "Token and Session Design": "Include stable claims for user_id, actor_type,
 * role names, permission keys, scope_type, scope_id, authentication strength, and correlation
 * ID." and "Do not expose credential details, password hashes, MFA secrets, ... in tokens."
 * (accordingly, only the {@link PlatformUserPrincipal}'s derived claims are added here - never
 * the password hash itself, which the principal only carries internally for authentication).
 */
@Configuration
public class JwtClaimsCustomizerConfig {

	@Bean
	public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
		return context -> {
			if (!(context.getPrincipal().getPrincipal() instanceof PlatformUserPrincipal user)) {
				return;
			}
			boolean isAccessOrIdToken = "ACCESS_TOKEN".equalsIgnoreCase(context.getTokenType().getValue())
					|| OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue());
			if (!isAccessOrIdToken) {
				return;
			}

			List<Map<String, Object>> scopeGrants = user.getScopeGrants().stream()
					.map(grant -> {
						UUID scopeId = grant.scopeId();
						return Map.<String, Object>of(
								"role", grant.roleName(),
								"scope_type", grant.scopeType(),
								"scope_id", scopeId != null ? scopeId.toString() : "");
					})
					.toList();

			context.getClaims()
					.claim("user_id", user.getUserId().toString())
					.claim("actor_type", user.getActorType().name())
					.claim("roles", user.getRoleNames())
					.claim("permissions", user.getPermissionKeys())
					.claim("scopes", scopeGrants)
					// Authentication strength reflects the doc's step-up-auth requirement (2.2.3/
					// 2.2.4): "pwd" means only the password factor was verified. mfa_required
					// signals the resource server that a stronger step-up auth SHOULD be demanded
					// before privileged operations, but no actual MFA challenge/verification is
					// implemented yet - see the TODO on IdentityCredential.mfaFactorType.
					.claim("auth_strength", "pwd")
					.claim("mfa_required", user.isMfaEnabled())
					.claim("correlation_id", UUID.randomUUID().toString());
		};
	}
}
