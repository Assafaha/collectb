import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Brand,
  BrandUser,
  CatalogCollection,
  CatalogCollectionRequest,
  CatalogMediaAsset,
  CatalogMediaAssetRequest,
  CatalogTemplate,
  CatalogTemplateRequest,
  GrantBrandUserRequest,
  Me,
  MyBrandAccess,
} from './models';

/** Typed client for the brand portal's JSON REST API (see BrandPortalController/BrandController
 *  in collectb-platform-api). All calls go through this app's dev-server proxy under {@code /api}. */
@Injectable({ providedIn: 'root' })
export class BrandPortalApiService {
  private readonly http = inject(HttpClient);

  getMe(): Observable<Me> {
    return this.http.get<Me>('/api/v1/me');
  }

  getMyBrands(): Observable<MyBrandAccess[]> {
    return this.http.get<MyBrandAccess[]>('/api/v1/me/brands');
  }

  getBrand(brandId: string): Observable<Brand> {
    return this.http.get<Brand>(`/api/v1/brands/${brandId}`);
  }

  getBrandUsers(brandId: string): Observable<BrandUser[]> {
    return this.http.get<BrandUser[]>(`/api/v1/brands/${brandId}/users`);
  }

  getAssignableRoles(brandId: string): Observable<string[]> {
    return this.http.get<string[]>(`/api/v1/brands/${brandId}/users/assignable-roles`);
  }

  grantBrandUser(brandId: string, request: GrantBrandUserRequest): Observable<BrandUser> {
    return this.http.post<BrandUser>(`/api/v1/brands/${brandId}/users`, request);
  }

  revokeBrandUser(brandId: string, assignmentId: string): Observable<void> {
    return this.http.post<void>(`/api/v1/brands/${brandId}/users/${assignmentId}/revoke`, {});
  }

  getCollections(brandId: string): Observable<CatalogCollection[]> {
    return this.http.get<CatalogCollection[]>(`/api/v1/brands/${brandId}/collections`);
  }

  createCollection(brandId: string, request: CatalogCollectionRequest): Observable<CatalogCollection> {
    return this.http.post<CatalogCollection>(`/api/v1/brands/${brandId}/collections`, request);
  }

  updateCollection(
    brandId: string,
    collectionId: string,
    request: CatalogCollectionRequest,
  ): Observable<CatalogCollection> {
    return this.http.patch<CatalogCollection>(
      `/api/v1/brands/${brandId}/collections/${collectionId}`,
      request,
    );
  }

  deleteCollection(brandId: string, collectionId: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/brands/${brandId}/collections/${collectionId}`);
  }

  collectionAction(
    brandId: string,
    collectionId: string,
    action: 'submit-for-review' | 'withdraw-from-review' | 'publish' | 'retire',
  ): Observable<CatalogCollection> {
    return this.http.post<CatalogCollection>(
      `/api/v1/brands/${brandId}/collections/${collectionId}/${action}`,
      {},
    );
  }

  reviewCollection(
    brandId: string,
    collectionId: string,
    decision: 'APPROVE' | 'REQUEST_CHANGES',
    comment: string | null,
  ): Observable<CatalogCollection> {
    return this.http.post<CatalogCollection>(
      `/api/v1/brands/${brandId}/collections/${collectionId}/review`,
      { decision, comment },
    );
  }

  getTemplates(brandId: string, collectionId: string): Observable<CatalogTemplate[]> {
    return this.http.get<CatalogTemplate[]>(
      `/api/v1/brands/${brandId}/collections/${collectionId}/templates`,
    );
  }

  createTemplate(
    brandId: string,
    collectionId: string,
    request: CatalogTemplateRequest,
  ): Observable<CatalogTemplate> {
    return this.http.post<CatalogTemplate>(
      `/api/v1/brands/${brandId}/collections/${collectionId}/templates`,
      request,
    );
  }

  updateTemplate(
    brandId: string,
    collectionId: string,
    templateId: string,
    request: CatalogTemplateRequest,
  ): Observable<CatalogTemplate> {
    return this.http.patch<CatalogTemplate>(
      `/api/v1/brands/${brandId}/collections/${collectionId}/templates/${templateId}`,
      request,
    );
  }

  deleteTemplate(brandId: string, collectionId: string, templateId: string): Observable<void> {
    return this.http.delete<void>(
      `/api/v1/brands/${brandId}/collections/${collectionId}/templates/${templateId}`,
    );
  }

  templateAction(
    brandId: string,
    collectionId: string,
    templateId: string,
    action:
      | 'submit-for-review'
      | 'withdraw-from-review'
      | 'publish'
      | 'retire',
  ): Observable<CatalogTemplate> {
    return this.http.post<CatalogTemplate>(
      `/api/v1/brands/${brandId}/collections/${collectionId}/templates/${templateId}/${action}`,
      {},
    );
  }

  reviewTemplate(
    brandId: string,
    collectionId: string,
    templateId: string,
    decision: 'APPROVE' | 'REQUEST_CHANGES',
    comment: string | null,
  ): Observable<CatalogTemplate> {
    return this.http.post<CatalogTemplate>(
      `/api/v1/brands/${brandId}/collections/${collectionId}/templates/${templateId}/review`,
      { decision, comment },
    );
  }

  cloneTemplate(
    brandId: string,
    collectionId: string,
    templateId: string,
    templateCode: string,
    displayName: string,
  ): Observable<CatalogTemplate> {
    return this.http.post<CatalogTemplate>(
      `/api/v1/brands/${brandId}/collections/${collectionId}/templates/${templateId}/clone`,
      { templateCode, displayName },
    );
  }

  getMediaAssets(brandId: string): Observable<CatalogMediaAsset[]> {
    return this.http.get<CatalogMediaAsset[]>(`/api/v1/brands/${brandId}/media-assets`);
  }

  registerMediaAsset(
    brandId: string,
    request: CatalogMediaAssetRequest,
  ): Observable<CatalogMediaAsset> {
    return this.http.post<CatalogMediaAsset>(`/api/v1/brands/${brandId}/media-assets`, request);
  }
}
