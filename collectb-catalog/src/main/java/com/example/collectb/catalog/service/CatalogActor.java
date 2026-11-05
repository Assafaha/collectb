package com.example.collectb.catalog.service;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class CatalogActor {

	private CatalogActor() {
	}

	public static UUID userId(Authentication authentication) {
		if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
			String userId = jwtAuthentication.getToken().getClaimAsString("user_id");
			if (userId != null && !userId.isBlank()) {
				return UUID.fromString(userId);
			}
		}
		try {
			return UUID.fromString(authentication.getName());
		} catch (IllegalArgumentException ex) {
			throw new IllegalStateException("Unable to resolve acting user id from authentication", ex);
		}
	}
}
