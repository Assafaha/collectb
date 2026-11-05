package com.example.collectb.users.web;

/** Request body for {@code POST /api/v1/users/accounts}: creates a brand-new {@code
 *  platform_user} with no role assignment yet - see {@code RoleAssignmentAdminService#createUser}. */
public record CreateUserRequest(String email, String displayName, String actorType) {
}
