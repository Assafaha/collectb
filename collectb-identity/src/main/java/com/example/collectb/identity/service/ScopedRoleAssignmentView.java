package com.example.collectb.identity.service;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model joining a {@code role_assignment} row with the display data an admin UI/API needs
 * (user email/display name, role name) without exposing raw entities or requiring callers to
 * join {@code platform_user}/{@code role} themselves. Returned by
 * {@link RoleAssignmentAdminService#listAssignmentViewsForScope}.
 */
public record ScopedRoleAssignmentView(
		UUID roleAssignmentId,
		UUID userId,
		String userEmail,
		String userDisplayName,
		String roleName,
		Instant assignedAt,
		UUID scopeId) {
}
