package com.example.collectb.identity.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * JPA entity mapping the {@code role_assignment} table (see
 * {@code db/changelog/changes/001-identity.xml}, {@code db/schema.sql}), per design doc
 * section 2.2.2. Connects a {@link PlatformUser} to a {@link Role} within an explicit
 * platform/brand/collector/support/compliance/service scope.
 * <p>
 * Design rule (doc 2.2.2): {@code platform_user} answers who the actor is, while
 * {@code role_assignment} answers what the actor can do and in which scope - one user may hold
 * several scoped roles at once (e.g. collector plus brand admin for one specific brand).
 * All foreign keys are stored as plain UUIDs rather than JPA associations, since authorization
 * checks typically only need the id/role-name/scope tuple, not the full related entity graphs.
 */
@Entity
@Table(name = "role_assignment")
public class RoleAssignment {

	@Id
	@GeneratedValue
	@Column(name = "role_assignment_id")
	private UUID roleAssignmentId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "role_id", nullable = false)
	private UUID roleId;

	/** PLATFORM, BRAND, COLLECTOR, SUPPORT, COMPLIANCE, or SERVICE - matches the role's
	 *  role_type grouping, but scoped to a concrete instance via scopeId where applicable. */
	@Column(name = "scope_type", nullable = false)
	private String scopeType;

	/** Nullable: PLATFORM/COLLECTOR-scoped assignments may have no separate scope record to
	 *  point at (e.g. PLATFORM is a single global scope; a collector's own user_id already
	 *  identifies their scope). BRAND-scoped assignments should reference a brand.brand_id. */
	@Column(name = "scope_id")
	private UUID scopeId;

	@Column(name = "status", nullable = false)
	private String status;

	@Column(name = "assigned_at", nullable = false, updatable = false)
	private Instant assignedAt;

	@Column(name = "assigned_by")
	private UUID assignedBy;

	protected RoleAssignment() {
		// JPA
	}

	public RoleAssignment(UUID userId, UUID roleId, String scopeType, UUID scopeId, String status, UUID assignedBy) {
		this.userId = userId;
		this.roleId = roleId;
		this.scopeType = scopeType;
		this.scopeId = scopeId;
		this.status = status;
		this.assignedBy = assignedBy;
	}

	@PrePersist
	protected void onCreate() {
		this.assignedAt = Instant.now();
	}

	public UUID getRoleAssignmentId() {
		return roleAssignmentId;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getRoleId() {
		return roleId;
	}

	public String getScopeType() {
		return scopeType;
	}

	public void setScopeType(String scopeType) {
		this.scopeType = scopeType;
	}

	public UUID getScopeId() {
		return scopeId;
	}

	public void setScopeId(UUID scopeId) {
		this.scopeId = scopeId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getAssignedAt() {
		return assignedAt;
	}

	public UUID getAssignedBy() {
		return assignedBy;
	}
}
