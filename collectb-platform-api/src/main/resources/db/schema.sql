-- =============================================================================
-- collectb Foundation Schema (raw DDL reference)
-- =============================================================================
-- This file is a plain SQL reference snapshot of the schema produced by the
-- Liquibase changelogs under src/main/resources/db/changelog/. It is NOT
-- executed by the application at startup -- Liquibase (db.changelog-master.xml)
-- remains the single source of truth for schema creation/migration.
-- Regenerate this file whenever the Liquibase changesets change, e.g. via:
--   docker exec collectb pg_dump -U collectdb -d collectdb --schema-only \
--     --no-owner --no-privileges -T databasechangelog -T databasechangeloglock
-- Generated: 2026-08-30
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Extensions
-- -----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =============================================================================
-- Domain: Identity (design doc section 2.2 "Identity Implementation Milestone",
--         2.2.2 "Identity Tables and Role Model")
-- =============================================================================

-- platform_user: the authenticated login identity for collectors, brand users,
-- platform admins, support/compliance users, service accounts, and system actors.
-- actor_type is VARCHAR + CHECK (not a native enum) since the allowed value set
-- is still evolving with the design doc; CHECK constraints are far cheaper to
-- alter than Postgres enum types (add-only, no rename/remove).
CREATE TABLE platform_user (
    user_id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email              VARCHAR(320) NOT NULL,
    display_name       VARCHAR(200) NOT NULL,
    actor_type         VARCHAR(30)  NOT NULL DEFAULT 'COLLECTOR',
    account_status     VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    email_verified_at  TIMESTAMPTZ,
    last_login_at      TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by         UUID,
    disabled_reason    TEXT,
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_platform_user_email UNIQUE (email),
    CONSTRAINT ck_platform_user_actor_type CHECK (actor_type IN
        ('COLLECTOR', 'BRAND_USER', 'PLATFORM_ADMIN', 'SUPPORT_USER',
         'COMPLIANCE_USER', 'SERVICE_ACCOUNT', 'SYSTEM')),
    CONSTRAINT fk_platform_user_created_by FOREIGN KEY (created_by)
        REFERENCES platform_user (user_id) ON DELETE SET NULL
);
CREATE INDEX ix_platform_user_status ON platform_user (account_status);
CREATE INDEX ix_platform_user_actor_type ON platform_user (actor_type);

-- identity_credential: authentication method metadata, kept separate from
-- platform_user so secrets are not exposed to business services. Extends the
-- design doc's minimal field list with password auth, external IdP linkage,
-- MFA factors, and credential lifecycle (expiry/revocation).
CREATE TABLE identity_credential (
    credential_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    provider            VARCHAR(30) NOT NULL DEFAULT 'LOCAL',
    credential_type     VARCHAR(30) NOT NULL DEFAULT 'PASSWORD',
    provider_subject    VARCHAR(255),
    credential_ref      VARCHAR(255),
    password_hash       VARCHAR(255),
    password_algorithm  VARCHAR(50),
    mfa_factor_type     VARCHAR(30),
    mfa_enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at        TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ,
    revoked_at          TIMESTAMPTZ,
    revoked_reason      TEXT,
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_identity_credential_provider CHECK (provider IN
        ('LOCAL', 'GOOGLE', 'APPLE', 'BRAND_SSO', 'SERVICE_CREDENTIAL', 'PASSKEY')),
    CONSTRAINT ck_identity_credential_credential_type CHECK (credential_type IN
        ('PASSWORD', 'OIDC', 'SAML', 'MFA_FACTOR', 'API_CLIENT', 'PASSKEY')),
    CONSTRAINT ck_identity_credential_status CHECK (status IN
        ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED', 'REVOKED', 'EXPIRED', 'DISABLED')),
    CONSTRAINT ck_identity_credential_mfa_factor_type CHECK (mfa_factor_type IS NULL OR mfa_factor_type IN
        ('TOTP', 'SMS', 'EMAIL_OTP', 'WEBAUTHN')),
    CONSTRAINT fk_identity_credential_user FOREIGN KEY (user_id)
        REFERENCES platform_user (user_id) ON DELETE CASCADE
);
CREATE INDEX ix_identity_credential_user_id ON identity_credential (user_id);
-- Prevents exact-duplicate credentials per user (e.g. re-running a seed script) while still
-- allowing multiple distinct credentials per user (different MFA factor types, different
-- linked OIDC subjects, different API client refs). COALESCE(..., '') normalizes NULLs so two
-- rows both leaving an optional column NULL (e.g. two PASSWORD credentials) still collide.
CREATE UNIQUE INDEX uq_identity_credential_identity ON identity_credential (
    user_id, provider, credential_type,
    COALESCE(provider_subject, ''), COALESCE(credential_ref, ''), COALESCE(mfa_factor_type, '')
);

-- role: reusable access roles (COLLECTOR, BRAND_VIEWER, BRAND_EDITOR,
-- BRAND_ADMIN, PLATFORM_ADMIN, SUPPORT_AGENT, COMPLIANCE_OFFICER, SERVICE_ACCOUNT).
CREATE TABLE role (
    role_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_name      VARCHAR(50) NOT NULL,
    description    TEXT,
    role_type      VARCHAR(30) NOT NULL,
    is_privileged  BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_role_role_name UNIQUE (role_name)
);

-- permission: fine-grained actions that can be granted through roles
-- (e.g. collection.create, template.approve.brand, adc.issue.brand).
CREATE TABLE permission (
    permission_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    permission_key  VARCHAR(100) NOT NULL,
    resource_type   VARCHAR(50) NOT NULL,
    action          VARCHAR(50) NOT NULL,
    description     TEXT,
    CONSTRAINT uq_permission_permission_key UNIQUE (permission_key)
);

-- role_permission: maps roles to the permissions they grant.
CREATE TABLE role_permission (
    role_permission_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id             UUID NOT NULL,
    permission_id       UUID NOT NULL,
    CONSTRAINT uq_role_permission_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id)
        REFERENCES role (role_id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id)
        REFERENCES permission (permission_id) ON DELETE CASCADE
);
CREATE INDEX ix_role_permission_permission_id ON role_permission (permission_id);

-- role_assignment: connects a user to a role within an explicit scope
-- (platform-wide, brand-specific, collector-specific, or service-account access).
-- Design rule: platform_user answers "who is the actor", role_assignment
-- answers "what can they do and in which scope" -- roles are never a single
-- field on platform_user because one user may hold multiple scoped roles.
CREATE TABLE role_assignment (
    role_assignment_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    role_id             UUID NOT NULL,
    scope_type          VARCHAR(30) NOT NULL,
    scope_id            UUID,
    status              VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    assigned_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    assigned_by         UUID,
    CONSTRAINT fk_role_assignment_user FOREIGN KEY (user_id)
        REFERENCES platform_user (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_role_assignment_role FOREIGN KEY (role_id)
        REFERENCES role (role_id) ON DELETE CASCADE,
    CONSTRAINT fk_role_assignment_assigned_by FOREIGN KEY (assigned_by)
        REFERENCES platform_user (user_id) ON DELETE SET NULL
);
CREATE INDEX ix_role_assignment_user_id ON role_assignment (user_id);
CREATE INDEX ix_role_assignment_scope ON role_assignment (scope_type, scope_id);

-- =============================================================================
-- Domain: Brand and Catalog (product-definition layer)
-- =============================================================================
-- A brand owns collections, collections contain collectible templates, and
-- templates reference media assets. Nothing can be issued as an ADC until
-- this layer exists. Brand membership/role is modeled via role_assignment
-- (scope_type=BRAND, scope_id=brand_id) in the Identity domain above -- there
-- is deliberately no separate brand_user table.
-- =============================================================================

CREATE TABLE brand (
    brand_id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    brand_code          VARCHAR(80) NOT NULL,
    name                VARCHAR(200) NOT NULL,
    description         TEXT,
    license_status      VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_by_user_id  UUID NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_brand_brand_code UNIQUE (brand_code),
    CONSTRAINT uq_brand_name UNIQUE (name),
    CONSTRAINT fk_brand_created_by_user FOREIGN KEY (created_by_user_id)
        REFERENCES platform_user (user_id) ON DELETE RESTRICT
);
CREATE INDEX ix_brand_license_status ON brand (license_status);

CREATE TABLE collection (
    collection_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    brand_id            UUID NOT NULL,
    collection_code     VARCHAR(80) NOT NULL,
    name                VARCHAR(200) NOT NULL,
    description         TEXT,
    release_status      VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    release_start_at    TIMESTAMPTZ,
    release_end_at      TIMESTAMPTZ,
    created_by_user_id  UUID NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_collection_brand_code UNIQUE (brand_id, collection_code),
    CONSTRAINT fk_collection_brand FOREIGN KEY (brand_id)
        REFERENCES brand (brand_id) ON DELETE CASCADE,
    CONSTRAINT fk_collection_created_by_user FOREIGN KEY (created_by_user_id)
        REFERENCES platform_user (user_id) ON DELETE RESTRICT
);
CREATE INDEX ix_collection_brand_id ON collection (brand_id);
CREATE INDEX ix_collection_release_status ON collection (release_status);

CREATE TABLE media_asset (
    media_asset_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_type       VARCHAR(30) NOT NULL,
    storage_uri      TEXT NOT NULL,
    cdn_uri          TEXT,
    content_type     VARCHAR(100),
    checksum         VARCHAR(128) NOT NULL,
    file_size_bytes  BIGINT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_media_asset_storage_uri UNIQUE (storage_uri)
);
CREATE INDEX ix_media_asset_asset_type ON media_asset (asset_type);

-- collectible_template: DRAFT -> APPROVED -> PUBLISHED lifecycle (publish_status).
CREATE TABLE collectible_template (
    template_id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    collection_id            UUID NOT NULL,
    media_asset_id           UUID,
    template_code            VARCHAR(80) NOT NULL,
    display_name             VARCHAR(200) NOT NULL,
    rarity                   VARCHAR(50),
    edition_size             INTEGER NOT NULL,
    metadata_schema_version  VARCHAR(20) NOT NULL,
    metadata_uri             TEXT NOT NULL,
    metadata_hash            VARCHAR(128),
    publish_status           VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_by_user_id       UUID NOT NULL,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_collectible_template_collection_code UNIQUE (collection_id, template_code),
    CONSTRAINT fk_collectible_template_collection FOREIGN KEY (collection_id)
        REFERENCES collection (collection_id) ON DELETE CASCADE,
    CONSTRAINT fk_collectible_template_media_asset FOREIGN KEY (media_asset_id)
        REFERENCES media_asset (media_asset_id) ON DELETE SET NULL,
    CONSTRAINT fk_collectible_template_created_by_user FOREIGN KEY (created_by_user_id)
        REFERENCES platform_user (user_id) ON DELETE RESTRICT
);
CREATE INDEX ix_collectible_template_collection_id ON collectible_template (collection_id);
CREATE INDEX ix_collectible_template_publish_status ON collectible_template (publish_status);

-- =============================================================================
-- Domain: Collector
-- =============================================================================
-- The Collector Service owns the person/account side of the platform (identity,
-- KYC, eligibility, preferences) -- it does not own ADC lifecycle, ownership
-- history, or custody wallet operations.
-- =============================================================================

CREATE TABLE collector (
    collector_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL,
    display_name         VARCHAR(200) NOT NULL,
    account_status       VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    kyc_status           VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    eligibility_status   VARCHAR(30) NOT NULL DEFAULT 'ELIGIBLE',
    onboarding_status    VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    preferences          JSONB,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_collector_user_id UNIQUE (user_id),
    CONSTRAINT fk_collector_user FOREIGN KEY (user_id)
        REFERENCES platform_user (user_id) ON DELETE CASCADE
);
CREATE INDEX ix_collector_account_status ON collector (account_status);
CREATE INDEX ix_collector_kyc_status ON collector (kyc_status);
