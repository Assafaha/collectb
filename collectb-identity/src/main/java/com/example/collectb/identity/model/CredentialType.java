package com.example.collectb.identity.model;

/**
 * Classifies the mechanism/format of an {@code identity_credential} row.
 * <p>
 * Mirrors the {@code credential_type} column on the {@code identity_credential} table
 * (see {@code db/changelog/changes/001-identity.xml} and {@code db/schema.sql}), enforced via
 * the {@code ck_identity_credential_credential_type} CHECK constraint. Keep in sync with that
 * constraint.
 */
public enum CredentialType {

	/** A local password credential (see {@code password_hash}/{@code password_algorithm}). */
	PASSWORD,

	/** An OpenID Connect / OAuth2 federated credential. */
	OIDC,

	/** A SAML federated credential. */
	SAML,

	/** A multi-factor authentication factor (see {@code mfa_factor_type}). */
	MFA_FACTOR,

	/** An API client credential used by service accounts to call internal APIs. */
	API_CLIENT,

	/** A WebAuthn/FIDO2 passkey credential. */
	PASSKEY
}
