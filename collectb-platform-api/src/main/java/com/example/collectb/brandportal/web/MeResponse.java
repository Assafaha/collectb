package com.example.collectb.brandportal.web;

import java.util.List;
import java.util.UUID;

/** The calling user's own identity/profile summary, for the portal's account/profile view. Roles
 *  and permissions mirror the authenticated {@code Authentication}'s granted authorities (so they
 *  are accurate for both the browser-session and Bearer-token auth flows), not a raw DB dump. */
public record MeResponse(
		UUID userId,
		String email,
		String displayName,
		String actorType,
		List<String> roles,
		List<String> permissions,
		String authStrength,
		boolean catalogMfaRequired) {
}
