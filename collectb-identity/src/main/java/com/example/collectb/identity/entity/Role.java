package com.example.collectb.identity.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity mapping the {@code role} table (see
 * {@code db/changelog/changes/001-identity.xml}, {@code db/schema.sql}), per design doc
 * section 2.2.2. Defines reusable roles such as {@code COLLECTOR}, {@code BRAND_VIEWER},
 * {@code BRAND_EDITOR}, {@code BRAND_ADMIN}, {@code PLATFORM_ADMIN}, {@code SUPPORT_AGENT},
 * {@code COMPLIANCE_OFFICER}, {@code SERVICE_ACCOUNT}.
 * <p>
 * {@code role_name} and {@code role_type} are free-form {@code VARCHAR} (no CHECK constraint
 * yet, since the doc treats this as an example/recommended list rather than a fixed enum), so
 * they are mapped as plain {@link String} rather than a Java enum.
 */
@Entity
@Table(name = "role")
public class Role {

	@Id
	@GeneratedValue
	@Column(name = "role_id")
	private UUID roleId;

	@Column(name = "role_name", nullable = false, unique = true)
	private String roleName;

	@Column(name = "description")
	private String description;

	/** Groups roles by the scope_type they are assigned under in role_assignment
	 *  (e.g. PLATFORM, BRAND, COLLECTOR, SUPPORT, COMPLIANCE, SERVICE). */
	@Column(name = "role_type", nullable = false)
	private String roleType;

	@Column(name = "is_privileged", nullable = false)
	private boolean privileged;

	protected Role() {
		// JPA
	}

	public Role(String roleName, String description, String roleType, boolean privileged) {
		this.roleName = roleName;
		this.description = description;
		this.roleType = roleType;
		this.privileged = privileged;
	}

	public UUID getRoleId() {
		return roleId;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getRoleType() {
		return roleType;
	}

	public void setRoleType(String roleType) {
		this.roleType = roleType;
	}

	public boolean isPrivileged() {
		return privileged;
	}

	public void setPrivileged(boolean privileged) {
		this.privileged = privileged;
	}
}
