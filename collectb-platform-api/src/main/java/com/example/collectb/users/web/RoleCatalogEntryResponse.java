package com.example.collectb.users.web;

/**
 * One entry of the full role catalog (every {@code role_type}), for the Users detail view's
 * "add role" picker - see {@code RoleAssignmentAdminService#listAllRoles}.
 */
public record RoleCatalogEntryResponse(String roleName, String roleType, String description, boolean privileged) {
}
