package com.example.collectb.identity.service;

import java.util.UUID;

/**
 * Minimal {@code platform_user} projection for the admin dashboard's per-user detail view
 * ({@link RoleAssignmentAdminService#getUserSummary}) - deliberately excludes
 * password/credential-adjacent fields, which live in {@code identity_credential}, not
 * {@code platform_user}, and are never exposed through this admin API anyway.
 */
public record PlatformUserSummary(
		UUID userId,
		String email,
		String displayName,
		String actorType,
		String accountStatus) {
}
