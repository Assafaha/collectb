package com.example.collectb.identity.entity;

import java.time.Instant;
import java.util.UUID;

import com.example.collectb.identity.model.AccountStatus;
import com.example.collectb.identity.model.ActorType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * JPA entity mapping the {@code platform_user} table (see
 * {@code db/changelog/changes/001-identity.xml}, {@code db/schema.sql}), per design doc
 * section 2.2.2 "Identity Tables and Role Model". Represents the authenticated platform
 * identity for both human and non-human actors ("who is the actor").
 * <p>
 * {@code actor_type}/{@code account_status} are stored as {@code VARCHAR} + {@code CHECK}
 * (not native Postgres enums) but mapped here as Java enums via {@link EnumType#STRING},
 * which round-trips using the enum constant name - keep {@link ActorType}/{@link AccountStatus}
 * in sync with the corresponding DB {@code CHECK} constraints.
 */
@Entity
@Table(name = "platform_user")
public class PlatformUser {

	@Id
	@GeneratedValue
	@Column(name = "user_id")
	private UUID userId;

	@Column(name = "email", nullable = false, unique = true)
	private String email;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(name = "actor_type", nullable = false)
	private ActorType actorType;

	@Enumerated(EnumType.STRING)
	@Column(name = "account_status", nullable = false)
	private AccountStatus accountStatus;

	@Column(name = "email_verified_at")
	private Instant emailVerifiedAt;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	/** Self-referencing pointer to the admin/system actor that created this user; nullable for
	 *  self-registered/root seed users. Only the raw UUID is stored here, not a JPA association,
	 *  to avoid a circular fetch graph on the very first user (which has no creator). */
	@Column(name = "created_by")
	private UUID createdBy;

	@Column(name = "disabled_reason")
	private String disabledReason;

	/** Optimistic locking version, backed by the {@code version} column. */
	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected PlatformUser() {
		// JPA
	}

	public PlatformUser(String email, String displayName, ActorType actorType, AccountStatus accountStatus) {
		this.email = email;
		this.displayName = displayName;
		this.actorType = actorType;
		this.accountStatus = accountStatus;
	}

	@PrePersist
	protected void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = Instant.now();
	}

	public UUID getUserId() {
		return userId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public ActorType getActorType() {
		return actorType;
	}

	public void setActorType(ActorType actorType) {
		this.actorType = actorType;
	}

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}

	public void setAccountStatus(AccountStatus accountStatus) {
		this.accountStatus = accountStatus;
	}

	public Instant getEmailVerifiedAt() {
		return emailVerifiedAt;
	}

	public void setEmailVerifiedAt(Instant emailVerifiedAt) {
		this.emailVerifiedAt = emailVerifiedAt;
	}

	public Instant getLastLoginAt() {
		return lastLoginAt;
	}

	public void setLastLoginAt(Instant lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public UUID getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(UUID createdBy) {
		this.createdBy = createdBy;
	}

	public String getDisabledReason() {
		return disabledReason;
	}

	public void setDisabledReason(String disabledReason) {
		this.disabledReason = disabledReason;
	}

	public Long getVersion() {
		return version;
	}
}
