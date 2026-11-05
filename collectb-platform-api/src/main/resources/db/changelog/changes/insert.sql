insert into platform_user (user_id, email, display_name, actor_type, account_status, email_verified_at, last_login_at,
                           created_at, updated_at, created_by, disabled_reason, version)
values ('0cb0d47c-be9c-4a3d-8ac3-83715dd4fb52',
        'admin@example.com',
        'Assaf Aharoni',
        'PLATFORM_ADMIN',
        'ACTIVE',
        now(),
        now(),
        now(),
        now(),
        null,
        null, 0);

insert into platform_user (email, display_name, actor_type, account_status, email_verified_at, last_login_at,
                           created_at, updated_at, created_by, disabled_reason, version)
values ('Satoshi@example.com',
        'Satoshi',
        'BRAND_USER',
        'ACTIVE',
        now(),
        now(),
        now(),
        now(),
        '0cb0d47c-be9c-4a3d-8ac3-83715dd4fb52',
        null, 0);

insert into platform_user (email, display_name, actor_type, account_status, email_verified_at, last_login_at,
                           created_at, updated_at, created_by, disabled_reason, version)
values ('omri@example.com',
        'Omri Harari',
        'COLLECTOR',
        'ACTIVE',
        now(),
        now(),
        now(),
        now(),
        '0cb0d47c-be9c-4a3d-8ac3-83715dd4fb52',
        null, 0);

-- Every seed user shares the same password, "abc123", hashed with argon2id (time_cost=3,
-- memory_cost=65536 KiB, parallelism=2), via Argon2PasswordEncoder(16, 32, 2, 65536, 3) -
-- matching PasswordEncoderConfig's parameters exactly. password_hash carries a
-- Spring-Security-style {argon2} algorithm marker prefix ahead of the standard PHC-formatted
-- argon2id hash string, so a DelegatingPasswordEncoder-style scheme can pick the right verifier
-- purely from the stored value. password_algorithm additionally records the concrete algorithm
-- ("argon2id") for reporting/migration purposes without needing to parse it.
insert into identity_credential (user_id, provider, credential_type, password_hash, password_algorithm,
                                 mfa_enabled, status, created_at, updated_at, version)
select u.user_id,
       'LOCAL',
       'PASSWORD',
       '{argon2}$argon2id$v=19$m=65536,t=3,p=2$ObRkTWDQvr13Exrn6kkrPQ$gvkvh/xY0Ao7ziBGrllpumz5ZZjuP99u9TOh873z7sg',
       'argon2id',
       false,
       'ACTIVE',
       now(),
       now(),
       0
from platform_user u
where u.email in ('admin@example.com', 'Satoshi@example.com', 'omri@example.com');

insert into identity_credential (user_id, provider, credential_type, status)
values ('0cb0d47c-be9c-4a3d-8ac3-83715dd4fb52',
        'GOOGLE',
        'OIDC',
        'ACTIVE');

-- Default brand so the seeded BRAND-scoped role assignments below (Satoshi's BRAND_ADMIN grant)
-- resolve to a real row instead of an illustrative placeholder. Uses a fixed brand_id so the
-- role_assignment insert further down can reference it directly without a subquery.
insert into brand (brand_id, brand_code, name, description, license_status, created_by_user_id, created_at, updated_at)
select '11111111-1111-1111-1111-111111111111',
       'POKEMON',
       'Pokemon',
       'Official Pokemon digital collectibles brand.',
       'ACTIVE',
       u.user_id,
       now(),
       now()
from platform_user u
where u.email = 'admin@example.com';

-- The role catalog, permission catalog, and role -> permission grants are seeded as native
-- Liquibase <insert> changes in 005-seed-identity.xml (changesets 005-seed-roles,
-- 005-seed-permissions, 005-seed-role-permissions), not here, so they run before this file and
-- don't need a lookup subquery against hard-coded UUIDs.

-- Example role assignments, connecting the seed users above to scoped roles per design doc 2.2.2:
-- role_assignment answers "what can this actor do, and in which scope" (platform_user only answers
-- "who is the actor"). Looked up by email/role_name rather than hard-coded UUIDs.
-- Assaf (PLATFORM_ADMIN actor) is granted the PLATFORM_ADMIN role at PLATFORM scope (scope_id NULL,
-- since PLATFORM is a single global scope, not tied to one brand/collector record). He is the
-- root seed user, so assigned_by is NULL (self/system-bootstrapped, no human assigner).
insert into role_assignment (user_id, role_id, scope_type, scope_id, status, assigned_at, assigned_by)
select u.user_id, r.role_id, 'PLATFORM', null, 'ACTIVE', now(), null
from platform_user u, role r
where u.email = 'admin@example.com' and r.role_name = 'PLATFORM_ADMIN';

-- Satoshi (BRAND_USER actor) is granted BRAND_ADMIN scoped to the Pokemon brand seeded above.
insert into role_assignment (user_id, role_id, scope_type, scope_id, status, assigned_at, assigned_by)
select u.user_id, r.role_id, 'BRAND', '11111111-1111-1111-1111-111111111111', 'ACTIVE', now(), a.user_id
from platform_user u, role r, platform_user a
where u.email = 'Satoshi@example.com' and r.role_name = 'BRAND_ADMIN' and a.email = 'admin@example.com';

-- Omri (COLLECTOR actor) is granted the COLLECTOR role at COLLECTOR scope. scope_id is left NULL
-- since a collector's own user_id already identifies their scope; there is no separate collector
-- record to reference here yet (Collector domain table not seeded).
insert into role_assignment (user_id, role_id, scope_type, scope_id, status, assigned_at, assigned_by)
select u.user_id, r.role_id, 'COLLECTOR', null, 'ACTIVE', now(), a.user_id
from platform_user u, role r, platform_user a
where u.email = 'omri@example.com' and r.role_name = 'COLLECTOR' and a.email = 'admin@example.com';