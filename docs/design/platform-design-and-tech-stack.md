<!-- Generated from "Collectable Platform Design & Tech Stack.docx" (text + tables only; no images). The DOCX remains the original; regenerate this file if the DOCX changes. -->

# Digital Collectibles Platform Technical Design

Technology Stack and High-Level Design for an Authenticated Digital Collectibles Platform

## Executive Summary

This document defines the technical design for an Authenticated Digital Collectibles platform that enables brands to create, issue, manage, and distribute official digital collectibles while preserving authenticity, scarcity, ownership, and provenance. The platform is designed so collectors interact with familiar product concepts such as ADCs, Vaults, Digital Passports, and Registries, while blockchain, wallets, and custody remain implementation details behind the experience.

The selected architecture is a modular platform with strong bounded contexts rather than a large set of independent microservices from day one. The MVP (Minimal Viable Product) will start with a Platform API that contains the main business modules and will separate infrastructure-oriented capabilities such as blockchain, custody, notification, and audit processing only where operationally justified. This approach reduces initial delivery complexity while preserving a clear path to extract services later based on scale, security, or ownership needs.

The core technology stack uses Next.js/React for the visually rich collector web experience, Angular for brand and administrative portals, Flutter for mobile applications, Java 21 and Spring Boot for backend implementation, PostgreSQL/Aurora for transactional data, Redis for caching, OpenSearch for discovery, Amazon S3 and CloudFront for media storage and delivery, Kafka for event streaming, Polygon and ERC-721 for blockchain proof, Fireblocks for custodial wallet management, and Kubernetes-based deployment on AWS.

The primary domain concept is the ADC or Authenticated Digital Collectible. A Collection represents a brand-managed release or set. A Collectible Template defines what can be issued; and an ADC is the specific issued asset owned by a collector. Each ADC is linked to a Digital Passport, Registry Entry, Ownership Record, Custodial Wallet, and optional Blockchain Token that provides cryptographic proof.

For the MVP, collectors will not hold private keys directly. The platform will provide custodial ownership through a custody provider such as Fireblocks, while maintaining an internal ownership ledger and reconciling that state against blockchain events. The system should support a future self-custody option without requiring it in the first release.

The first MVP focuses on the core flow: a brand creates a collection, defines collectible templates, issues ADCs, assigns them to collector custodial accounts, registers blockchain proof, and enables collectors to verify authenticity, ownership, and provenance. Marketplace, auctions, advanced trading, AI recommendations, multi-chain support, and advanced fraud detection are intentionally deferred until the core issuance and verification model is proven.

Based on the collectible platform vision you've been describing (Pokémon-style licensed collectibles, blockchain-backed ownership, custodial wallets, brand governance, recovery support), I would recommend the following enterprise-grade technology stack.

Your own architecture notes already point toward a hybrid model where blockchain is the ownership registry while most application functionality remains off-chain. 

## MVP Scope and Non-Goals

MVP means Minimum Viable Product. For this platform, the MVP should be the smallest version that proves the core product idea: a licensed brand can create a digital collectible collection, issue blockchain-backed Authenticated Digital Collectibles (ADCs), assign them to custodial collector accounts, and allow collectors to verify authenticity, ownership, and provenance without needing to understand blockchain technology.

The first MVP will not include a full marketplace, auctions, complex trading, AI recommendations, multi-chain support, or advanced fraud detection. These capabilities are intentionally deferred until the core ADC issuance, custody, ownership, verification, and registry flows are proven.

## Part 1: Technology Stack and Platform Technologies

### Recommended Technology Stack

### Frontend

The frontend architecture should be split by product experience rather than using one framework for every surface. The Collector Portal must feel premium, animated, and collectible-native, while the Brand Portal and internal admin surfaces require structured enterprise workflows, forms, role-based access control, approval processes, and operational reliability.

#### Collector Web Portal

- Next.js / React as the primary web stack for the collector-facing experience.
- TypeScript for safe frontend development and shared API contracts.
- Framer Motion for page transitions, card reveals, vault interactions, rarity effects, and micro-interactions.
- GSAP for advanced cinematic animations such as pack opening, reveal sequences, and campaign landing pages.
- Three.js or React Three Fiber for optional 3D card presentation, holographic effects, tilt interactions, and premium collectible showcases.
- Tailwind CSS or a token-based design system for highly customized, brandable visual design.
- WebSocket or server-sent event support for live drops, ownership updates, limited-edition availability, and future marketplace activity.
Rationale: the Collector Portal is the main visual showcase for TCG companies and collectors. It should support immersive experiences such as animated Vaults, card inspection, rarity effects, pack-opening flows, Digital Passport storytelling, and collectible galleries. React and Next.js provide a stronger ecosystem for animation-heavy consumer experiences than an Angular-only approach.

#### Brand Portal and Admin Portal

- Angular for brand-facing and internal administrative applications.
- TypeScript for consistency with shared contracts and enterprise application structure.
- Angular Material, PrimeNG, or an internal component library for dashboards, tables, forms, approval queues, and operational screens.
- RBAC-aware routing, guards, and component-level authorization for brand-scoped workflows.
- OAuth2/OIDC integration for brand SSO, platform admin login, and privileged operations.
- Form-heavy workflows for brand onboarding, collection creation, collectible template management, media upload, review, approval, and issuance control.
Rationale: Angular remains the right choice for structured enterprise portals where maintainability, forms, dashboard navigation, RBAC, and opinionated architecture matter more than cinematic interaction design.

#### Mobile Application

- Flutter for iOS and Android collector applications.
- Single mobile codebase with strong support for custom visuals, gestures, animations, and card-based interfaces.
- Use Flutter for mobile Vault browsing, collectible detail pages, push notifications, passport verification, and future event-based collector experiences.

#### Shared Design System

- Define shared design tokens for color, typography, spacing, motion, elevation, rarity effects, and brand theming.
- Keep reusable interaction patterns consistent across Collector Web, Mobile, Brand Portal, and Admin Portal.
- Support brand-specific visual skins so each TCG company can express its identity without forking the core application architecture.
- Store artwork, videos, animation assets, 3D models, thumbnails, and high-resolution media in Amazon S3 and deliver them through CloudFront or another CDN.

#### Frontend Recommendation Summary

| Application | Recommended Technology | Reason |
|---|---|---|
| Collector Web Portal | Next.js / React | Best fit for premium visuals, animation-heavy experiences, collectible showcases, pack opening, rarity effects, and SEO-friendly public pages. |
| Brand Portal | Angular | Strong fit for enterprise workflows, forms, dashboards, approvals, RBAC, and brand-scoped administration. |
| Admin Portal | Angular | Strong fit for internal operations, support tools, compliance workflows, audit views, and platform configuration. |
| Mobile App | Flutter | Excellent cross-platform mobile framework for animated collector experiences, custom card interfaces, and iOS/Android delivery from one codebase. |
| Shared Experience Layer | Design tokens, shared assets, motion guidelines, and API contracts | Keeps visual identity, brand theming, motion language, and API usage consistent across all client applications. |

### API Layer

#### API Gateway

- Spring Cloud Gateway
- Amazon API Gateway, or AWS Application Load Balancer for Kubernetes ingress-based exposure
Responsibilities:

- Authentication
- Rate limiting
- Audit logging
- Brand API exposure

### Backend Technologies

Since you already work heavily with Spring Boot:

#### Microservices

- Java 21
- Spring Boot 4.x
- Spring Security
- Spring Authorization Server
Core services:

| 1     Identity Service<br>2     Collector Service<br>3     Brand Service<br>4     Catalog Service<br>5     ADC Service<br>6     Marketplace Service<br>7     Trading Service<br>8     Blockchain Service<br>9     Wallet Service<br>10     Notification Service<br>11     Audit Service |
|---|

### Authentication Technologies

#### Identity Platform

- Spring Authorization Server
- OAuth2
- OpenID Connect
- MFA
- SSO
Supported login methods:

- Email/password
- Social login
- Brand SSO
- Passkeys (future)

### Database Layer

### PostgreSQL

Primary transactional database

Stores:

- Users
- Catalogs
- Orders
- Marketplace listings
- Trading history
- Governance rules
Why?

- Strong consistency
- Excellent Spring support
- JSONB support

### Redis

Stores:

- Sessions
- Caching
- Leaderboards
- Hot marketplace data

### Search Engine

### OpenSearch

or

### Elasticsearch

Stores:

- Collectible metadata
- Text search
- Rarity filtering
- Marketplace filtering
Example:

| 1     Pokemon<br>2     → Charizard<br>3     → First Edition<br>4     → Holographic<br>5     → Grade 10 |
|---|

### Blockchain Layer

From your previous discussions, I would recommend:

### Polygon

Reasons:

- Low fees
- Mature ecosystem
- Ethereum compatibility
- Massive NFT adoption
Your document specifically highlights Polygon as a strong candidate.

### Smart Contracts

#### ERC-721

For unique collectibles.

| 1     1 Card<br>2     =<br>3     1 NFT |
|---|

### Custody Layer

Your preferred model is custodial ownership for most collectors. The referenced design recommends custody providers such as Fireblocks or BitGo. 

#### Recommended

##### Fireblocks

Pros:

- Enterprise-grade
- HSM-backed
- Multi-party computation
- Recovery support
or

##### BitGo

Pros:

- Large ecosystem
- Insured custody offerings

### Storage Layer

Do NOT store images on-chain.

Recommended:

### Amazon S3

Stores:

- Card artwork
- Videos
- Animation assets
- High resolution media

### CDN

- Amazon CloudFront
- Cloudflare
Global asset delivery.

### Event Streaming

### Kafka

Used for:

| 1     Minted<br>2     Transferred<br>3     Listed<br>4     Sold<br>5     Frozen<br>6     Recovered |
|---|

events.

Benefits:

- Scalability
- Auditability
- Event sourcing

### Blockchain Integration Service

Build a dedicated service:

| 1     Blockchain Service |
|---|

Responsibilities:

- Register blockchain proof for ADCs
- Transfer ownership
- Freeze assets
- Recover assets
- Monitor blockchain events
Recommended libraries:

- Web3j (Java)
- Alchemy APIs
- Infura APIs

### AI and Analytics Technologies

Useful for:

- Fake account detection
- Fraud detection
- Marketplace recommendations
Stack:

- Amazon Bedrock
- Amazon SageMaker
- Amazon OpenSearch Service
- AWS Glue / Amazon Redshift for analytics pipelines

### Monitoring and Observability

Because you're already familiar with Kubernetes and Prometheus:

#### Observability

- Prometheus
- Grafana
- Loki
- OpenTelemetry
Metrics:

| 1     ADC issuance rate<br>2     Transaction failures<br>3     Marketplace latency<br>4     Wallet operations<br>5     Brand activity |
|---|

### Kubernetes and Container Platform

### Amazon EKS

or

### Amazon ECS with AWS Fargate

Deployment:

| 1     AWS Load Balancer Controller / Application Load Balancer<br>2     cert-manager or AWS Certificate Manager<br>3     Amazon Managed Service for Prometheus<br>4     Amazon Managed Grafana<br>5     KEDA or EKS Cluster Autoscaler / Karpenter |
|---|

KEDA or Karpenter is especially useful for marketplace spikes such as limited-edition drops.

### Logical Architecture Diagram

| 1     Collector App<br>2     Brand Portal<br>3     Mobile App<br>4            │<br>5            ▼<br>6      Amazon API Gateway / ALB<br>7            │<br>8      ┌────────────────────┐<br>9      │ Spring Boot APIs   │<br>10      └────────────────────┘<br>11            │<br>12      ├─ Amazon Aurora PostgreSQL / Amazon RDS<br>13      ├─ Amazon ElastiCache for Redis<br>14      ├─ Amazon OpenSearch Service<br>15      ├─ Amazon MSK / Kafka<br>16      ├─ Amazon S3 + CloudFront<br>17      │<br>18      ▼<br>19      Blockchain Service<br>20            │<br>21            ▼<br>22      Polygon Blockchain<br>23            │<br>24            ▼<br>25      Fireblocks Custody |
|---|

### Technology Stack Summary

Frontend

- Next.js / React for Collector Web, Angular for Brand/Admin Portals, and Flutter for Mobile
Backend

- Java 21 + Spring Boot
Data

- Amazon Aurora PostgreSQL / Amazon RDS + Amazon ElastiCache for Redis + Amazon OpenSearch Service
Blockchain

- Polygon + ERC-721
Custody

- Fireblocks
Hosting

- Amazon EKS, or Amazon ECS with AWS Fargate for simpler container operations
Observability

- Amazon Managed Service for Prometheus + Amazon Managed Grafana + AWS X-Ray / OpenTelemetry
This stack gives you:

- Brand governance
- Blockchain-verifiable ownership
- Custodial experience for mainstream collectors
- Recovery of stolen assets
- Scalability to millions of collectibles
- Enterprise-grade security and compliance
The central design principle is: keep ownership and provenance on-chain, but keep collectible content, trading logic, user accounts, governance, and recovery workflows off-chain. That gives brands independent verification while preserving a consumer-friendly experience.

## Part 2: High-Level Platform Design

### 2.1 Design Context and Objective

Now that the recommended technology stack is defined, the next step is to move from technology selection into platform design. The goal is to define how the system behaves, how the main business domains are separated, which service owns each data model, and which flows must be reliable, auditable, and recoverable.

### 2.2 First Design Step: Identity Implementation Milestone

The first implementation milestone should establish the minimum secure identity foundation required before building Brand, Catalog, ADC issuance, custody, blockchain, marketplace, or support workflows. The goal is not to build a full enterprise identity product on day one, but to prove that the platform can authenticate users, issue trusted tokens, assign roles, enforce scoped permissions, and map authenticated identities to collector, brand, admin, support, compliance, service, or system actors.

This milestone should produce a reusable identity layer that every other module can depend on. Brand users should be authorized only within their assigned brand scopes, collectors should access only their own account context, service accounts should call internal APIs explicitly as non-human actors, and privileged actions should require stronger permissions and auditability.

#### 2.2.1 Milestone Scope

- Support collector registration and login using email/password for the MVP.
- Support brand user, platform admin, support user, compliance user, service account, and system actor identities.
- Issue OAuth2/OIDC-compatible access tokens with stable identity, actor type, roles, permissions, and scope claims.
- Implement role-based and permission-based authorization for collector, brand, platform, support, compliance, and service-account actions.
- Support brand-scoped authorization so brand users can only access resources belonging to their assigned brand.
- Map authenticated identities to domain objects such as Collector, Brand Membership, Platform Admin, Support Agent, Compliance User, and Service Account.
- Emit audit events for login, failed login, token issuance, role changes, permission changes, privileged access, and access denial.

#### 2.2.2 Identity Tables and Role Model

The Identity Service should store authenticated users, credentials, roles, permissions, and scoped role assignments in separate but related tables. This keeps authentication concerns independent from business domains while allowing one user to hold multiple scoped roles, such as a collector account plus brand access for one or more brands.

| Table | Purpose | Recommended Fields |
|---|---|---|
| PLATFORM_USER | Stores the authenticated platform identity for human and non-human actors. | user_id, email, display_name, actor_type, account_status, email_verified_at, created_at, updated_at, last_login_at, created_by, disabled_reason, version |
| IDENTITY_CREDENTIAL | Stores authentication method metadata and secrets separately from the user identity. | credential_id, user_id, provider, credential_type, credential_ref, status, created_at, last_used_at |
| ROLE | Defines reusable roles such as COLLECTOR, BRAND_VIEWER, BRAND_EDITOR, BRAND_ADMIN, PLATFORM_ADMIN, SUPPORT_AGENT, COMPLIANCE_OFFICER, and SERVICE_ACCOUNT. | role_id, role_name, description, role_type, is_privileged |
| PERMISSION | Defines fine-grained actions that can be granted through roles. | permission_id, permission_key, resource_type, action, description |
| ROLE_PERMISSION | Maps roles to permissions. | role_permission_id, role_id, permission_id |
| ROLE_ASSIGNMENT | Connects users to roles within explicit platform, brand, collector, support, compliance, or service scopes. | role_assignment_id, user_id, role_id, scope_type, scope_id, status, assigned_at, assigned_by |

Design rule: The PLATFORM_USER table answers who the actor is, while ROLE_ASSIGNMENT answers what the actor can do and in which scope. Roles should not be stored directly as a single field on the user because one user may be a collector, a brand admin for one brand, and a viewer for another brand at the same time.

##### 2.2.2.1 PLATFORM_USER Table Design

The PLATFORM_USER table is the core identity table. It represents a login-capable actor in the system before that actor is mapped to a business object such as collector, brand membership, platform admin, support user, compliance user, service account, or system actor.

Core principle: PLATFORM_USER answers “Who authenticated?” while collector, brand membership, role assignment, and service-account tables answer “What business profile does this identity represent?” and “What is this identity allowed to do?”

| Field | Purpose | Notes |
|---|---|---|
| user_id | Immutable internal identity key. | Use in tokens, audit events, role assignments, and domain mappings. Prefer UUID or stable platform-generated ID. |
| email | Human login email or primary contact email. | Unique for human login accounts, normalized to lowercase, nullable for service accounts, and not used as the primary identifier. |
| display_name | Identity-facing display name for admin, audit, and support views. | Separate from public collector nickname or brand-facing display profile. |
| actor_type | Classifies the identity type. | Recommended values: COLLECTOR, BRAND_USER, PLATFORM_ADMIN, SUPPORT_USER, COMPLIANCE_USER, SERVICE_ACCOUNT, SYSTEM. Actor type is not the same as role. |
| account_status | Controls whether the identity may authenticate. | Recommended values: PENDING_VERIFICATION, ACTIVE, LOCKED, DISABLED, SUSPENDED, DELETED. |
| email_verified_at | Timestamp proving email verification. | Null means the email is not verified. |
| created_at / updated_at | Lifecycle timestamps. | Used for auditing, lifecycle management, reporting, and operational traceability. |
| last_login_at | Timestamp of the latest successful authentication. | Used for security monitoring, inactive-account detection, and support investigation. |
| created_by | Identifies who or what created the identity. | Useful for self-registration, brand user onboarding, admin-created accounts, and service account creation. |
| disabled_reason | Explains why an account is locked, disabled, or suspended. | Supports support workflows, risk review, and audit analysis. |
| version | Optimistic-lock field. | Prevents concurrent identity updates from overwriting each other. |

Do not store in PLATFORM_USER: password hashes, MFA secrets, OAuth provider secrets, roles as direct columns, brand IDs, collector profile details, KYC data, vault preferences, custody references, wallet addresses, ADC ownership state, or blockchain token state. These belong in dedicated credential, role assignment, collector, custody, ownership, or blockchain tables.

##### 2.2.2.2 IDENTITY_CREDENTIAL Table Design

The IDENTITY_CREDENTIAL table stores authentication method metadata for a Platform User. One Platform User may have multiple credentials over time, such as local password login, social login, brand SSO, MFA factors, service credentials, or future passkeys. This separation keeps authentication details out of PLATFORM_USER while allowing credential types to evolve independently.

IDENTITY_CREDENTIAL should store credential metadata, safe references, and hashed or encrypted authentication material only. Raw secrets, plaintext passwords, private keys, OAuth client secrets, TOTP seeds, and service account secrets should not be stored directly in this table unless protected through an approved secret-management and encryption model.

| Field | Purpose | Notes |
|---|---|---|
| credential_id | Immutable identifier for one authentication credential. | Primary key for the credential record. A user may have multiple credential records. |
| user_id | References the Platform User that owns the credential. | Creates the relationship platform_user 1 to 0..* identity_credential. |
| provider | Identifies the source of the credential. | Recommended values: LOCAL, GOOGLE, APPLE, BRAND_SSO, SERVICE_CREDENTIAL, PASSKEY. |
| credential_type | Classifies the credential mechanism. | Recommended values: PASSWORD, OIDC, SAML, MFA_FACTOR, API_CLIENT, PASSKEY. |
| provider_subject | External user or subject identifier from an identity provider. | Used for OIDC or SAML identities. Null for local password credentials unless an external provider is involved. |
| credential_ref | Safe reference to stored secret material or external credential metadata. | May point to a secret manager record, passkey credential ID, IdP configuration, or service-client reference. |
| password_hash | Secure password hash for local password credentials. | Only populated for LOCAL PASSWORD credentials. Never store plaintext passwords. |
| password_algorithm | Records which hashing algorithm produced the password hash. | Use to support upgrades and migration. Recommended value for new credentials: ARGON2ID. |
| status | Lifecycle state of the credential. | Recommended values: PENDING_VERIFICATION, ACTIVE, LOCKED, REVOKED, EXPIRED, DISABLED. |
| created_at / updated_at / last_used_at | Lifecycle and latest-use timestamps. | Used for audit, credential rotation, suspicious activity detection, and inactive-credential cleanup. |
| expires_at / revoked_at / revoked_reason | Expiration and revocation metadata. | Useful for temporary service credentials, compromised credentials, recovery flows, and security incident response. |
| version | Optimistic-lock field. | Prevents concurrent credential changes from silently overwriting each other. |

#### 2.2.3 Token and Session Design

- Use short-lived access tokens and refresh tokens where needed.
- Include stable claims for user_id, actor_type, role names, permission keys, scope_type, scope_id, authentication strength, and correlation ID.
- Do not expose credential details, password hashes, MFA secrets, custody references, or private operational data in tokens.
- Require step-up authentication for privileged operations such as template approval, ADC issuance, freeze, recovery, custody changes, and role administration.

#### 2.2.4 Authorization Rules

- Collectors may access only their own collector account, vault, ownership records, and eligible public ADC information.
- Brand users may access only collections, templates, issuance workflows, and reports inside their assigned brand scope.
- Platform admins require privileged permissions for cross-brand configuration and platform-level administration.
- Support and compliance users require purpose-specific permissions and should receive limited, auditable access.
- Service accounts must be explicitly scoped and must not be treated as collector or brand human users.

#### 2.2.5 Brand Editor Permission Specification

The BRAND_EDITOR role prepares and maintains collections, collectible templates, metadata, traits, and media for an assigned Brand. The role is Brand-scoped and may validate, preview, and submit content for review, but may not approve, publish, issue, mint, administer users, or perform asset-lifecycle operations.

| Brand Editor — Collection and Collectible Template Permissions |
|---|
| Permission Key | Allowed Operation |
| collection:read | View collections belonging to the assigned Brand. |
| collection:create | Create a collection in DRAFT state. |
| collection:update | Edit collection details while the collection is in DRAFT or CHANGES_REQUESTED. |
| collection:delete | Delete a draft collection only when it has no issued ADCs and has not been submitted for review. |
| collection:submit_for_review | Submit a completed collection for approval. |
| collection:withdraw_from_review | Return a submitted collection to DRAFT before review is completed. |
| collectible_template:read | View collectible templates within the assigned Brand. |
| collectible_template:create | Create a template under a Brand-owned collection. |
| collectible_template:update | Edit a template in DRAFT or CHANGES_REQUESTED state. |
| collectible_template:delete | Delete a draft template that has not produced any ADCs. |
| collectible_template:clone | Create a new draft template from an existing Brand template. |
| collectible_template:submit_for_review | Submit a completed template for approval. |
| collectible_template:withdraw_from_review | Return a submitted template to DRAFT before approval. |
| collectible_template:view_validation_results | View schema, metadata, supply, and media validation results. |

| Brand Editor — Metadata, Media, Review, and Reporting Permissions |
|---|
| Permission Key | Allowed Operation |
| collectible_metadata:read | View template metadata and metadata versions. |
| collectible_metadata:create | Create metadata for a draft template. |
| collectible_metadata:update | Modify metadata before template approval. |
| collectible_metadata:validate | Run metadata-schema and business-rule validation. |
| collectible_trait:manage | Add, edit, reorder, or remove traits on a draft template. |
| rarity:assign | Assign an allowed rarity classification to a draft template. |
| edition_size:propose | Define or change the proposed edition size before approval; this does not authorize approval or post-approval increases. |
| media_asset:read | View Brand media assets. |
| media_asset:upload | Upload artwork, images, video, animation, audio, thumbnails, or 3D assets. |
| media_asset:update_metadata | Edit media title, description, type, usage, and accessibility metadata. |
| media_asset:replace | Replace media attached to a DRAFT or CHANGES_REQUESTED template. |
| media_asset:delete | Delete an unused asset not referenced by an approved or issued collectible. |
| media_asset:attach / media_asset:detach | Add or remove media references on draft collections or templates. |
| media_asset:view_validation_results | View file-integrity, format, size, malware-scan, and processing results. |
| review_status:read | View submission, review, approval, and rejection status. |
| review_comment:read / review_comment:create | View reviewer comments and add responses or clarifications. |
| change_request:resolve | Update content and mark requested changes as addressed. |
| content_preview:read | Preview how a collection or template will appear to collectors. |
| catalog_report:read | View catalog completeness and validation reports for the assigned Brand. |
| issuance_summary:read | View aggregate issuance counts for Brand templates. |
| audit_event:read_own_actions | View audit events generated by the editor’s own actions. |

##### Explicitly Excluded Permissions

The BRAND_EDITOR role must not include: collection:approve, collection:publish, collection:retire, collectible_template:approve, collectible_template:publish, collectible_template:retire, edition_size:approve, edition_size:increase_after_approval, adc:create, adc:issue, adc:mint, adc:assign, adc:freeze, adc:recover, adc:burn, digital_passport:approve, registry_entry:create, blockchain_transaction:submit, custodial_wallet:manage, brand_user:create, brand_user:update, brand_user:disable, role:assign, permission:grant, brand_governance:update, brand:activate, brand:suspend, or brand:revoke.

##### Scope and Enforcement Rules

- Evaluate every permission with scope_type = BRAND and the assigned brand_id.
- Allow access only to collections, templates, metadata, and media owned by that Brand.
- Make approved or published templates read-only unless an authorized change workflow moves them to CHANGES_REQUESTED.
- Prevent the editor from approving content they created or modified.
- Prevent the editor from triggering ADC creation, blockchain minting, ownership assignment, custody operations, or post-approval supply increases.
- Generate an audit event for every create, update, delete, upload, submission, and withdrawal action.
- Block destructive operations when an object is approved, published, referenced by an ADC, or required for provenance.
- Require recent authentication for submission to approval; approval and issuance remain separate privileged operations.

#### 2.2.6 Platform Administrator Permission Specification

The PLATFORM_ADMIN role provides platform-wide governance and operational administration. It manages Brand onboarding and lifecycle, platform identities, scoped role assignments, approved platform configuration, cross-Brand operational visibility, and administrative audit access. It is not an unrestricted superuser and must not normally create Brand catalog content, issue ADCs, operate custody assets, submit blockchain transactions, or perform exceptional asset-lifecycle actions.

| Platform Administrator — Brand Governance Permissions |
|---|
| Permission Key | Allowed Operation |
| BRAND_APPLICATION:READ | View submitted Brand applications and supporting evidence. |
| BRAND_APPLICATION:REVIEW | Start and manage an application review. |
| BRAND_APPLICATION:REQUEST_INFORMATION | Request missing or additional evidence from an applicant. |
| BRAND_APPLICATION:APPROVE | Approve a verified Brand application with a recorded decision. |
| BRAND_APPLICATION:REJECT | Reject an application with a recorded reason. |
| BRAND:READ | View any Brand and its platform status. |
| BRAND:CREATE | Create a Brand tenant only from an approved Brand application. |
| BRAND:UPDATE | Update platform-controlled Brand information. |
| BRAND:ACTIVATE | Authorize an approved Brand to operate. |
| BRAND:SUSPEND | Temporarily block Brand operations. |
| BRAND:REINSTATE | Restore an eligible suspended Brand. |
| BRAND:REVOKE | Permanently remove Brand authorization through a controlled workflow. |
| BRAND_GOVERNANCE:READ | View Brand governance and approval policies. |
| BRAND_GOVERNANCE:UPDATE | Configure approved platform-enforced Brand policies. |

| Platform Administrator — Identity Administration Permissions |
|---|
| Permission Key | Allowed Operation |
| USER:READ | View platform identity and account status. |
| USER:CREATE | Create approved internal administrative, support, or compliance identities. |
| USER:UPDATE | Update platform-controlled identity attributes. |
| USER:ENABLE / USER:DISABLE | Enable an authorized account or prevent it from authenticating. |
| USER:UNLOCK | Unlock an account after the required verification. |
| USER:REVOKE_SESSIONS | Terminate active sessions and refresh tokens. |
| ROLE:READ | View available roles and their permission composition. |
| ROLE_ASSIGNMENT:READ | View platform- and Brand-scoped role assignments. |
| ROLE_ASSIGNMENT:CREATE | Assign an allowed role within an authorized scope. |
| ROLE_ASSIGNMENT:REVOKE | Remove a role assignment through an audited workflow. |
| SERVICE_ACCOUNT:READ | View service-account metadata and operational status. |
| SERVICE_ACCOUNT:CREATE | Create an approved non-human identity with explicit scope. |
| SERVICE_ACCOUNT:DISABLE | Disable a service account. |
| SERVICE_CREDENTIAL:ROTATE | Initiate credential rotation without exposing the secret. |

| Platform Administrator — Operational Visibility and Configuration Permissions |
|---|
| Permission Key | Allowed Operation |
| COLLECTION:READ | View collections across Brands for platform administration. |
| TEMPLATE:READ | View collectible templates across Brands. |
| ADC:READ | View ADC lifecycle and operational state. |
| ISSUANCE:READ | View issuance requests and results. |
| REGISTRY_ENTRY:READ | View registry records. |
| BLOCKCHAIN_TRANSACTION:READ | View blockchain submission and reconciliation status. |
| CUSTODIAL_WALLET:READ_METADATA | View non-secret wallet metadata and operational status. |
| PLATFORM_REPORT:READ | View platform-wide operational reports. |
| AUDIT_EVENT:READ | View administrative audit events across platform scopes, subject to purpose and data-minimization controls. |
| PLATFORM_CONFIGURATION:READ | View platform-level configuration. |
| PLATFORM_CONFIGURATION:UPDATE | Modify approved non-secret platform settings. |
| FEATURE_FLAG:READ / FEATURE_FLAG:UPDATE | View or change approved capability rollout state. |
| REFERENCE_DATA:MANAGE | Manage controlled values such as supported countries, asset types, and statuses. |
| INTEGRATION_CONFIGURATION:READ | View integration status and non-secret configuration. |
| MAINTENANCE_MODE:MANAGE | Enable or disable controlled maintenance mode. |

##### Explicitly Excluded Permissions

The normal PLATFORM_ADMIN role must not include: COLLECTION:CREATE, COLLECTION:UPDATE, TEMPLATE:CREATE, TEMPLATE:UPDATE, TEMPLATE:APPROVE, ADC:ISSUE, ADC:MINT, ADC:ASSIGN, BLOCKCHAIN_TRANSACTION:SUBMIT, CUSTODIAL_WALLET:CREATE, CUSTODIAL_WALLET:TRANSFER, CUSTODIAL_WALLET:EXPORT_KEY, OWNERSHIP:TRANSFER, ADC:FREEZE, ADC:RECOVER, ADC:BURN, AUDIT_EVENT:DELETE, AUDIT_EVENT:UPDATE, SECRET:READ, SIGNING_KEY:READ, PERMISSION:CREATE, or unrestricted ROLE:UPDATE. The permission catalog should be version-controlled and deployed with the application rather than manufactured at runtime by an administrator.

##### Separation-of-Duties Roles

- PLATFORM_SECURITY_ADMIN: manages identity security, privileged role assignments, authentication policy, session revocation, and service credentials.
- PLATFORM_COMPLIANCE_ADMIN: manages licensing evidence, investigations, compliance holds, disputes, and sensitive compliance audit access.
- PLATFORM_OPERATIONS_ADMIN: monitors integrations, failed workflows, reconciliation, controlled retries, and maintenance mode without changing business approvals.
- PLATFORM_ASSET_OPERATOR: performs exceptional freeze, recovery, burn, or ownership-correction actions under case-based and dual-approval controls.
- PLATFORM_SUPER_ADMIN: is a break-glass role only, requiring MFA, time-limited activation, independent approval, immutable auditing, and automatic expiration.

##### Scope and Enforcement Rules

- Assign PLATFORM_ADMIN with scope_type = PLATFORM and a null or explicit platform scope identifier.
- A platform-scoped role may operate across Brands only for explicitly granted permissions; it does not inherit all Brand-scoped capabilities.
- Require MFA and recent authentication for Brand activation, suspension, revocation, privileged role assignment, service-account creation, and configuration changes.
- Require an independent approver for Brand revocation, privileged administrator assignment, and other high-impact actions.
- Record immutable audit events containing actor, permission, scope, reason, affected object, before/after state, timestamp, and correlation ID.
- Limit cross-Brand access to a documented administrative purpose and minimize exposure of collector personal data.
- Keep secrets, custody credentials, signing keys, and production infrastructure access outside the role and in dedicated secret-management and operational systems.
- Use a separate time-limited break-glass role for emergency recovery rather than expanding the standing PLATFORM_ADMIN grant.

#### 2.2.7 Identity API Deliverables

- Registration API for collector self-registration.
- Login, refresh, logout, and account-status APIs.
- Role assignment and permission administration APIs for privileged users.
- Token introspection or validation support for API Gateway and Platform API modules.
- Identity-to-domain mapping APIs for Collector, Brand Membership, Platform Admin, Support Agent, Compliance User, and Service Account contexts.
Design rule: Identity Service owns authentication, credentials, tokens, roles, and platform identity mapping. It should not own collector profile details, brand governance rules, ADC lifecycle state, wallet operations, or blockchain token state. Those remain owned by the relevant business services.

#### 2.2.8 Acceptance Criteria

- A collector can register, log in, receive a valid token, and access only their own account context.
- A brand user can authenticate and access only resources associated with their assigned brand scope.
- A platform admin can manage brands and identity configuration only through privileged permissions.
- A service account can call internal APIs using service credentials without being treated as a human collector or brand user.
- Unauthorized cross-brand access is denied and written to the audit trail.
- Privileged actions require the expected role, permission, and authentication strength.
- All sensitive identity events are traceable using actor ID, actor type, source service, timestamp, object reference, and correlation ID.

#### 2.2.9 Dependencies and Next Artifacts

This milestone should be completed before implementing brand collection creation, collectible template approval, ADC issuance, vault access, custody wallet creation, or blockchain proof registration. The next implementation artifact should be an Identity API contract and a concrete RBAC permission matrix for the first MVP workflows.

### 2.3 Architecture Style and Core Decisions

The platform will be implemented as a modular platform, not as a large set of independent microservices from day one. The system is divided into clear bounded contexts: identity, brand management, catalog, ADC lifecycle, ownership, custody, blockchain registry, marketplace, trading, notification, and audit. This structure keeps the first implementation manageable while preserving a clean path to extract high-scale or operationally complex modules into independent services later.

For the first version, the selected implementation model is a modular monolith with strong internal boundaries, supported by a small number of separately deployable technical services where separation is operationally justified. The initial deployment will group the business modules inside the Platform API and separate infrastructure-oriented capabilities such as Blockchain/Custody integration and Notification/Audit processing when needed. This reduces operational complexity while still supporting future scalability.

| Design Decision | Conclusion |
|---|---|
| Architecture style | Use a modular platform with strong bounded contexts rather than many independent microservices in the first release. |
| MVP deployment model | Start with a Platform API that contains the main business modules, plus separate workers or services for blockchain/custody operations and notification/audit processing where needed. |
| Service extraction strategy | Extract modules into independent services only when scale, security, operational isolation, or delivery ownership justifies the added complexity. |
| Domain ownership | Keep service boundaries aligned with the domain model: Brand, Catalog, ADC, Collector, Custody, Blockchain, Marketplace, Trading, Notification, and Audit. |
| Blockchain role | Use blockchain as the verification and proof layer, while keeping user experience, catalog, media, trading logic, custody policy, and recovery workflows off-chain. |

### 2.5 Collector and ADC Domain Separation

The Collector Service and ADC Service should remain separate because they own different parts of the domain. The Collector Service owns the person or account side of the platform, while the ADC Service owns the issued collectible lifecycle, authenticity state, and asset state.

| Service | Primary Responsibility | Owns | Does Not Own |
|---|---|---|---|
| Collector Service | Manages the collector account, profile, eligibility, account status, KYC status, preferences, and onboarding lifecycle. | Collector identity, account status, KYC status, eligibility rules, account preferences, and collector profile. | ADC lifecycle, mint status, ownership history, blockchain token state, or custody wallet operations. |
| ADC Service | Manages the ADC lifecycle and coordinates the off-chain state of each issued authenticated digital collectible. | ADC, mint status, asset status, authenticity state, Digital Passport linkage, Registry Entry linkage, freeze state, recovery state, ownership record coordination, provenance, and blockchain token linkage. | Collector profile, collector account settings, KYC decisioning, login identity, or personal account preferences. |

In simple terms, the Collector Service answers “who is the collector and is the collector eligible?”, while the ADC Service answers “what is this ADC and what is its lifecycle state?”. During issuance, the ADC Service should call the Collector Service to validate that the target collector exists and is eligible before resolving or creating a custodial wallet.

### 2.6 Blockchain Proof Model

The on-chain token is not the complete TCG card data and should not be treated as the primary container for artwork, game attributes, rarity metadata, media files, or brand-specific collectible content. In this architecture, the on-chain token is the public blockchain proof object that represents one issued ADC and connects it to a verifiable ownership and authenticity layer.

The ADC remains the business asset that collectors understand. The Digital Passport and Registry Entry contain the rich collectible meaning, including the TCG identity, edition details, authenticity status, history, and metadata references. The blockchain token provides an independent proof that a specific ADC was officially minted, assigned to a wallet, and can be verified outside the platform database.

- Existence proof: the token proves that a specific ADC was issued by the official smart contract.
- Ownership proof: the token identifies the wallet that currently holds the blockchain representation of the ADC.
- Authenticity proof: the token is valid only if it was minted by the authorized platform or brand contract.
- Off-chain data anchor: the token can reference or hash the Digital Passport, Registry Entry, or metadata payload so off-chain data can be verified against a known proof.
- Independent verification: external parties can verify that the token exists, belongs to a wallet, and came from the official contract without relying only on the platform database.
Recommended on-chain or token-associated fields include contract address, token ID, chain, owner wallet address, mint transaction hash, metadata URI or registry URI, and metadata or authenticity hash. The actual TCG data, card artwork, rarity rules, edition logic, media files, collector profile, Digital Passport, Registry Entry, trading history, recovery state, and custody mapping remain off-chain under platform control.

Design rule: The on-chain token should be interpreted as the official, verifiable blockchain proof that a specific ADC exists, is authentic, and is currently assigned to a wallet. It does not need to contain the full TCG collectible data in order to provide value.

### 2.7 Digital Passport Versioning Model

The Digital Passport should be modeled as a mutable business concept with immutable versions. A hash does not contain the Digital Passport; it is a cryptographic fingerprint of one specific passport snapshot. Therefore, when the passport changes over time, the platform must create a new passport version with a new snapshot URI and snapshot hash.

The recommended model is to keep the current Digital Passport as the collector-facing profile, while preserving each historical version as an immutable snapshot. The blockchain token may reference the initial passport snapshot, the current passport snapshot, or a registry checkpoint hash, but it should not be treated as the full container of the evolving passport data.

| Concept | Recommended Meaning |
|---|---|
| DigitalPassport | The current collector-facing profile of the ADC, including current status, current version pointer, and latest snapshot reference. |
| DigitalPassportVersion | An immutable snapshot of the Digital Passport at a specific point in time, with its own version number, snapshot URI, snapshot hash, creation metadata, and change reason. |
| snapshotHash | A fingerprint of one immutable passport version, not the full evolving passport object. |
| registryCheckpointHash | A periodic root hash that can anchor multiple passport updates on-chain without writing every update directly to the blockchain. |

### 2.8 Domain Objects

The most important design decision is to separate the product concept from the technical implementation. An Authenticated Digital Collectible (ADC) is the business object that collectors understand and own. A Collectible Template defines what can be issued. A Blockchain Token is the on-chain proof used to verify authenticity, ownership, and provenance.

| Brand: the licensed IP owner, publisher, or rights holder that controls product identity, commercial authorization, collectible governance, brand users, approval rules, and the right to create official collections on the platform.<br>Collection: a brand-managed release, set, season, campaign, or product drop that groups related collectible templates under one release context, including release timing, publishing status, eligibility rules, and commercial governance.<br>Collectible Template: the reusable blueprint for a collectible type before issuance. It defines the card or item identity, rarity, edition size, metadata URI, media asset reference, trait model, publishing status, and validation rules used before ADCs can be minted.<br>Media Asset: the platform object that represents the artwork, image, video, animation, thumbnail, or other media file used by a template or passport. It stores media-specific details such as storage URI, CDN URI, asset type, content information, and checksum. It should not own metadataHash; metadata and passport hashes belong to metadata, registry, or passport version objects.<br>ADC / Authenticated Digital Collectible: the specific issued asset created from an approved template. This is the business object owned by a collector and linked to serial number, lifecycle state, Digital Passport, Registry Entry, Ownership Record, Custodial Wallet, and optional Blockchain Token proof.<br>Collector: the platform account that receives, owns, views, verifies, trades, or transfers ADCs. The collector owns account-level information such as profile, account status, preferences, KYC status, and eligibility state, but does not directly own blockchain key material in the MVP.<br>Custodial Wallet: the blockchain wallet managed by the platform or custody provider on behalf of a collector. It holds the ERC-721 token while abstracting private-key management, recovery, provider account mapping, and wallet operational status away from mainstream collectors.<br>Ownership Record: the internal ledger record that links an ADC to a collector and custodial wallet over a valid time range. It is the platform source of truth for ownership history, acquisition type, current ownership status, valid-from/valid-to periods, and transfer or recovery transitions.<br>Blockchain Token: the ERC-721 on-chain proof object representing the issued ADC. It provides public proof of token identity, contract origin, owner wallet, mint transaction, and selected hash anchors, but it does not contain the full TCG data, media files, collector profile, or evolving Digital Passport content.<br>Blockchain Event: a normalized off-chain record of an observed on-chain event such as mint confirmation, transfer, freeze, recovery, burn, or checkpoint anchoring. It supports reconciliation, ordering, failure recovery, audit analysis, and timeline enrichment.<br>Digital Passport: the current collector-facing profile of the ADC. It exposes the latest authenticity state, current version pointer, latest snapshot URI, latest passport hash, operational status, and curated business view of the collectible.<br>Digital Passport Version: an immutable snapshot of the Digital Passport at a specific point in time. Each version records its version number, snapshot URI, snapshot hash, change reason, creation actor, timestamp, and optional checkpoint linkage.<br>Registry Entry: the official platform registration record for an ADC. It verifies that the asset belongs to an approved brand, collection, template, edition, serial number, and authenticity context, and provides the platform-side registry identity used for verification.<br>Registry Checkpoint: a batch, root-hash, or checkpoint record that anchors one or more registry and passport updates. It allows the platform to prove later off-chain changes without writing every Digital Passport update directly to the blockchain.<br>Marketplace Listing: a commercial listing that exposes an eligible ADC for sale or trade. It defines seller, asking price, currency, listing state, expiration policy, reservation state, and eligibility validation without becoming the ownership source of truth.<br>Transaction: the business record for a purchase, sale, trade, primary issuance, transfer, or reservation workflow. It coordinates buyer, seller, ADC, listing reference, payment state, ownership transition, and settlement state.<br>Settlement Record: the financial or operational settlement result for a sale, trade, transfer, or primary issuance. It captures provider references, gross amount, fee amount, net amount, currency, payment status, and final settlement timestamp.<br>Vault Item: the collector-facing read model that describes how an owned ADC appears inside the collector’s Vault. It includes display status, favorite flag, sort order, visibility, acquired-at timestamp, and links back to the authoritative ownership record.<br>History Event: a normalized business event describing something that happened to an ADC or collector, such as issuance, assignment, sale, transfer, freeze, recovery, passport update, display event, or registry update.<br>ADC Timeline: a read-optimized chronological view assembled from ownership records, transactions, blockchain events, passport versions, registry updates, marketplace activity, and audit events. It supports collector, brand, support, and compliance history views.<br>Audit Event: an immutable operational and security record that captures who performed an action, what object was affected, when it happened, which service produced it, and under which correlation context.<br>Field Reference and Relationships<br>This section expands the compact domain model into implementation-oriented object definitions. Each object includes its recommended fields, field-level meaning, and design notes for unresolved structural choices such as on-chain anchoring, off-chain ownership, versioning, custody, marketplace state, and auditability.<br>Brand<br><br>Brand Onboarding and Approval Model<br>A Brand represents a trusted issuer within the platform and must be treated as a privileged business entity rather than a standard user account. Brands can create collections, issue Authenticated Digital Collectibles, define scarcity and governance rules, manage collector experiences, and control official digital asset ecosystems. For this reason, Brand creation must not be exposed as an unrestricted public self-service action.<br>The platform should support public user registration, but Brand activation should follow an approval workflow. A user may submit a Brand Application, but only a Platform Administrator may approve the application and create or activate the Brand tenant.<br>Registration Strategy<br>Collector registration is public and self-service.<br>Brand registration is submitted as an application.<br>Brand creation and activation require Platform Administrator approval.<br>A Brand entity should be created only after the application is approved.<br>Brand Application Process<br>Any authenticated user may submit a Brand Application. The application should capture company name, brand name, corporate website, business email address, contact person, country of operation, brand description, licensing or ownership information, and optional supporting documentation. After submission, the application enters a review state and cannot issue collectibles until approved.<br>Platform Administration Responsibilities<br>Approve Brand applications.<br>Reject Brand applications.<br>Request additional information from applicants.<br>Suspend Brand operations when required.<br>Revoke Brand authorization for legal, compliance, fraud, or governance reasons.<br>Brand Application Lifecycle<br>DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED → BRAND_CREATED → ACTIVE<br>UNDER_REVIEW → REJECTED<br>ACTIVE → SUSPENDED → REVOKED<br>Brand Verification Status<br><br>Operational Restrictions<br>Only Brands with licenseStatus set to ACTIVE may create collections, publish collections, issue ADCs, register Digital Passports, mint blockchain-backed assets, configure marketplace integrations, or manage collector-facing collectible experiences.<br>Architectural Rationale<br>The platform intentionally separates BrandApplication from Brand. The BrandApplication captures the request, evidence, review status, and decision history. The Brand entity represents an approved issuer tenant and should exist only after successful approval. This reduces legal, trademark, fraud, and reputation risk while preserving a smooth onboarding path for legitimate brands.<br>Collection<br><br>Collectible Template<br><br>Media Asset<br><br>Design note: media checksums should prove the integrity of files only. Metadata, passport, and registry hashes should remain separate because they represent business snapshots rather than binary media files.<br>ADC / Authenticated Digital Collectible<br><br>Collector<br><br>Custodial Wallet<br><br>Ownership Record<br><br>Blockchain Token: On-Chain Proof Object<br>The BlockchainToken is the ERC-721 representation of the issued ADC on the selected blockchain network. It is not the Digital Passport and does not contain the full TCG object, artwork, marketplace state, collector profile, or evolving business history. Its primary role is to provide public, independently verifiable proof that the ADC was minted by the official contract and is currently held by a specific wallet.<br><br>Open design decision: decide whether passport hash updates are written directly to contract storage, emitted only as blockchain events, or batched as periodic registry checkpoints. For the ADC platform, the recommended approach is to keep the original issuance proof immutable and anchor later Digital Passport versions through checkpoint events or registry roots, rather than overwriting token metadata.<br>Blockchain Event<br><br>Digital Passport: Off-Chain Business Object<br>The Digital Passport is the collector-facing representation of the ADC’s identity, authenticity, and lifecycle. It is mutable as a business object because the current view can change after ownership transfers, recovery events, registry updates, display events, or brand-approved metadata corrections. However, it must be immutable as a sequence of versions: every change creates a new DigitalPassportVersion with its own snapshot hash.<br><br>Digital Passport Version: Immutable Snapshot<br><br>Registry Entry<br><br>Marketplace Listing<br><br>Design note: marketplace listings should never become the ownership source of truth. Ownership must remain governed by OwnershipRecord and confirmed through custody/blockchain reconciliation.<br>Transaction<br><br>Settlement Record<br><br>Vault Item<br><br>History Event<br><br>ADC Timeline<br><br>Audit Event<br>Design rule: the Digital Passport is mutable only through append-only version creation. The current passport row may point to the latest version, but previous versions must never be overwritten or deleted because they are part of the ADC provenance trail.<br>On-Chain vs. Off-Chain Responsibility Split<br>Missing Supporting Objects<br><br>Domain Relationships and Cardinalities<br><br><br><br> |
|---|

### 2.10 Product Terminology and Hierarchy

The terms Collection, Collectible Template, Collectible, and ADC must be used consistently because they describe different levels of the product hierarchy. A Collection is a brand-managed release or set. A Collectible Template is the blueprint for a collectible type inside that collection. An ADC is a specific issued authenticated digital collectible created from a template and assigned to a collector.

| Term | Meaning | Example | Owned By |
|---|---|---|---|
| Brand | The company or IP owner that controls the product identity, artwork, release strategy, and governance rules. | Pokémon, One Piece, Marvel, NBA | Brand domain |
| Collection | A brand-managed grouping of collectibles, usually representing a release, series, season, drop, set, or campaign. | Pokémon 151, One Piece Genesis Set, Marvel Heroes Series 1 | Catalog Service |
| Collectible Template | The definition or blueprint of a collectible type that can be issued as one or more ADCs. | Charizard Holographic, Luffy Legendary Card, Spider-Man Rare Variant | Catalog Service |
| ADC | A specific issued Authenticated Digital Collectible created from a template. This is the asset that can be assigned to a collector, verified, traded, frozen, recovered, and represented by blockchain proof. | Charizard Holographic #0042 owned by Collector A | ADC Service |
| Collectible | A generic product-language term. In technical design, it should be qualified as Collection, Collectible Template, or ADC to avoid ambiguity. | “Collectible” may refer to the general item experience, but the model should use more precise terms. | Context-dependent |

| Recommended Hierarchy<br>Brand<br> └── Collection<br>      └── Collectible Template<br>           └── ADC |
|---|

#### Examples

| Brand | Collection | Collectible Template | ADC Example |
|---|---|---|---|
| Pokémon | Pokémon 151 | Charizard Holographic | Charizard Holographic #0042 assigned to Collector A |
| One Piece | One Piece Genesis Set | Luffy Legendary Card | Luffy Legendary Card #0187 assigned to Collector B |
| Marvel | Marvel Heroes Series 1 | Spider-Man Rare Variant | Spider-Man Rare Variant #0009 assigned to Collector C |
| NBA | World Cup 2026 Legends | Legendary Player Signature Card | Legendary Player Signature Card #1024 assigned to Collector D |

In these examples, the Collection is the brand-managed release, the Collectible Template is the reusable definition inside that release, and the ADC is the unique issued asset that appears in a collector’s Vault with its own serial number, ownership record, Digital Passport, Registry Entry, custody reference, and blockchain proof.

Design rule: A collector owns ADCs, not Collections or Collectible Templates. Collections and templates are brand/catalog definitions. ADCs are the issued assets that appear in the collector’s Vault and are linked to ownership records, Digital Passports, Registry Entries, custodial wallets, and blockchain proof.

#### 2.11 MVP Functional Capabilities

- Brand creates a collection.
- Brand uploads collectible metadata and artwork.
- Platform stores media in Amazon S3 and delivers it through Amazon CloudFront.
- Platform stores collectible templates and ADCs in Amazon Aurora PostgreSQL or Amazon RDS.
- Blockchain Service registers the ADC’s blockchain proof on Polygon.
- Custody Service assigns the asset to a collector account using Fireblocks.
- Collector views the collectible in the portal or mobile application.
- Collector verifies authenticity, ownership, and provenance.

#### 2.12 Design Deliverables and Next Implementation Artifacts

- Create the high-level domain model diagram.
- Document the internal module boundaries for the Platform API and identify which modules require separate deployment in the MVP.
- Design the main sequence flows: create collection, mint collectible, assign collectible, verify ownership, and transfer ownership.
- Draft the initial database schema for brands, collections, collectible templates, ADCs, digital passports, registry entries, ownership records, and blockchain tokens.
- Define the smart contract interface and decide what is stored on-chain versus off-chain.
- Design the custody model, including account mapping, recovery process, freeze operation, and audit trail.
- Convert the MVP scope into a prioritized implementation backlog.
The immediate next step is to validate the finalized modular design through the domain model, module boundaries, and main sequence diagrams before investing effort in database schemas, API contracts, smart contracts, or infrastructure automation.

#### 2.13 Flow: View ADC History and Timeline

| Step | Interaction | Description |
|---|---|---|
| 1 | Collector App / Brand Portal → API Gateway | User requests the history of an ADC, card transaction history, or collector activity timeline. |
| 2 | API Gateway → Identity Service | The platform authenticates the user and validates whether the user is allowed to view the requested history scope. |
| 3 | API Gateway → History / Timeline Service | The request is routed to the timeline read model for ADC history, collector history, or transaction history. |
| 4 | History / Timeline Service → ADC Service | The service resolves ADC identity, current status, Digital Passport reference, and Registry Entry reference. |
| 5 | History / Timeline Service → Ownership / Transaction Sources | The service retrieves ownership records, transaction records, marketplace activity, and transfer events related to the ADC or collector. |
| 6 | History / Timeline Service → Blockchain Service | The service enriches the timeline with blockchain proof events such as token registration, transfer confirmation, freeze, or recovery events. |
| 7 | History / Timeline Service → Audit Service | The service includes auditable lifecycle events that are safe to expose based on the requester’s role and permissions. |
| 8 | History / Timeline Service → API Gateway | The service returns a chronological timeline with event type, timestamp, actor display, source, and public/private visibility rules applied. |
| 9 | API Gateway → Collector App / Brand Portal | The UI renders the ADC history, transaction history, or collector timeline in a user-friendly format. |

Design rule: ADC history must be complete and immutable from a business perspective, but UI visibility must be role-based. Collectors should see public ownership and transaction history, while support, brand admins, and compliance users may see additional operational or security-sensitive events.

#### 2.14 Lifecycle State Reference

| CollectibleTemplate<br>DRAFT → READY_FOR_REVIEW → APPROVED → PUBLISHED → RETIRED | ADC<br>CREATED → PENDING_MINT → MINTED → ASSIGNED → FROZEN → RECOVERED / BURNED |
|---|---|
| BlockchainToken<br>PENDING → MINTED → TRANSFER_PENDING → TRANSFERRED → FROZEN → RECOVERED | MarketplaceListing<br>DRAFT → ACTIVE → RESERVED → SOLD / CANCELLED / EXPIRED |
| OwnershipRecord<br>ACTIVE → TRANSFER_PENDING → TRANSFERRED → REVOKED / RECOVERED | CustodialWallet<br>CREATED → ACTIVE → SUSPENDED → RECOVERY_PENDING → CLOSED |

#### 2.15 Timeline Read Models and History Sources

The platform must support a complete history view for each ADC and each collector. This history should allow collectors, brands, support teams, and compliance users to see how an ADC was created, assigned, transferred, listed, sold, frozen, recovered, displayed, or otherwise updated over time.

| History View | Description | Primary Sources |
|---|---|---|
| ADC History | Shows the full lifecycle of a specific ADC, including issuance, ownership changes, marketplace activity, custody events, blockchain proof updates, freeze/recovery actions, and Digital Passport updates. | ADC, OwnershipRecord, Transaction, BlockchainToken, DigitalPassport, RegistryEntry, AuditEvent, HistoryEvent |
| Transaction History | Shows all commercial or transfer-related activity for an ADC or collector, including primary issuance, sale, trade, transfer, reservation, cancellation, and settlement status. | Transaction, MarketplaceListing, OwnershipRecord, AuditEvent, BlockchainToken |
| Collector History | Shows a collector-facing activity timeline across all owned or previously owned ADCs, including received ADCs, completed sets, trades, marketplace purchases, transfers, recovery events, and Vault activity. | Collector, OwnershipRecord, Transaction, Wallet, AuditEvent, HistoryEvent |
| Digital Passport Timeline | Shows the curated history that becomes part of the ADC’s identity, including authenticity, ownership milestones, display history, special events, Twin Pair status, and notable lifecycle events. | DigitalPassport, HistoryEvent, OwnershipRecord, Transaction, RegistryEntry |

The History / Timeline Service should not become the system of record for ownership, trading, custody, or blockchain state. Instead, it should build read-optimized timeline views from authoritative domain events and source records. This keeps history query performance high while preserving strong ownership boundaries across the platform.

#### 2.16 Aggregate Boundaries

- Brand Aggregate: Brand, brand users, permissions, licensed properties, and governance rules.
- Catalog Aggregate: Collection, collectible template, edition rules, metadata, rarity, traits, and media references.
- ADC Aggregate: ADC, Digital Passport, Digital Passport Version, Registry Entry, ownership record, ownership status, provenance, freeze state, and recovery state.
- Blockchain Aggregate: Blockchain token, contract reference, token status, mint transaction, transfer transaction, and reconciliation events.
- Custody Aggregate: Collector custody account, custodial wallet, custody provider reference, key-management policy, and recovery workflow.
- Marketplace Aggregate: Listing, offer, sale transaction, trade transaction, price, and settlement state.
- History Aggregate: History event, ADC timeline, collector activity timeline, transaction timeline, source event reference, event type, and chronological read model.
- Audit Aggregate: Audit event, actor, action type, object reference, timestamp, source system, and correlation ID.

#### 2.17 Flow: Issue ADC and Register Blockchain Proof

This sequence describes how a licensed brand issues an Authenticated Digital Collectible (ADC), registers its blockchain proof, and assigns it to a collector through the platform’s custodial model. The flow assumes that the collection and collectible template already exist and that the collector account is already registered.

##### 2.17.1 Flow Participants

- Brand Portal: User interface used by the brand to initiate minting.
- API Gateway: Entry point that authenticates and routes the request.
- Brand Service: Validates brand permissions, collection ownership, and minting rights.
- Catalog Service: Validates the collectible template, edition rules, metadata, and media assets.
- Collector Service: Validates that the target collector exists, is active, and is eligible to receive the minted collectible.
- ADC Service: Creates the ADC and owns the off-chain issued asset lifecycle.
- Custody Service: Resolves or creates the collector’s custodial wallet.
- Blockchain Service: Submits the mint transaction to the blockchain and reconciles the result.
- Smart Contract: ERC-721 contract that registers the ADC’s blockchain proof and emits the on-chain event.
- Audit Service: Records important business, security, and compliance events.
- Notification Service: Notifies the collector and brand after the minting flow completes.

##### 2.17.2 Issuance and Minting Sequence

| Step | Interaction | Description |
|---|---|---|
| 1 | Brand Portal → API Gateway | The brand submits an ADC issuance request for a specific collectible template, target collector, and optional edition parameters. |
| 2 | API Gateway → Brand Service | The platform authenticates the brand user and validates that the brand is authorized to mint collectibles for the selected collection. |
| 3 | Brand Service → Catalog Service | The platform validates the collectible template, edition supply rules, metadata completeness, and publishing status. |
| 4 | Catalog Service → ADC Service | The platform requests creation of a new ADC in a pending issuance state. |
| 5 | ADC Service → Collector Service | The platform validates that the target collector exists, is active, and is eligible to receive the collectible. |
| 6 | ADC Service → Custody Service | The platform resolves the target collector’s custodial wallet or creates one if the collector does not yet have an active wallet. |
| 7 | ADC Service → Blockchain Service | The platform sends a blockchain proof registration command containing the ADC ID, initial passport snapshot URI, initial passport snapshot hash, destination custodial wallet, and idempotency key. |
| 8 | Blockchain Service → Smart Contract | The blockchain service submits the ERC-721 registration transaction to the configured blockchain network, such as Polygon. |
| 9 | Smart Contract → Blockchain Service | The blockchain emits a mint event containing the contract address, token ID, transaction hash, owner wallet, and block number. |
| 10 | Blockchain Service → ADC Service | The blockchain service confirms the registration result and updates the ADC with the blockchain token reference. |
| 11 | ADC Service → Ownership Record | The platform creates the initial ownership record linking the collector, ADC, custodial wallet, and blockchain token. |
| 12 | ADC Service → Audit Service | The platform records ADC issuance, validation results, blockchain proof registration, ownership assignment, and final issuance status. |
| 13 | ADC Service → Notification Service | The collector and brand are notified that the ADC was issued, registered, and assigned successfully. |

##### 2.17.3 Text-Based Sequence Diagram

| Brand Portal → API Gateway: submit ADC issuance request<br>API Gateway → Brand Service: authenticate and authorize brand<br>Brand Service → Catalog Service: validate template, edition, metadata, and publishing rules<br>Catalog Service → ADC Service: create ADC as PENDING_ISSUANCE<br>ADC Service → Collector Service: validate target collector eligibility<br>ADC Service → Custody Service: resolve collector custodial wallet<br>ADC Service → Blockchain Service: request blockchain proof registration with idempotency key<br>ADC Service → Digital Passport Version: create immutable initial passport snapshot and hash<br>Blockchain Service → Smart Contract: register ADC proof as ERC-721 token<br>Smart Contract → Blockchain Service: emit mint event with token ID and transaction hash<br>Blockchain Service → ADC Service: confirm token reference<br>ADC Service → Ownership Record: assign initial owner<br>ADC Service → Audit Service: record issuance audit trail<br>ADC Service → Notification Service: notify collector and brand |
|---|

##### 2.17.4 Reliability, Security, and Reconciliation Notes

- Idempotency: Every ADC issuance request should include an idempotency key so retries do not create duplicate blockchain proofs.
- Pending state: The ADC should be created before the blockchain transaction and marked as PENDING_ISSUANCE until confirmation is received.
- Reconciliation: Blockchain events should be monitored independently and reconciled against internal mint records.
- Failure handling: If blockchain proof registration fails, the ADC should move to ISSUANCE_FAILED with a clear retry or cancellation path.
- Auditability: All critical actions should be written to the audit log, including who initiated the issuance, which template was used, which ADC was created, and which wallet received the blockchain token.
- Custody security: The collector should not directly control the private key in the MVP. The blockchain token that represents the ADC should be held in the platform-managed custodial wallet.

### Service Boundaries and Ownership Model

The platform should define clear service ownership boundaries so that each service owns a specific part of the business domain and avoids overlapping data responsibility. The following service catalog describes the recommended responsibilities for the MVP and future scalable architecture.

| Service | Purpose | Main Responsibilities |
|---|---|---|
| Identity Service | Owns authentication, authorization, and platform identity. | Manages login, OAuth2/OIDC flows, MFA, token issuance, session security, RBAC integration, and mapping external identities to internal platform users. |
| Collector Service | Owns the collector account and profile domain. | Manages collector profiles, account status, onboarding, preferences, KYC status, eligibility checks, suspension rules, and collector-facing account metadata. |
| Brand Service | Owns the brand domain and brand-side governance. | Manages brands, brand users, permissions, licensed properties, brand governance rules, and authorization to create collections or issue ADCs. |
| Catalog Service | Owns the collectible definition before an ADC is issued. | Manages collections, collectible templates, edition rules, rarity, traits, metadata requirements, media references, publishing status, and validation of whether a template can be issued. |
| ADC Service | Owns the issued Authenticated Digital Collectible lifecycle. | Creates ADCs from approved templates, manages issuance state, asset status, authenticity status, freeze and recovery state, Digital Passport linkage, Registry Entry linkage, ownership record coordination, and blockchain token linkage. |
| Custody / Wallet Service | Owns the platform-managed custody model for collectors. | Creates or resolves custodial wallets, integrates with Fireblocks or another custody provider, maps collectors to custody accounts, manages wallet status, supports future withdrawal to self-custody, and coordinates custody-related recovery workflows. |
| Blockchain Service | Owns blockchain integration and on-chain proof operations. | Registers ADC blockchain proof, submits ERC-721 transactions, monitors blockchain events, reconciles token state, stores contract and token references, handles idempotent transaction submission, and coordinates freeze, transfer, and recovery operations where supported. |
| Marketplace Service | Owns listing and discovery for collectible trading. | Manages marketplace listings, pricing, listing status, expiration, searchable marketplace views, seller validation, availability checks, and marketplace read models. |
| Trading Service | Owns trade execution and settlement flow. | Coordinates purchase, sale, trade, reservation, payment, settlement, ownership transfer, and transaction status updates. |
| Notification Service | Owns user and brand communication. | Sends transactional notifications for issuance, assignment, sale, transfer, recovery, freeze, failed operations, and security-sensitive events across email, push, and in-app channels. |
| Audit Service | Owns immutable operational and security audit history. | Records business-critical and security-sensitive events, including actor, action type, object reference, timestamp, source service, correlation ID, and investigation metadata for compliance and recovery use cases. |

### Mocked Domain Model Example

The following example shows how the domain model can look with mocked business data. The example follows one brand release, one collectible template, one issued ADC, and the related ownership, custody, registry, passport, and blockchain proof records.

#### Scenario

The brand launches a digital release called Pokémon 151 Digital Genesis. Inside this release, the brand defines a Charizard Holographic collectible template. The platform issues one ADC from that template, assigns it to a collector, stores the blockchain token in a Fireblocks-managed custodial wallet, and creates the Digital Passport and Registry Entry for verification.

#### Mocked Records

| Entity | Mocked Data |
|---|---|
| Brand | brandId: brand-pokemon<br>name: Pokémon<br>licenseStatus: ACTIVE<br>governancePolicyId: gov-pokemon-standard |
| Collection | collectionId: col-pokemon-151-genesis<br>brandId: brand-pokemon<br>name: Pokémon 151 Digital Genesis<br>series: 151<br>releaseStatus: PUBLISHED<br>publishStartDate: 2026-10-01T10:00:00Z |
| Collectible Template | templateId: tpl-charizard-holo-001<br>collectionId: col-pokemon-151-genesis<br>name: Charizard Holographic<br>rarity: LEGENDARY<br>editionSize: 10000<br>metadataUri: s3://adc-media/pokemon-151/charizard/metadata.json<br>mediaAssetId: media-charizard-holo-main<br>publishStatus: APPROVED |
| ADC | adcId: adc-charizard-0042<br>templateId: tpl-charizard-holo-001<br>serialNumber: 42<br>assetStatus: ASSIGNED<br>mintStatus: MINTED<br>freezeStatus: NOT_FROZEN<br>recoveryStatus: NONE<br>productName: Authenticated Digital Collectible |

| Entity | Mocked Data |
|---|---|
| Collector | collectorId: collector-assaf-001<br>displayName: Assaf Collector<br>accountStatus: ACTIVE<br>kycStatus: VERIFIED<br>createdAt: 2026-09-15T08:30:00Z |
| Custodial Wallet | walletId: wallet-assaf-fireblocks-001<br>collectorId: collector-assaf-001<br>provider: FIREBLOCKS<br>providerAccountRef: fb-vault-acc-928173<br>walletAddress: 0x7A8D...F42B<br>walletStatus: ACTIVE |
| Ownership Record | ownershipId: own-charizard-0042-current<br>adcId: adc-charizard-0042<br>collectorId: collector-assaf-001<br>walletId: wallet-assaf-fireblocks-001<br>ownershipStatus: ACTIVE<br>acquisitionType: PRIMARY_ISSUANCE<br>validFrom: 2026-10-01T10:05:22Z<br>validTo: null |
| Vault Item | vaultItemId: vaultitem-assaf-charizard-0042<br>collectorId: collector-assaf-001<br>adcId: adc-charizard-0042<br>ownershipId: own-charizard-0042-current<br>displayStatus: VISIBLE<br>favorite: true<br>sortOrder: 1<br>visibility: PRIVATE |
| Blockchain Token | tokenRefId: token-charizard-0042<br>adcId: adc-charizard-0042<br>chain: POLYGON<br>contractAddress: 0xADC151...9B02<br>tokenId: 42<br>ownerWalletAddress: 0x7A8D...F42B<br>mintTxHash: 0x9f2a...81cc<br>tokenStatus: MINTED |
| Blockchain Event | blockchainEventId: bevt-mint-charizard-0042<br>tokenRefId: token-charizard-0042<br>eventType: MINT_CONFIRMED<br>txHash: 0x9f2a...81cc<br>blockNumber: 68123456<br>logIndex: 12<br>toWallet: 0x7A8D...F42B<br>confirmationStatus: CONFIRMED |

| Entity | Mocked Data |
|---|---|
| Digital Passport | passportId: pass-charizard-0042<br>adcId: adc-charizard-0042<br>currentVersion: 1<br>currentPassportHash: sha256:passport-v1-charizard-0042<br>latestSnapshotUri: s3://adc-passports/pass-charizard-0042/v1.json<br>authenticityStatus: VERIFIED<br>status: ACTIVE |
| Digital Passport Version | passportVersionId: pass-charizard-0042-v1<br>passportId: pass-charizard-0042<br>versionNumber: 1<br>snapshotUri: s3://adc-passports/pass-charizard-0042/v1.json<br>snapshotHash: sha256:passport-v1-charizard-0042<br>changeReason: INITIAL_ISSUANCE<br>createdAt: 2026-10-01T10:05:30Z<br>createdBy: brand-user-pokemon-admin |
| Registry Checkpoint | checkpointId: chk-pokemon-151-20261001-001<br>registryEntryId: reg-charizard-0042<br>checkpointType: DAILY_REGISTRY_ROOT<br>rootHash: sha256:root-pokemon-151-20261001<br>includedVersionCount: 1<br>blockchainTxHash: 0xabc1...root<br>createdAt: 2026-10-01T23:59:59Z |
| Registry Entry | registryEntryId: reg-charizard-0042<br>adcId: adc-charizard-0042<br>officialRegistrationStatus: REGISTERED<br>populationNumber: 42<br>editionNumber: 10000<br>authenticityHash: sha256:8f39c2e1adc0042<br>registeredAt: 2026-10-01T10:05:30Z |
| Media Asset | mediaAssetId: media-charizard-holo-main<br>assetType: IMAGE<br>storageUri: s3://adc-media/pokemon-151/charizard/charizard-holo.png<br>cdnUri: https://cdn.collectb.example/pokemon-151/charizard-holo.png<br>checksum: sha256:cc91e8a9charizard<br> |
| Audit Event | auditEventId: audit-issue-charizard-0042<br>actorId: brand-user-pokemon-admin<br>actionType: ADC_ISSUED<br>objectType: ADC<br>objectId: adc-charizard-0042<br>correlationId: corr-issue-20261001-00042<br>createdAt: 2026-10-01T10:05:35Z |
| Settlement Record | settlementId: settle-charizard-0042-primary<br>transactionId: tx-primary-issue-charizard-0042<br>providerRef: internal-primary-issuance<br>paymentStatus: NOT_REQUIRED<br>grossAmount: 0.00<br>feeAmount: 0.00<br>netAmount: 0.00<br>currency: USD<br>settledAt: 2026-10-01T10:05:36Z |

#### How the Records Connect

| Brand: Pokémon<br> └── Collection: Pokémon 151 Digital Genesis<br>      └── Collectible Template: Charizard Holographic<br>           └── ADC: Charizard Holographic #0042<br>                ├── Collector: Assaf Collector<br>                ├── Custodial Wallet: Fireblocks wallet 0x7A8D...F42B<br>                ├── Ownership Record: ACTIVE<br>                ├── Digital Passport: VERIFIED<br>                ├── Registry Entry: REGISTERED population #42 of 10,000<br>                └── Blockchain Token: Polygon ERC-721 tokenId 42 |
|---|

### Platform Continuity, Insolvency Protection, and Asset Portability

### Purpose

The platform is designed to ensure that collector ownership, authenticity verification, and critical asset records remain accessible and recoverable even if the platform operator becomes insolvent, ceases operations, or can no longer maintain the service. The objective is to protect collectors and brands by ensuring that digital assets are not solely dependent on the continued existence of the operating company.

### Design Principles

The continuity model is based on the following principles:

- Collector assets must remain distinct from company assets.
- Ownership and authenticity must be independently verifiable.
- Critical platform data must be recoverable by authorized parties.
- Brands and collectors must be able to export and migrate their assets.
- A defined transition mechanism must exist for successor operators.
- Verification capabilities must survive the loss of commercial platform services.

### Platform Commitments

#### 1. Collector Beneficial Ownership

The platform recognizes collectors as the beneficial owners of their ADCs and associated digital assets.

Commitments

- Collector assets are not treated as company-owned assets.
- Platform terms and custody agreements clearly distinguish customer assets from platform assets.
- Ownership records remain available for export and verification.

#### 2. Segregated Custodial Wallets

Collector assets are maintained using segregated custody structures designed to prevent commingling with operational or treasury assets.

Commitments

- Collector assets are separated from company treasury assets.
- Custody providers must support asset recovery and transfer procedures.
- Custody arrangements must enable migration to a successor operator if required.

#### 3. Blockchain-Based Ownership Proof

Ownership and authenticity are anchored on-chain through blockchain records.

Commitments

- Every ADC is represented by a blockchain token.
- Contract address and token ID remain independently verifiable.
- Ownership can be validated without requiring access to the platform application.
- Blockchain records remain the authoritative proof of issuance and ownership.

#### 4. Immutable Digital Passport Preservation

Digital Passports serve as the long-term authenticity and provenance record for ADCs.

Commitments

- Passport versions are immutable once published.
- Historical passport versions remain preserved.
- Passport integrity is protected through cryptographic hashing.
- Passport exports remain available to collectors and brands.

#### 5. Registry Checkpoint Anchoring

The ADC Registry provides an additional layer of integrity verification.

Commitments

- Registry checkpoints are periodically cryptographically anchored.
- Registry integrity can be independently validated.
- Registry evidence remains exportable and recoverable.

#### 6. Read-Only Verification Survival Mode

The platform shall support a reduced-operational mode that preserves verification capabilities.

Capabilities

- ADC lookup and verification
- Digital Passport viewing
- Ownership verification
- Provenance history access
- Metadata export
- Authenticity proof validation
Commercial functions such as trading, minting, administration, and marketplace operations may cease, but verification capabilities remain available.

#### 7. Data Export and Portability

Collectors and brands retain the ability to export their platform data.

Collector Exports

- Owned ADC inventory
- Ownership records
- Digital Passport information
- Token references
- Provenance records
Brand Exports

- Collections
- Templates
- Registry records
- Issuance history
- Passport records
- Audit evidence
Exports should be provided in documented formats suitable for migration to alternative systems.

#### 8. Brand Step-In Rights

Brands may be granted continuity rights that allow continued operation of their collections.

Commitments

- Contracts may define step-in rights for participating brands.
- Authorized successors may assume responsibility for collection management.
- Collection verification and ownership records remain available during transition.

#### 9. Escrow of Critical Operational Assets

Critical platform materials may be deposited with an independent escrow provider.

Escrow Scope

- Source code
- Smart contract source code
- Infrastructure-as-code
- Deployment procedures
- Database schemas
- Operational runbooks
- Recovery procedures
- Technical documentation
Release Conditions

- Insolvency
- Platform abandonment
- Prolonged inability to provide service
- Other continuity events defined by contract

#### 10. Successor Operator and Migration Framework

The platform shall maintain procedures that support transition to a successor operator.

Commitments

- Recovery documentation is maintained and periodically reviewed.
- Data required to reconstruct verification services is preserved.
- Migration procedures are documented.
- Authorized successor operators can assume responsibility for continuity operations.

### Continuity Guarantee Statement

The platform maintains a continuity framework consisting of independent custody arrangements, blockchain-verifiable ownership records, Digital Passport preservation, registry integrity controls, escrowed operational materials, and asset portability mechanisms. These controls are designed to ensure that collector assets, ownership records, authenticity evidence, and Digital Passports remain accessible, verifiable, and recoverable if the platform operator becomes insolvent or ceases operations.

#### Suggested TBD Items

Given the maturity of your design, I would also add these as explicit TBD decisions:

- TBD-CP-01: Select custody provider (e.g., Fireblocks).
- TBD-CP-02: Define escrow provider and release conditions.
- TBD-CP-03: Define successor operator governance model.
- TBD-CP-04: Define collector self-custody migration strategy.
- TBD-CP-05: Define long-term metadata storage strategy (S3 only vs. S3 + decentralized storage).
TBD-CP-06: Define brand step-in rights contract template.

## FAQ

### What guarantee can the platform provide if the company becomes insolvent?

Question: Because I’m building a platform for collectors and brands, what kind of guarantee can I provide my clients that, if my company becomes insolvent in the future, the platform will continue to exist and collectors will not lose access to their assets?

Answer: The platform should not promise that the operating company itself will always exist. Instead, it should provide a legally enforceable and technically executable continuity model that protects collector access, brand verification, ownership proof, and critical platform data if the company becomes insolvent, ceases operations, or can no longer maintain the service.

The recommended guarantee should be framed as a Platform Continuity and Asset Access Guarantee. This guarantee combines legal commitments, independent custody, data escrow, source-code escrow, operational runbooks, blockchain verification, and a predefined transition process. The goal is that collectors and brands can continue to verify ownership and access essential asset records even if the original company can no longer operate the product.

- Independent custody of blockchain assets: Collector-owned tokens should be held either in segregated custodial wallets or in a custody-provider structure that clearly separates client assets from company assets. The custody provider should support recovery, transfer, and export procedures if the platform operator fails.
- Escrow of source code, infrastructure, and operational materials: The platform should deposit current source code, build scripts, infrastructure-as-code, deployment documentation, database schemas, runbooks, and dependency manifests with an independent escrow agent. Release conditions should include insolvency, prolonged service failure, or inability to maintain the platform.
- Escrow of critical platform data: The platform should regularly escrow the data needed to reconstruct ownership, Digital Passports, Registry Entries, wallet mappings, blockchain token references, media metadata, and audit evidence. The escrow should be verified periodically so the deposit is usable and not merely contractual.
- Blockchain-verifiable proof: Each ADC should remain independently verifiable through its contract address, token ID, owner wallet, mint transaction, issuance hash, passport hash, and registry checkpoint hash. This ensures that authenticity and ownership proof do not depend only on the platform database.
- Continuity foundation or trustee option: For stronger enterprise assurance, the platform may define a neutral trustee, foundation, or appointed continuity operator that receives the escrow release and can operate a read-only verification portal or migrate the platform to another operator.
- Collector export and recovery rights: The terms of service should define how collectors can export asset records, transfer tokens to self-custody where supported, or receive continued read-only access to Digital Passports and ownership records during a wind-down event.
In practical terms, the client-facing guarantee should say that the company commits to maintaining independently controlled continuity mechanisms so that, upon a defined failure event, authorized beneficiaries can access the current platform materials, reconstruct the verification service, validate ADC ownership against the blockchain, and preserve collector access to essential asset records. This is stronger and more credible than promising perpetual platform operation because it separates collector protection from the financial survival of the operating company.

Recommended wording: The platform will maintain a legally enforceable continuity framework, including independent custody, verified escrow deposits, blockchain-verifiable ownership proof, and defined transition procedures, so that collector assets, Digital Passports, registry evidence, and ownership records remain accessible or recoverable if the platform operator becomes insolvent or ceases operations.

