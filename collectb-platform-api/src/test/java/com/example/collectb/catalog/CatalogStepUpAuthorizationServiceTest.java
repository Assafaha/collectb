package com.example.collectb.catalog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.example.collectb.catalog.security.CatalogStepUpAuthorizationService;

class CatalogStepUpAuthorizationServiceTest {
	private JwtAuthenticationToken token(String strength) {
		return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "RS256")
				.subject("test").claim("auth_strength", strength).build(), List.of());
	}

	@Test
	void enforcementDefaultsToMfaAndDevelopmentBypassAllowsPasswordTokens() {
		var strict = new CatalogStepUpAuthorizationService(new MockEnvironment());
		assertTrue(strict.isMfaRequired());
		assertFalse(strict.hasRecentStepUp(token("pwd")));
		assertTrue(strict.hasRecentStepUp(token("mfa")));
		var environment = new MockEnvironment().withProperty("collectb.catalog.allow-development-mfa-bypass", "true");
		environment.setActiveProfiles("development");
		var development = new CatalogStepUpAuthorizationService(environment);
		assertFalse(development.isMfaRequired());
		assertTrue(development.hasRecentStepUp(token("pwd")));
		assertFalse(development.hasRecentStepUp(null));
	}

	@Test
	void bypassFailsStartupOutsideDevelopmentOrWithProductionProfile() {
		for (String[] profiles : List.of(new String[] {}, new String[] {"production"},
				new String[] {"development", "production"}, new String[] {"development", "prod"})) {
			var environment = new MockEnvironment().withProperty("collectb.catalog.allow-development-mfa-bypass", "true");
			environment.setActiveProfiles(profiles);
			assertThrows(IllegalStateException.class, () -> new CatalogStepUpAuthorizationService(environment));
		}
	}
}
