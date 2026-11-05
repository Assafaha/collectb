package com.example.collectb.identity.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.collectb.identity.entity.Permission;
import com.example.collectb.identity.entity.Role;
import com.example.collectb.identity.entity.RolePermission;
import com.example.collectb.identity.repository.PermissionRepository;
import com.example.collectb.identity.repository.RolePermissionRepository;
import com.example.collectb.identity.repository.RoleRepository;

/**
 * Role catalog viewing and role-to-permission mapping ({@code role_permission}). Viewing roles is
 * open to {@code PLATFORM_ADMIN} and {@code BRAND_EDITOR}; reading and changing which permissions
 * a role grants requires {@code PLATFORM_IDENTITY:MANAGE}.
 */
@Service
public class RolePermissionAdminService {

	private static final Logger log = LoggerFactory.getLogger(RolePermissionAdminService.class);

	private static final String PLATFORM_ADMIN_ROLE = "PLATFORM_ADMIN";
	private static final String IDENTITY_MANAGE_KEY = "PLATFORM_IDENTITY:MANAGE";

	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;
	private final RolePermissionRepository rolePermissionRepository;

	public RolePermissionAdminService(RoleRepository roleRepository, PermissionRepository permissionRepository,
			RolePermissionRepository rolePermissionRepository) {
		this.roleRepository = roleRepository;
		this.permissionRepository = permissionRepository;
		this.rolePermissionRepository = rolePermissionRepository;
	}

	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE') or hasRole('BRAND_EDITOR')")
	@Transactional(readOnly = true)
	public List<Role> listRoles() {
		return roleRepository.findAll();
	}

	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE')")
	@Transactional(readOnly = true)
	public List<Permission> listPermissions() {
		return permissionRepository.findAll();
	}

	/** Creates a brand-new permission in the catalog - platform admin only. */
	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE')")
	@Transactional
	public Permission createPermission(String permissionKey, String resourceType, String action, String description) {
		String key = requireNonBlank(permissionKey, "permissionKey");
		requireNonBlank(resourceType, "resourceType");
		requireNonBlank(action, "action");
		if (permissionRepository.findByPermissionKey(key).isPresent()) {
			throw new IllegalStateException("A permission with key " + key + " already exists");
		}
		Permission permission = new Permission(key, resourceType, action, description);
		Permission saved = permissionRepository.save(permission);
		log.info("Created permission key={}", key);
		return saved;
	}

	/** Edits an existing permission's fields - platform admin only. Renaming
	 *  {@code PLATFORM_IDENTITY:MANAGE} itself is refused, since every {@code @PreAuthorize}
	 *  check gating this very CRUD (and all of role/role_assignment administration) is hardcoded
	 *  against that literal key - renaming it here would silently lock every platform admin out. */
	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE')")
	@Transactional
	public Permission updatePermission(UUID permissionId, String permissionKey, String resourceType, String action,
			String description) {
		Permission permission = requirePermission(permissionId);
		String key = requireNonBlank(permissionKey, "permissionKey");
		requireNonBlank(resourceType, "resourceType");
		requireNonBlank(action, "action");
		if (IDENTITY_MANAGE_KEY.equals(permission.getPermissionKey()) && !IDENTITY_MANAGE_KEY.equals(key)) {
			throw new IllegalStateException("Cannot rename " + IDENTITY_MANAGE_KEY + " (every admin check depends on this exact key)");
		}
		permissionRepository.findByPermissionKey(key)
				.filter(existing -> !existing.getPermissionId().equals(permissionId))
				.ifPresent(existing -> {
					throw new IllegalStateException("A permission with key " + key + " already exists");
				});
		permission.setPermissionKey(key);
		permission.setResourceType(resourceType);
		permission.setAction(action);
		permission.setDescription(description);
		log.info("Updated permission id={} key={}", permissionId, key);
		return permissionRepository.save(permission);
	}

	/** Deletes a permission from the catalog - platform admin only. Refuses to delete one that is
	 *  still granted to any role (clear it from every role first via {@link #revokePermission}),
	 *  and refuses to ever delete {@code PLATFORM_IDENTITY:MANAGE} itself (would lock out every
	 *  platform admin, including the ability to recreate it). */
	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE')")
	@Transactional
	public void deletePermission(UUID permissionId) {
		Permission permission = requirePermission(permissionId);
		if (IDENTITY_MANAGE_KEY.equals(permission.getPermissionKey())) {
			throw new IllegalStateException("Cannot delete " + IDENTITY_MANAGE_KEY + " (would lock out all administrators)");
		}
		if (rolePermissionRepository.existsByPermissionId(permissionId)) {
			throw new IllegalStateException(
					"Permission " + permission.getPermissionKey() + " is still granted to one or more roles - revoke it from them first");
		}
		permissionRepository.delete(permission);
		log.info("Deleted permission id={} key={}", permissionId, permission.getPermissionKey());
	}

	private static String requireNonBlank(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		return value.trim();
	}

	/** Ids of the permissions currently granted to {@code roleId}. */
	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE')")
	@Transactional(readOnly = true)
	public Set<UUID> listPermissionIdsForRole(UUID roleId) {
		requireRole(roleId);
		return rolePermissionRepository.findByRoleId(roleId).stream()
				.map(RolePermission::getPermissionId)
				.collect(Collectors.toSet());
	}

	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE')")
	@Transactional
	public void grantPermission(UUID roleId, UUID permissionId) {
		requireRole(roleId);
		requirePermission(permissionId);
		if (rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
			return;
		}
		rolePermissionRepository.save(new RolePermission(roleId, permissionId));
		log.info("Granted permission {} to role {}", permissionId, roleId);
	}

	@PreAuthorize("hasAuthority('PLATFORM_IDENTITY:MANAGE')")
	@Transactional
	public void revokePermission(UUID roleId, UUID permissionId) {
		Role role = requireRole(roleId);
		Permission permission = requirePermission(permissionId);
		if (PLATFORM_ADMIN_ROLE.equals(role.getRoleName()) && IDENTITY_MANAGE_KEY.equals(permission.getPermissionKey())) {
			throw new IllegalStateException(
					"Cannot remove " + IDENTITY_MANAGE_KEY + " from " + PLATFORM_ADMIN_ROLE + " (would lock out all administrators)");
		}
		rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
		log.info("Revoked permission {} from role {}", permissionId, roleId);
	}

	private Role requireRole(UUID roleId) {
		return roleRepository.findById(roleId)
				.orElseThrow(() -> new IllegalArgumentException("No role with id " + roleId));
	}

	private Permission requirePermission(UUID permissionId) {
		return permissionRepository.findById(permissionId)
				.orElseThrow(() -> new IllegalArgumentException("No permission with id " + permissionId));
	}
}
