import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreatePermissionRequest, Permission, Role } from './models';

/** Typed client for RolesController (roles + permission catalog CRUD + role_permission mapping). */
@Injectable({ providedIn: 'root' })
export class RolesApiService {
  private readonly http = inject(HttpClient);

  listRoles(): Observable<Role[]> {
    return this.http.get<Role[]>('/api/v1/roles');
  }

  listPermissions(): Observable<Permission[]> {
    return this.http.get<Permission[]>('/api/v1/roles/permissions');
  }

  createPermission(request: CreatePermissionRequest): Observable<Permission> {
    return this.http.post<Permission>('/api/v1/roles/permissions', request);
  }

  updatePermission(permissionId: string, request: CreatePermissionRequest): Observable<Permission> {
    return this.http.put<Permission>(`/api/v1/roles/permissions/${permissionId}`, request);
  }

  deletePermission(permissionId: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/roles/permissions/${permissionId}`);
  }

  listRolePermissionIds(roleId: string): Observable<string[]> {
    return this.http.get<string[]>(`/api/v1/roles/${roleId}/permissions`);
  }

  grantPermission(roleId: string, permissionId: string): Observable<void> {
    return this.http.put<void>(`/api/v1/roles/${roleId}/permissions/${permissionId}`, {});
  }

  revokePermission(roleId: string, permissionId: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/roles/${roleId}/permissions/${permissionId}`);
  }
}
