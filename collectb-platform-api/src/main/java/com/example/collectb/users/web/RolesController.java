package com.example.collectb.users.web;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.collectb.identity.entity.Permission;
import com.example.collectb.identity.service.RolePermissionAdminService;

/** JSON API for the admin dashboard's "Roles" and "Role permissions" screens. */
@RestController
@RequestMapping("/api/v1/roles")
public class RolesController {

	private static final Logger log = LoggerFactory.getLogger(RolesController.class);

	private final RolePermissionAdminService service;

	public RolesController(RolePermissionAdminService service) {
		this.service = service;
	}

	@GetMapping
	public List<RoleResponse> listRoles() {
		return service.listRoles().stream()
				.map(r -> new RoleResponse(r.getRoleId(), r.getRoleName(), r.getRoleType(), r.getDescription(),
						r.isPrivileged()))
				.toList();
	}

	@GetMapping("/permissions")
	public List<PermissionResponse> listPermissions() {
		return service.listPermissions().stream()
				.map(p -> new PermissionResponse(p.getPermissionId(), p.getPermissionKey(), p.getResourceType(),
						p.getAction(), p.getDescription()))
				.toList();
	}

	/** Creates a brand-new permission in the catalog - platform admin only. */
	@PostMapping("/permissions")
	public ResponseEntity<PermissionResponse> createPermission(@RequestBody PermissionRequest request) {
		Permission permission = service.createPermission(request.permissionKey(), request.resourceType(),
				request.action(), request.description());
		return ResponseEntity.status(HttpStatus.CREATED).body(toPermissionResponse(permission));
	}

	/** Edits an existing permission's fields - platform admin only. */
	@PutMapping("/permissions/{permissionId}")
	public PermissionResponse updatePermission(@PathVariable UUID permissionId, @RequestBody PermissionRequest request) {
		Permission permission = service.updatePermission(permissionId, request.permissionKey(), request.resourceType(),
				request.action(), request.description());
		return toPermissionResponse(permission);
	}

	/** Deletes a permission from the catalog - platform admin only. Refused if it is still
	 *  granted to any role. */
	@DeleteMapping("/permissions/{permissionId}")
	public ResponseEntity<Void> deletePermission(@PathVariable UUID permissionId) {
		service.deletePermission(permissionId);
		return ResponseEntity.noContent().build();
	}

	private static PermissionResponse toPermissionResponse(Permission permission) {
		return new PermissionResponse(permission.getPermissionId(), permission.getPermissionKey(),
				permission.getResourceType(), permission.getAction(), permission.getDescription());
	}

	@GetMapping("/{roleId}/permissions")
	public List<UUID> listRolePermissionIds(@PathVariable UUID roleId) {
		return service.listPermissionIdsForRole(roleId).stream().toList();
	}

	@PutMapping("/{roleId}/permissions/{permissionId}")
	public ResponseEntity<Void> grantPermission(@PathVariable UUID roleId, @PathVariable UUID permissionId) {
		service.grantPermission(roleId, permissionId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{roleId}/permissions/{permissionId}")
	public ResponseEntity<Void> revokePermission(@PathVariable UUID roleId, @PathVariable UUID permissionId) {
		service.revokePermission(roleId, permissionId);
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
	public ResponseEntity<String> handleBadRequest(RuntimeException ex) {
		log.warn("Roles API request rejected: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
	}

	public record RoleResponse(UUID roleId, String roleName, String roleType, String description, boolean privileged) {
	}

	public record PermissionResponse(UUID permissionId, String permissionKey, String resourceType, String action,
			String description) {
	}

	/** Request body for both create and update - see {@code RolesController#createPermission}/
	 *  {@code #updatePermission}. */
	public record PermissionRequest(String permissionKey, String resourceType, String action, String description) {
	}
}
