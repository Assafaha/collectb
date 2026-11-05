package com.example.collectb.identity.model;

/**
 * Identifies the identity/authentication provider backing an {@code identity_credential} row.
 * <p>
 * Mirrors the {@code provider} column on the {@code identity_credential} table
 * (see {@code db/changelog/changes/001-identity.xml} and {@code db/schema.sql}), enforced via
 * the {@code ck_identity_credential_provider} CHECK constraint. Keep in sync with that constraint.
 */
public enum CredentialProvider {

	/** Local email/password authentication owned entirely by the platform. */
	LOCAL,

	/** Federated login via Google OAuth2/OIDC. */
	GOOGLE,

	/** Federated login via Apple Sign In. */
	APPLE,

	/** Federated login via a brand's own SSO/identity provider. */
	BRAND_SSO,

	/** A credential used by a non-human service account. */
	SERVICE_CREDENTIAL,

	/** A WebAuthn/FIDO2 passkey credential. */
	PASSKEY
}
