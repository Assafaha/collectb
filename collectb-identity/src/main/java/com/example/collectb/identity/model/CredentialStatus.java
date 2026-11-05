package com.example.collectb.identity.model;

/**
 * Classifies the lifecycle state of an {@code identity_credential} row.
 * <p>
 * Mirrors the {@code status} column on the {@code identity_credential} table
 * (see {@code db/changelog/changes/001-identity.xml} and {@code db/schema.sql}), enforced via
 * the {@code ck_identity_credential_status} CHECK constraint. Keep in sync with that constraint.
 */
public enum CredentialStatus {

	/** Credential created but not yet verified (e.g. awaiting email confirmation). */
	PENDING_VERIFICATION,

	/** Credential is active and may be used to authenticate. */
	ACTIVE,

	/** Credential is temporarily locked, typically due to repeated failed attempts. */
	LOCKED,

	/** Credential has been explicitly revoked (see {@code revoked_at}/{@code revoked_reason}). */
	REVOKED,

	/** Credential has passed its {@code expires_at} and is no longer usable. */
	EXPIRED,

	/** Credential has been administratively disabled. */
	DISABLED
}
