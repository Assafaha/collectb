package com.example.collectb.catalog.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.slf4j.LoggerFactory;

@Component("catalogStepUpAuthorizationService")
public class CatalogStepUpAuthorizationService {

	private final boolean mfaRequired;

	public CatalogStepUpAuthorizationService(Environment environment) {
		boolean bypass = environment.getProperty("collectb.catalog.allow-development-mfa-bypass", Boolean.class, false);
		if (bypass && (!environment.acceptsProfiles(Profiles.of("development"))
				|| environment.acceptsProfiles(Profiles.of("prod", "production")))) {
			throw new IllegalStateException("Catalog MFA bypass is allowed only in the development profile, never Production");
		}
		mfaRequired = !bypass;
		if (bypass) {
			LoggerFactory.getLogger(CatalogStepUpAuthorizationService.class)
					.warn("Catalog MFA bypass enabled for development. MFA is mandatory for Production.");
		}
	}

	public boolean isMfaRequired() {
		return mfaRequired;
	}

	public boolean hasRecentStepUp(Authentication authentication) {
		if (!mfaRequired) {
			return authentication != null && authentication.isAuthenticated()
					&& authentication instanceof JwtAuthenticationToken;
		}
		if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
			return false;
		}
		String strength = jwtAuthentication.getToken().getClaimAsString("auth_strength");
		return "mfa".equalsIgnoreCase(strength) || "mfa_pwd".equalsIgnoreCase(strength);
	}
}
