package com.example.collectb.users.web;

import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/users/{userId}/roles}: grants {@code roleName} to the
 * path's user at the given scope. {@code scopeId} must be a brand id when {@code scopeType} is
 * {@code "BRAND"}, and must be {@code null} for every other scope type (PLATFORM/COLLECTOR/
 * SUPPORT/COMPLIANCE/SERVICE) - enforced server-side by
 * {@code RoleAssignmentAdminService#assignRoleToUser}.
 */
public record AssignRoleRequest(String roleName, String scopeType, UUID scopeId) {
}
