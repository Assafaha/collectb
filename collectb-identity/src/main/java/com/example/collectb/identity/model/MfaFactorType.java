package com.example.collectb.identity.model;

/**
 * Classifies the multi-factor authentication (MFA) factor type for an {@code identity_credential}
 * row where {@code credential_type = MFA_FACTOR}.
 * <p>
 * Mirrors the {@code mfa_factor_type} column on the {@code identity_credential} table
 * (see {@code db/changelog/changes/001-identity.xml} and {@code db/schema.sql}), enforced via
 * the {@code ck_identity_credential_mfa_factor_type} CHECK constraint (nullable). Keep in sync
 * with that constraint.
 */
public enum MfaFactorType {

	/** Time-based one-time password (e.g. authenticator app). */
	TOTP,

	/** One-time password delivered via SMS. */
	SMS,

	/** One-time password delivered via email. */
	EMAIL_OTP,

	/** WebAuthn/FIDO2-based hardware or platform authenticator. */
	WEBAUTHN
}
