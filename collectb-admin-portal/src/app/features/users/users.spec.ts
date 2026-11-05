import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CurrentUserService } from '../../core/current-user.service';
import { UserDetail } from '../../core/models';
import { Users } from './users';

describe('Users detail authorization', () => {
  let component: Users;
  let http: HttpTestingController;
  let identityManager: boolean;
  const detail: UserDetail = {
    userId: 'target',
    email: 'target@example.com',
    displayName: 'Target',
    actorType: 'BRAND_USER',
    accountStatus: 'ACTIVE',
    permissions: ['COLLECTION:READ'],
    roleAssignments: [{
      roleAssignmentId: 'assignment',
      roleName: 'BRAND_VIEWER',
      roleType: 'BRAND',
      scopeType: 'BRAND',
      scopeId: 'managed-brand',
      scopeLabel: 'Managed brand',
      assignedAt: '2026-11-01T08:00:00Z',
    }],
  };

  beforeEach(() => {
    identityManager = false;
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: CurrentUserService,
          useValue: {
            canManageRolePermissions: () => identityManager,
            me: () => ({ userId: 'caller' }),
          },
        },
      ],
    });
    component = TestBed.runInInjectionContext(() => new Users());
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads Brand Admin details without requesting the global role catalog', () => {
    component.openDetail('target');
    http.expectOne('/api/v1/users/target').flush(detail);
    http.expectNone('/api/v1/users/roles');
    expect(component.selectedUser()).toEqual(detail);
    component.newRoleBrandId = 'managed-brand';
    component.onDetailBrandChange();
    http.expectOne('/api/v1/users/assignable-roles?brandId=managed-brand').flush(['BRAND_VIEWER']);
    expect(component.detailAssignableRoles()).toEqual(['BRAND_VIEWER']);
  });

  it('loads the global role catalog for platform identity managers', () => {
    identityManager = true;
    component.openDetail('target');
    http.expectOne('/api/v1/users/target').flush(detail);
    http.expectOne('/api/v1/users/roles').flush([]);
  });

  it('clears previous details and surfaces a failed detail request', () => {
    component.selectedUser.set(detail);
    component.openDetail('unrelated');
    expect(component.selectedUser()).toBeNull();
    http.expectOne('/api/v1/users/unrelated').flush('Forbidden', { status: 403, statusText: 'Forbidden' });
    expect(component.detailErrorMessage()).toBe('Forbidden');
  });

  it('uses the existing Brand-scoped grant endpoint for a Brand Admin', () => {
    component.detailVisible.set(true);
    component.selectedUser.set(detail);
    component.newRoleName = 'BRAND_EDITOR';
    component.newRoleBrandId = 'managed-brand';
    component.grantRoleToSelectedUser();
    const request = http.expectOne('/api/v1/users');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      email: detail.email, roleName: 'BRAND_EDITOR', brandId: 'managed-brand',
    });
    request.flush({});
    http.expectOne('/api/v1/users/target').flush(detail);
    http.expectOne('/api/v1/users').flush([]);
  });

  it('hides revoke controls outside managed brands and for self-view', () => {
    component.selectedUser.set(detail);
    const assignment = detail.roleAssignments[0];
    expect(component.canRevokeAssignment(assignment)).toBe(false);
    identityManager = true;
    expect(component.canRevokeAssignment(assignment)).toBe(true);
    component.selectedUser.set({ ...detail, userId: 'caller' });
    expect(component.canRevokeAssignment(assignment)).toBe(false);
  });
});
