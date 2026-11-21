# Collectb

Authenticated Digital Collectibles (ADC) platform: brands define collections and collectible templates, issue ADCs to collectors, with custodial wallets and blockchain (Polygon ERC-721) as the *proof* layer. Currently built: Identity, Brand, Catalog, and an Angular admin/brand portal. Collector, ADC/Ownership, Custody, Blockchain, Audit/History and Notification are designed but not implemented.

## Sources of truth (read before non-trivial work)

- `docs/design/mvp-architecture-validation.md` – working architecture: module boundaries, domain ownership, core sequence flows, backlog.
- `docs/design/platform-design-and-tech-stack.md` – markdown conversion of `Collectable Platform Design & Tech Stack.docx` (the original product/tech design). Search it by heading; don't read it whole.
- `description.md` – chronological activity log (very large). Search it (`grep`) for history/rationale on a topic; never load it whole. Newest entries are at the bottom.
- Where the design doc and the code disagree (e.g. the doc says Java 21, the build uses Java 27), the code reflects later decisions — check `description.md` for why.

## Repository layout

Maven multi-module modular monolith (`pom.xml` is the reactor; Spring Boot 4.1, Java 27):

| Module | Owns |
|---|---|
| `collectb-identity` | Users, credentials, roles, permissions, scoped role assignments, embedded Spring Authorization Server (OAuth2/OIDC, JWT) |
| `collectb-brand` | Brands, brand licensing |
| `collectb-catalog` | Collections, collectible templates, media references, template lifecycle, `BrandCatalogPolicy` port |
| `collectb-platform-api` | The Spring Boot app: composes modules, cross-context endpoints (`users`, `brandportal`, `admin`), adapters (e.g. `catalog/BrandCatalogPolicyAdapter`), **all Liquibase changelogs**, config, and **all integration tests** |
| `collectb-admin-portal` | Angular 22 SPA (PrimeNG, AG Grid, angular-oauth2-oidc, Vitest). Separate deployment; talks only to Platform API |

Module package convention: `com.example.collectb.<context>.{entity,model,repository,service,web,security,config}`.

## Commands

Prerequisites: **JDK 27 with `JAVA_HOME` set** (the system `java` on this machine may be an older JRE), Docker running.

```bash
docker compose up -d                       # local PostgreSQL 16 (container "collectb", db collectdb, port 5432)
./mvnw -q compile                          # fast compile check of all modules
./mvnw -q -DskipTests install              # needed once (and after module changes) so the API module can resolve its siblings
./mvnw -pl collectb-platform-api spring-boot:run       # run API on :8080 (default profile: development). Do NOT add -am: Maven then runs the goal on the parent POM, which has no main class
```

Local JDK 27 lives at `C:\Users\assaf\.jdks\openjdk-27.0.1.1` (IntelliJ-managed); in a shell run `export JAVA_HOME="/c/Users/assaf/.jdks/openjdk-27.0.1.1"` first. A JVM crash (`hs_err_pid*.log` / `replay_pid*.log` in the repo root) happened once on JDK 27 and did not recur on retry — delete those files, don't commit them.

Tests: all backend tests live in `collectb-platform-api/src/test`. ITs use Testcontainers (`PostgresTestcontainerConfiguration`, needs Docker).
**Surefire does not pick up `*IT` classes by default** — plain `mvnw test` only runs `*Test`/`*Tests`. Always pass `-Dtest`:

```bash
# full backend suite
./mvnw -pl collectb-platform-api -am test -Dtest='*IT,*Test,*Tests' -Dsurefire.failIfNoSpecifiedTests=false
# one class / one method
./mvnw -pl collectb-platform-api -am test -Dtest=CatalogWorkflowIT -Dsurefire.failIfNoSpecifiedTests=false
./mvnw -pl collectb-platform-api -am test -Dtest='CatalogWorkflowIT#methodName' -Dsurefire.failIfNoSpecifiedTests=false
```

Frontend (run inside `collectb-admin-portal/`):

```bash
npm install
npm start          # ng serve on http://127.0.0.1:3000 (port fixed in angular.json; must match the OAuth redirect URIs in Oauth2ClientSeeder), proxies /api /oauth2 /login /logout /.well-known to :8080 (proxy.conf.json)
npx ng test --watch=false
npm run build      # production build — run it to verify frontend changes
```

## Architectural invariants (do not break without an explicit user decision)

- **Modular monolith, strict ownership.** A module writes only its own tables and exposes application-level services/ports. No cross-module use of another module's repositories or JPA entities; compose in `collectb-platform-api` via ports + adapters (pattern: `BrandCatalogPolicy` in catalog, implemented by `BrandCatalogPolicyAdapter` in platform-api).
- **Liquibase owns the schema.** `ddl-auto=none`. Add a new numbered changeset under `collectb-platform-api/src/main/resources/db/changelog/changes/` and register it in `db.changelog-master.xml`; never edit an applied changeset.
- **Scope is separate from permission.** Permissions are `RESOURCE:ACTION`; role assignments carry `scope_type`/`scope_id` (e.g. Brand). Brand Admins see/manage only their managed Brands — never leak other-Brand assignments in lists or details. Brand membership is a brand-scoped role assignment; don't duplicate it in another table.
- **Domain terms:** Collection/Template = product definition (not owned). ADC = the issued business asset a collector owns. Blockchain token = proof only. Digital Passport = current profile + immutable versioned snapshots, off-chain.
- **Template lifecycle** `DRAFT → APPROVED → PUBLISHED` (+ retire) with independent review (author can't approve own work) and published content immutable.
- **MFA:** Catalog review/publish/retire require MFA except under the `development` profile bypass (`collectb.catalog.allow-development-mfa-bypass`). Startup must fail if the bypass is enabled with `prod`/`production`. Never weaken this for production.
- **Future issuance / pack / transfer flows** must be idempotent (idempotency key), concurrency-safe on edition/serial allocation, and treat provider/chain submission as pending until confirmed (persisted intent, reconciliation, outbox).
- Marketplace/trading and other non-MVP features are deferred unless the user changes scope.
- Open product decisions (e.g. pack composition/randomness, unopened-pack transferability) must be asked, not assumed.

## Conventions

- Auth: authorization_code + PKCE only (no password grant). Passwords hashed with Argon2 (BouncyCastle required at runtime).
- Dev seed users come from `005-seed-identity.xml`; local credentials are development-only — never copy them into code, tests, or docs beyond what exists.
- Prefer existing patterns in neighbouring code over new abstractions. Keep REST DTOs in `web/` as records (`*Request`/`*Response`).
- Frontend: standalone Angular components under `src/app/features/<feature>/`; shared services/guards/grid helpers under `src/app/core/`. Use the existing AG Grid defaults/theme and PrimeNG components.
- Verification before calling work done: targeted backend tests for touched behavior (plus security-relevant regressions), `npx ng test --watch=false` and `npm run build` for portal changes.

## Working workflow

1. Non-trivial feature → plan first (plan mode), consulting the **architect** subagent for design/boundaries.
2. Implement in the main session.
3. Run the **test-runner** subagent for the relevant suites.
4. Review the diff with the **code-reviewer** subagent (or `/code-review`), and the **security** subagent for anything touching auth, scope, roles, Catalog approval, or issuance.
5. Append an entry to the bottom of `description.md`: `## YYYY-MM-DD - <short title>` followed by 2–4 concise bullets (what changed and why, decisions made, how it was verified). Update `docs/design/mvp-architecture-validation.md` when design/boundaries change.
