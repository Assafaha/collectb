package com.example.collectb.identity.service;

import java.util.List;
import java.util.UUID;

/**
 * Read model for the platform admin's full "Users" table: exactly one row per
 * {@code platform_user}, carrying every one of their active role assignments (across every
 * scope) rather than one row per assignment. Unlike {@link ScopedRoleAssignmentView} (which is
 * built strictly from existing {@code role_assignment} rows and so only ever surfaces users who
 * already hold a grant), this starts from every registered user and so also includes:
 * <ul>
 *   <li>users who hold only a PLATFORM-scoped role (e.g. a fellow {@code PLATFORM_ADMIN}) -
 *       {@code assignments} has one entry with a null/absent brand scope.
 *   <li>users with no active role assignment at all - {@code assignments} is empty.
 * </ul>
 * A user holding more than one active assignment (e.g. {@code BRAND_ADMIN} on two different
 * brands, or a brand role plus a platform role) still appears exactly once here, with all of
 * those assignments listed - the caller (the API layer) is responsible for summarizing them into
 * whatever flat display the "Users" table needs, and the full per-assignment list is what backs
 * the expandable per-user detail view.
 */
public record PlatformUserOverviewRow(
		UUID userId,
		String userEmail,
		String userDisplayName,
		List<UserRoleAssignmentView> assignments) {
}
