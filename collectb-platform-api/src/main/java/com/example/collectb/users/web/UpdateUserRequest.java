package com.example.collectb.users.web;

/** Request body for {@code PUT /api/v1/users/{userId}}: edits an existing user's identity fields
 *  (not their role assignments) - see {@code RoleAssignmentAdminService#updateUser}. */
public record UpdateUserRequest(String displayName, String actorType, String accountStatus) {
}
