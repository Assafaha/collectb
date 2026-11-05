package com.example.collectb.identity.model;

/**
 * Classifies the lifecycle state of a {@code platform_user} account.
 * <p>
 * Mirrors the {@code account_status} column on the {@code platform_user} table
 * (see {@code db/changelog/changes/001-identity.xml} and {@code db/schema.sql}), per the
 * design doc section 2.2 "Identity Implementation Milestone", subsection 2.2.2
 * "Identity Tables and Role Model".
 * <p>
 * The database column is {@code VARCHAR(30)} (no CHECK constraint yet); if one is added,
 * keep this enum's values in sync with it.
 */
public enum AccountStatus {

	/** Account created but not yet verified (e.g. awaiting email confirmation). */
	PENDING_VERIFICATION,

	/** Account is active and may authenticate/perform actions per its assigned roles. */
	ACTIVE,

	/** Account is temporarily locked, typically due to repeated failed login attempts. */
	LOCKED,

	/** Account has been administratively disabled and cannot authenticate. */
	DISABLED,

	/** Account is suspended, typically pending investigation or compliance review. */
	SUSPENDED,

	/** Account has been deleted/deactivated permanently. */
	DELETED
}
