/** Mirrors com.example.collectb.brandportal.web.MyBrandAccessResponse. */
export interface MyBrandAccess {
  brandId: string;
  brandCode: string;
  name: string;
  licenseStatus: string;
  myRoleName: string;
  createdAt: string;
}

/** Mirrors com.example.collectb.catalog.web.BrandResponse. */
export interface Brand {
  brandId: string;
  brandCode: string;
  name: string;
  description: string | null;
  licenseStatus: string;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

/** Mirrors com.example.collectb.brandportal.web.BrandUserResponse. */
export interface BrandUser {
  roleAssignmentId: string;
  userId: string;
  userEmail: string;
  userDisplayName: string;
  roleName: string;
  assignedAt: string;
}

/** Mirrors com.example.collectb.brandportal.web.GrantBrandUserRequest. */
export interface GrantBrandUserRequest {
  email: string;
  roleName: string;
}

/** Mirrors com.example.collectb.brandportal.web.MeResponse. */
export interface Me {
  userId: string;
  email: string;
  displayName: string;
  actorType: string;
  roles: string[];
  permissions: string[];
  authStrength: string;
  catalogMfaRequired: boolean;
}

export interface CatalogCollection {
  collectionId: string;
  brandId: string;
  collectionCode: string;
  name: string;
  description: string | null;
  releaseStatus: string;
  releaseStartAt: string | null;
  releaseEndAt: string | null;
  createdByUserId: string;
  updatedByUserId: string | null;
  reviewedByUserId: string | null;
  reviewedAt: string | null;
  reviewComment: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CatalogCollectionRequest {
  collectionCode: string;
  name: string;
  description: string | null;
  releaseStartAt: string | null;
  releaseEndAt: string | null;
}

export interface CatalogTemplate {
  templateId: string;
  collectionId: string;
  mediaAssetId: string | null;
  templateCode: string;
  displayName: string;
  rarity: string | null;
  editionSize: number;
  metadataSchemaVersion: string;
  metadataUri: string;
  metadataHash: string | null;
  traits: Record<string, string> | null;
  publishStatus: string;
  createdByUserId: string;
  updatedByUserId: string | null;
  reviewedByUserId: string | null;
  reviewedAt: string | null;
  reviewComment: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CatalogTemplateRequest {
  templateCode: string;
  displayName: string;
  rarity: string | null;
  editionSize: number;
  metadataSchemaVersion: string;
  metadataUri: string;
  metadataHash: string | null;
  mediaAssetId: string | null;
  traits: Record<string, string> | null;
}

export interface CatalogMediaAsset {
  mediaAssetId: string;
  brandId: string;
  assetType: string;
  storageUri: string;
  cdnUri: string | null;
  contentType: string | null;
  checksum: string;
  fileSizeBytes: number | null;
  createdAt: string;
}

export interface CatalogMediaAssetRequest {
  assetType: string;
  storageUri: string;
  cdnUri: string | null;
  contentType: string | null;
  checksum: string;
  fileSizeBytes: number | null;
}

/** Mirrors com.example.collectb.catalog.web.CreateBrandRequest. */
export interface CreateBrandRequest {
  brandCode: string;
  name: string;
  description: string;
}

/** Mirrors com.example.collectb.users.web.PlatformUserRowResponse. For a platform admin caller,
 *  roleAssignmentId/brandId/brandCode/brandName/roleName/assignedAt may all be null: this
 *  happens for a user who holds only a PLATFORM-scoped role (no brand grant to show) or no
 *  active role assignment at all - both still need a row in the full "every platform user"
 *  table. A brand admin caller never sees such rows. */
export interface PlatformUserRow {
  roleAssignmentId: string | null;
  userId: string;
  userEmail: string;
  userDisplayName: string;
  brandId: string | null;
  brandCode: string | null;
  brandName: string | null;
  roleName: string | null;
  assignedAt: string | null;
}

/** Mirrors com.example.collectb.users.web.GrantUserRequest. */
export interface GrantUserRequest {
  brandId: string;
  email: string;
  roleName: string;
}

/** Mirrors com.example.collectb.identity.model.ActorType. */
export const ACTOR_TYPES = [
  'COLLECTOR',
  'BRAND_USER',
  'PLATFORM_ADMIN',
  'SUPPORT_USER',
  'COMPLIANCE_USER',
  'SERVICE_ACCOUNT',
  'SYSTEM',
] as const;

/** Mirrors com.example.collectb.identity.model.AccountStatus. */
export const ACCOUNT_STATUSES = [
  'PENDING_VERIFICATION',
  'ACTIVE',
  'LOCKED',
  'DISABLED',
  'SUSPENDED',
  'DELETED',
] as const;

/** Mirrors com.example.collectb.users.web.CreateUserRequest. */
export interface CreateUserRequest {
  email: string;
  displayName: string;
  actorType: string;
}

/** Mirrors com.example.collectb.users.web.CreateUserResponse. tempPassword is shown once to the
 *  admin after creation - it is never retrievable again. */
export interface CreateUserResponse {
  userId: string;
  email: string;
  displayName: string;
  actorType: string;
  tempPassword: string;
}

/** Mirrors com.example.collectb.users.web.UpdateUserRequest. */
export interface UpdateUserRequest {
  displayName: string;
  actorType: string;
  accountStatus: string;
}

/** Mirrors com.example.collectb.users.web.RoleAssignmentDetailResponse. scopeLabel is the
 *  brand's display name when scopeType is "BRAND", null for every other scope type (which all
 *  have a null scopeId too). */
export interface RoleAssignmentDetail {
  roleAssignmentId: string;
  roleName: string;
  roleType: string;
  scopeType: string;
  scopeId: string | null;
  scopeLabel: string | null;
  assignedAt: string;
}

/** Mirrors com.example.collectb.users.web.UserDetailResponse. */
export interface UserDetail {
  userId: string;
  email: string;
  displayName: string;
  actorType: string;
  accountStatus: string;
  roleAssignments: RoleAssignmentDetail[];
  permissions: string[];
}

/** Mirrors com.example.collectb.users.web.RoleCatalogEntryResponse. */
export interface RoleCatalogEntry {
  roleName: string;
  roleType: string;
  description: string | null;
  privileged: boolean;
}

/** Mirrors com.example.collectb.users.web.AssignRoleRequest. scopeId must be a brandId when
 *  scopeType is "BRAND", and must be omitted/null for every other scope type. */
export interface AssignRoleRequest {
  roleName: string;
  scopeType: string;
  scopeId: string | null;
}


/** Mirrors com.example.collectb.users.web.RolesController.RoleResponse. */
export interface Role {
  roleId: string;
  roleName: string;
  roleType: string;
  description: string | null;
  privileged: boolean;
}

/** Mirrors com.example.collectb.users.web.RolesController.PermissionResponse. */
export interface Permission {
  permissionId: string;
  permissionKey: string;
  resourceType: string;
  action: string;
  description: string | null;
}

/** Mirrors com.example.collectb.users.web.RolesController.PermissionRequest. Used for both
 *  create and update. */
export interface CreatePermissionRequest {
  permissionKey: string;
  resourceType: string;
  action: string;
  description: string | null;
}
