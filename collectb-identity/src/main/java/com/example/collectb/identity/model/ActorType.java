package com.example.collectb.identity.model;

/**
 * Classifies the type of authenticated actor represented by a {@code platform_user} row.
 * <p>
 * Mirrors the {@code actor_type} column on the {@code platform_user} table
 * (see {@code db/changelog/changes/001-identity.xml} and {@code db/schema.sql}), per the
 * design doc section 2.2 "Identity Implementation Milestone", subsection 2.2.2
 * "Identity Tables and Role Model".
 * <p>
 * The database enforces the same value set via a {@code CHECK} constraint
 * ({@code ck_platform_user_actor_type}) rather than a native Postgres enum, since this
 * set is still evolving with the design doc. Keep this enum's values in sync with that
 * constraint whenever either one changes.
 */
public enum ActorType {

	/** A collector: an end user who owns/collects Authenticated Digital Collectibles. */
	COLLECTOR,

	/** A user acting on behalf of a licensed brand (e.g. managing collections/templates). */
	BRAND_USER,

	/** A platform administrator with cross-brand, platform-level privileges. */
	PLATFORM_ADMIN,

	/** A support agent with limited, purpose-specific, auditable access. */
	SUPPORT_USER,

	/** A compliance officer with limited, purpose-specific, auditable access. */
	COMPLIANCE_USER,

	/** A non-human service account used by internal services to call platform APIs. */
	SERVICE_ACCOUNT,

	/** The system actor itself, used for automated/internal actions with no human user. */
	SYSTEM
}
