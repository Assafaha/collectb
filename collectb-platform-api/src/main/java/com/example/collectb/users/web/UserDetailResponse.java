package com.example.collectb.users.web;

import java.util.List;
import java.util.UUID;

/**
 * Full detail for one platform user: identity fields plus every active role assignment they hold
 * (across any scope) and the effective permission set those roles grant - backs the admin
 * dashboard's per-user expanded/detail view (see {@code UsersController#getUserDetail}).
 */
public record UserDetailResponse(
		UUID userId,
		String email,
		String displayName,
		String actorType,
		String accountStatus,
		List<RoleAssignmentDetailResponse> roleAssignments,
		List<String> permissions) {
}
