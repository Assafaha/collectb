import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AssignRoleRequest,
  Brand,
  CreateUserRequest,
  CreateUserResponse,
  GrantUserRequest,
  PlatformUserRow,
  RoleAssignmentDetail,
  RoleCatalogEntry,
  UpdateUserRequest,
  UserDetail,
} from './models';

/** Typed client for the admin dashboard's aggregated "Users" screen (see UsersController in
 *  collectb-platform-api). Unlike AdminApiService/BrandPortalApiService (both scoped to one
 *  brand at a time), these endpoints return whatever the caller is allowed to see across every
 *  brand at once - the backend decides platform-admin-vs-brand-admin scoping server-side. */
@Injectable({ providedIn: 'root' })
export class UsersApiService {
  private readonly http = inject(HttpClient);

  listUsers(): Observable<PlatformUserRow[]> {
    return this.http.get<PlatformUserRow[]>('/api/v1/users');
  }

  manageableBrands(): Observable<Brand[]> {
    return this.http.get<Brand[]>('/api/v1/users/manageable-brands');
  }

  getAssignableRoles(brandId: string): Observable<string[]> {
    return this.http.get<string[]>('/api/v1/users/assignable-roles', { params: { brandId } });
  }

  grantUser(request: GrantUserRequest): Observable<PlatformUserRow> {
    return this.http.post<PlatformUserRow>('/api/v1/users', request);
  }

  revokeUser(roleAssignmentId: string, brandId: string): Observable<void> {
    return this.http.post<void>(`/api/v1/users/${roleAssignmentId}/revoke`, {}, { params: { brandId } });
  }

  /** Full role catalog (every scope type) for the user-detail view's "add role" picker. */
  listRoleCatalog(): Observable<RoleCatalogEntry[]> {
    return this.http.get<RoleCatalogEntry[]>('/api/v1/users/roles');
  }

  /** User detail; roles and permissions are restricted server-side to visible scopes. */
  getUserDetail(userId: string): Observable<UserDetail> {
    return this.http.get<UserDetail>(`/api/v1/users/${userId}`);
  }

  /** Grants a role to a user at any scope (not just BRAND, unlike {@link grantUser} above). */
  assignRole(userId: string, request: AssignRoleRequest): Observable<RoleAssignmentDetail> {
    return this.http.post<RoleAssignmentDetail>(`/api/v1/users/${userId}/roles`, request);
  }

  /** Revokes one of a user's role assignments at any scope. */
  revokeRole(userId: string, roleAssignmentId: string): Observable<void> {
    return this.http.post<void>(`/api/v1/users/${userId}/roles/${roleAssignmentId}/revoke`, {});
  }

  /** Creates a brand-new platform user with no role assignment yet (platform admin only). The
   *  response's tempPassword is shown once - hand it to the new user out-of-band. */
  createUser(request: CreateUserRequest): Observable<CreateUserResponse> {
    return this.http.post<CreateUserResponse>('/api/v1/users/accounts', request);
  }

  /** Edits an existing user's identity fields (platform admin only). */
  updateUser(userId: string, request: UpdateUserRequest): Observable<UserDetail> {
    return this.http.put<UserDetail>(`/api/v1/users/${userId}`, request);
  }
}
