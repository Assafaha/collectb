package com.example.collectb.users.web;

import java.time.Instant;
import java.util.UUID;

/** One row of the admin dashboard's "Users" table.
 *  <p>
 *  User listing returns one row per visible {@code platform_user} (never
 *  duplicated, even if the user holds several active role assignments across different brands
 *  and/or scopes): {@code roleAssignmentId}/{@code brandId}/{@code brandCode}/{@code brandName}
 *  are always {@code null} (there is no single assignment/brand to point at any more - see the
 *  "Details" dialog, {@code GET /{userId}}, for the full per-assignment breakdown and per-
 *  assignment revoke), {@code roleName} is a comma-separated human-readable summary of every
 *  active assignment (e.g. {@code "BRAND_ADMIN @ Acme, PLATFORM_ADMIN"}, or {@code null} for a
 *  user with no active assignment at all), and {@code assignedAt} is the most recent of them.
 *  <p>
 *  For a {@code BRAND_ADMIN}, the summary includes only assignments in managed Brands.
 *  The Brand grant endpoint also returns this shape for a single newly created assignment,
 *  with its assignment and Brand identifiers populated. */
public record PlatformUserRowResponse(
		UUID roleAssignmentId,
		UUID userId,
		String userEmail,
		String userDisplayName,
		UUID brandId,
		String brandCode,
		String brandName,
		String roleName,
		Instant assignedAt) {
}
