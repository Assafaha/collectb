package com.example.collectb.identity.service;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model for a single active {@code role_assignment} row belonging to one user, across *any*
 * scope (unlike {@link ScopedRoleAssignmentView}, which is always for one BRAND at a time) - used
 * by the admin dashboard's per-user "roles and permissions" detail view
 * ({@link RoleAssignmentAdminService#listAssignmentsForUser}). {@code scopeId} is {@code null}
 * for every scope type except {@code BRAND} (see {@code insert.sql}'s seed data commentary).
 */
public record UserRoleAssignmentView(
		UUID roleAssignmentId,
		String roleName,
		String roleType,
		String scopeType,
		UUID scopeId,
		Instant assignedAt) {
}
