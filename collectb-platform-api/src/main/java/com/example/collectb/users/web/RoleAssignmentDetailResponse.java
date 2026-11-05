package com.example.collectb.users.web;

import java.time.Instant;
import java.util.UUID;

/**
 * One role a user holds, for the admin dashboard's per-user "roles and permissions" detail view
 * (unlike {@link PlatformUserRowResponse}, which is one row per BRAND grant for the flat "Users"
 * table). {@code scopeLabel} is the brand's display name when {@code scopeType} is
 * {@code "BRAND"} (resolved server-side so the frontend never needs a second lookup), and
 * {@code null} for every other scope type (PLATFORM/COLLECTOR/SUPPORT/COMPLIANCE/SERVICE, which
 * all have a {@code null scopeId} too - see {@code insert.sql}'s seed data commentary).
 */
public record RoleAssignmentDetailResponse(
		UUID roleAssignmentId,
		String roleName,
		String roleType,
		String scopeType,
		UUID scopeId,
		String scopeLabel,
		Instant assignedAt) {
}
