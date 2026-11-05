package com.example.collectb.identity.web;

import java.util.Set;
import java.util.UUID;

/** Response body for {@code POST /api/v1/auth/register} and {@code POST /api/v1/auth/login}. */
public record AuthResponse(
		UUID userId,
		String email,
		String displayName,
		String actorType,
		Set<String> roles,
		String accessToken,
		String tokenType) {

	public static AuthResponse ofToken(UUID userId, String email, String displayName, String actorType,
			Set<String> roles, String accessToken) {
		return new AuthResponse(userId, email, displayName, actorType, roles, accessToken, "Bearer");
	}
}
