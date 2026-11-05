package com.example.collectb.brandportal.web;

import java.time.Instant;
import java.util.UUID;

import com.example.collectb.identity.service.ScopedRoleAssignmentView;

/** One user's access grant on a brand, for the brand portal's "users with access" table. */
public record BrandUserResponse(
		UUID roleAssignmentId,
		UUID userId,
		String userEmail,
		String userDisplayName,
		String roleName,
		Instant assignedAt) {

	public static BrandUserResponse from(ScopedRoleAssignmentView view) {
		return new BrandUserResponse(view.roleAssignmentId(), view.userId(), view.userEmail(),
				view.userDisplayName(), view.roleName(), view.assignedAt());
	}
}
