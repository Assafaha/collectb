import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Brand, BrandUser, CreateBrandRequest, GrantBrandUserRequest } from './models';

/** Typed client for the Angular admin dashboard section's JSON REST API (see
 *  AdminBrandApiController in collectb-platform-api). Unlike BrandPortalApiService, every call
 *  here operates on *any* brand, not just ones the caller has been granted access to - the
 *  backend enforces PLATFORM_BRAND:MANAGE/PLATFORM_IDENTITY:MANAGE on every endpoint. */
@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);

  listBrands(): Observable<Brand[]> {
    return this.http.get<Brand[]>('/api/v1/admin/brands');
  }

  createBrand(request: CreateBrandRequest): Observable<Brand> {
    return this.http.post<Brand>('/api/v1/admin/brands', request);
  }

  getBrand(brandId: string): Observable<Brand> {
    return this.http.get<Brand>(`/api/v1/admin/brands/${brandId}`);
  }

  updateLicenseStatus(brandId: string, licenseStatus: string): Observable<Brand> {
    return this.http.patch<Brand>(`/api/v1/admin/brands/${brandId}/license-status`, { licenseStatus });
  }

  getBrandUsers(brandId: string): Observable<BrandUser[]> {
    return this.http.get<BrandUser[]>(`/api/v1/admin/brands/${brandId}/users`);
  }

  getAssignableRoles(brandId: string): Observable<string[]> {
    return this.http.get<string[]>(`/api/v1/admin/brands/${brandId}/users/assignable-roles`);
  }

  grantBrandUser(brandId: string, request: GrantBrandUserRequest): Observable<BrandUser> {
    return this.http.post<BrandUser>(`/api/v1/admin/brands/${brandId}/users`, request);
  }

  revokeBrandUser(brandId: string, assignmentId: string): Observable<void> {
    return this.http.post<void>(`/api/v1/admin/brands/${brandId}/users/${assignmentId}/revoke`, {});
  }
}
