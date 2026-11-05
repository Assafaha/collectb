package com.example.collectb.identity.entity;

import java.time.Instant;
import java.util.UUID;

import com.example.collectb.identity.model.CredentialProvider;
import com.example.collectb.identity.model.CredentialStatus;
import com.example.collectb.identity.model.CredentialType;
import com.example.collectb.identity.model.MfaFactorType;

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
 * JPA entity mapping the {@code identity_credential} table (see
 * {@code db/changelog/changes/001-identity.xml}, {@code db/schema.sql}). Stores authentication
 * method metadata (password hash, external IdP linkage, MFA factor) separately from
 * {@link PlatformUser} so secrets are not exposed to business services.
 * <p>
 * {@code provider}/{@code credential_type}/{@code status}/{@code mfa_factor_type} are stored as
 * {@code VARCHAR} + {@code CHECK} but mapped as Java enums via {@link EnumType#STRING} - keep
 * {@link CredentialProvider}/{@link CredentialType}/{@link CredentialStatus}/{@link MfaFactorType}
 * in sync with the corresponding DB {@code CHECK} constraints.
 */
@Entity
@Table(name = "identity_credential")
public class IdentityCredential {

	@Id
	@GeneratedValue
	@Column(name = "credential_id")
	private UUID credentialId;

	/** Owning user's id, stored as a plain UUID rather than a JPA @ManyToOne, since credential
	 *  lookups (e.g. "does this user have a PASSWORD credential") don't need to load the full
	 *  PlatformUser graph - the foreign key is still enforced at the DB level. */
	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false)
	private CredentialProvider provider;

	@Enumerated(EnumType.STRING)
	@Column(name = "credential_type", nullable = false)
	private CredentialType credentialType;

	@Column(name = "provider_subject")
	private String providerSubject;

	@Column(name = "credential_ref")
	private String credentialRef;

	@Column(name = "password_hash")
	private String passwordHash;

	@Column(name = "password_algorithm")
	private String passwordAlgorithm;

	@Enumerated(EnumType.STRING)
	@Column(name = "mfa_factor_type")
	private MfaFactorType mfaFactorType;

	@Column(name = "mfa_enabled", nullable = false)
	private boolean mfaEnabled;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private CredentialStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "last_used_at")
	private Instant lastUsedAt;

	@Column(name = "expires_at")
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "revoked_reason")
	private String revokedReason;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected IdentityCredential() {
		// JPA
	}

	public IdentityCredential(UUID userId, CredentialProvider provider, CredentialType credentialType,
			CredentialStatus status) {
		this.userId = userId;
		this.provider = provider;
		this.credentialType = credentialType;
		this.status = status;
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

	public UUID getCredentialId() {
		return credentialId;
	}

	public UUID getUserId() {
		return userId;
	}

	public CredentialProvider getProvider() {
		return provider;
	}

	public void setProvider(CredentialProvider provider) {
		this.provider = provider;
	}

	public CredentialType getCredentialType() {
		return credentialType;
	}

	public void setCredentialType(CredentialType credentialType) {
		this.credentialType = credentialType;
	}

	public String getProviderSubject() {
		return providerSubject;
	}

	public void setProviderSubject(String providerSubject) {
		this.providerSubject = providerSubject;
	}

	public String getCredentialRef() {
		return credentialRef;
	}

	public void setCredentialRef(String credentialRef) {
		this.credentialRef = credentialRef;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getPasswordAlgorithm() {
		return passwordAlgorithm;
	}

	public void setPasswordAlgorithm(String passwordAlgorithm) {
		this.passwordAlgorithm = passwordAlgorithm;
	}

	public MfaFactorType getMfaFactorType() {
		return mfaFactorType;
	}

	public void setMfaFactorType(MfaFactorType mfaFactorType) {
		this.mfaFactorType = mfaFactorType;
	}

	public boolean isMfaEnabled() {
		return mfaEnabled;
	}

	public void setMfaEnabled(boolean mfaEnabled) {
		this.mfaEnabled = mfaEnabled;
	}

	public CredentialStatus getStatus() {
		return status;
	}

	public void setStatus(CredentialStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Instant getLastUsedAt() {
		return lastUsedAt;
	}

	public void setLastUsedAt(Instant lastUsedAt) {
		this.lastUsedAt = lastUsedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public void setRevokedAt(Instant revokedAt) {
		this.revokedAt = revokedAt;
	}

	public String getRevokedReason() {
		return revokedReason;
	}

	public void setRevokedReason(String revokedReason) {
		this.revokedReason = revokedReason;
	}

	public Long getVersion() {
		return version;
	}
}
