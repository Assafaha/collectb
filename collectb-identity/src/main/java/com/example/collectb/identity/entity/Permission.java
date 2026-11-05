package com.example.collectb.identity.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity mapping the {@code permission} table (see
 * {@code db/changelog/changes/001-identity.xml}, {@code db/schema.sql}), per design doc
 * section 2.2.2. Defines fine-grained actions that can be granted through roles, keyed by a
 * {@code RESOURCE:ACTION}-style {@code permission_key} (e.g. {@code COLLECTION:UPDATE}). The
 * resource name deliberately omits any brand/scope qualifier - which brand (or other scope) a
 * grant applies to is carried entirely by {@code role_assignment.scope_type}/{@code scope_id},
 * never by the permission key itself.
 */
@Entity
@Table(name = "permission")
public class Permission {

	@Id
	@GeneratedValue
	@Column(name = "permission_id")
	private UUID permissionId;

	@Column(name = "permission_key", nullable = false, unique = true)
	private String permissionKey;

	@Column(name = "resource_type", nullable = false)
	private String resourceType;

	@Column(name = "action", nullable = false)
	private String action;

	@Column(name = "description")
	private String description;

	protected Permission() {
		// JPA
	}

	public Permission(String permissionKey, String resourceType, String action, String description) {
		this.permissionKey = permissionKey;
		this.resourceType = resourceType;
		this.action = action;
		this.description = description;
	}

	public UUID getPermissionId() {
		return permissionId;
	}

	public String getPermissionKey() {
		return permissionKey;
	}

	public void setPermissionKey(String permissionKey) {
		this.permissionKey = permissionKey;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
