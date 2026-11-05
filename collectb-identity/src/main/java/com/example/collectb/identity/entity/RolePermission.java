package com.example.collectb.identity.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity mapping the {@code role_permission} join table (see
 * {@code db/changelog/changes/001-identity.xml}, {@code db/schema.sql}), per design doc
 * section 2.2.2. Maps a {@link Role} to a {@link Permission} it grants.
 */
@Entity
@Table(name = "role_permission")
public class RolePermission {

	@Id
	@GeneratedValue
	@Column(name = "role_permission_id")
	private UUID rolePermissionId;

	@Column(name = "role_id", nullable = false)
	private UUID roleId;

	@Column(name = "permission_id", nullable = false)
	private UUID permissionId;

	protected RolePermission() {
		// JPA
	}

	public RolePermission(UUID roleId, UUID permissionId) {
		this.roleId = roleId;
		this.permissionId = permissionId;
	}

	public UUID getRolePermissionId() {
		return rolePermissionId;
	}

	public UUID getRoleId() {
		return roleId;
	}

	public UUID getPermissionId() {
		return permissionId;
	}
}
