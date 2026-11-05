import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AgGridAngular } from 'ag-grid-angular';
import { AllCommunityModule, ColDef, ModuleRegistry } from 'ag-grid-community';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { MessageModule } from 'primeng/message';
import { DialogModule } from 'primeng/dialog';
import { TagModule } from 'primeng/tag';
import { Observable } from 'rxjs';
import { UsersApiService } from '../../core/users-api.service';
import { CurrentUserService } from '../../core/current-user.service';
import {
  ACCOUNT_STATUSES,
  ACTOR_TYPES,
  Brand,
  PlatformUserRow,
  RoleCatalogEntry,
  RoleAssignmentDetail,
  UserDetail,
} from '../../core/models';
import { ViewDetailsCellRenderer } from './view-details-cell-renderer';
import { defaultColDef } from '../../core/grid-defaults';
import { registerAgGridDarkTheme } from '../../core/ag-grid-theme';

ModuleRegistry.registerModules([AllCommunityModule]);
registerAgGridDarkTheme();

/** Admin dashboard "Users" screen: a single aggregated table of platform users. A PLATFORM_ADMIN
 *  sees every registered user at least once (including ones with only a PLATFORM-scoped role, or
 *  no role grant yet - shown with a blank Brand/Role); a BRAND_ADMIN sees only the users who
 *  actually have a role grant on the brand(s) they themselves administer - the backend
 *  (UsersController) decides which, this component just renders whatever GET /api/v1/users
 *  returns for the caller. Details show all roles for self-view/platform administrators,
 *  and only managed Brand scopes when a Brand Admin views another user. */
@Component({
  selector: 'app-users',
  imports: [
    AgGridAngular,
    FormsModule,
    ButtonModule,
    InputTextModule,
    SelectModule,
    MessageModule,
    DialogModule,
    TagModule,
  ],
  templateUrl: './users.html',
  styleUrl: './users.scss',
})
export class Users implements OnInit {
  private readonly api = inject(UsersApiService);
  private readonly currentUser = inject(CurrentUserService);

  readonly rows = signal<PlatformUserRow[]>([]);
  readonly manageableBrands = signal<Brand[]>([]);
  readonly assignableRoles = signal<string[]>([]);
  readonly loading = signal(true);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  /** Whether the caller can manage more than one brand's users here - drives whether the grant
   *  form and the table itself show a "Brand" picker/column at all (a BRAND_ADMIN with exactly
   *  one brand has nothing to disambiguate). */
  readonly isPlatformAdmin = () => this.currentUser.canManageRolePermissions();

  grantBrandId: string | null = null;
  grantEmail = '';
  grantRoleName: string | null = null;

  // --- "Add user" dialog state (platform admin only) ---
  readonly actorTypes: string[] = [...ACTOR_TYPES];
  readonly accountStatuses: string[] = [...ACCOUNT_STATUSES];
  readonly addUserVisible = signal(false);
  readonly addUserErrorMessage = signal<string | null>(null);
  readonly createdUser = signal<{ email: string; tempPassword: string } | null>(null);
  newUserEmail = '';
  newUserDisplayName = '';
  newUserActorType: string | null = null;

  // --- Per-user detail dialog state ---
  readonly detailVisible = signal(false);
  readonly selectedUser = signal<UserDetail | null>(null);
  readonly roleCatalog = signal<RoleCatalogEntry[]>([]);
  readonly detailAssignableRoles = signal<string[]>([]);
  readonly detailSuccessMessage = signal<string | null>(null);
  readonly detailErrorMessage = signal<string | null>(null);
  readonly editMode = signal(false);

  editDisplayName = '';
  editActorType: string | null = null;
  editAccountStatus: string | null = null;

  newRoleName: string | null = null;
  newRoleBrandId: string | null = null;

  readonly defaultColDef = defaultColDef;
  readonly columnDefs: ColDef<PlatformUserRow>[] = [
    { field: 'userEmail', headerName: 'Email', flex: 2 },
    { field: 'userDisplayName', headerName: 'Display name', flex: 1 },
    { field: 'roleName', headerName: 'Roles', flex: 2, valueFormatter: (p) => p.value ?? '—' },
    { field: 'assignedAt', headerName: 'Assigned at', flex: 1, valueFormatter: (p) => p.value ?? '—' },
    {
      headerName: '',
      flex: 1,
      cellRenderer: ViewDetailsCellRenderer,
      cellRendererParams: {
        onView: (userId: string) => this.openDetail(userId),
      },
    },
  ];

  ngOnInit(): void {
    this.reload();
    this.api.manageableBrands().subscribe({
      next: (brands) => this.manageableBrands.set(brands),
      error: () => {
        // Leaving the picker empty is enough feedback if this fails.
      },
    });
  }

  private reload(): void {
    this.loading.set(true);
    this.api.listUsers().subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onGrantBrandChange(): void {
    this.grantRoleName = null;
    this.assignableRoles.set([]);
    if (!this.grantBrandId) {
      return;
    }
    this.api.getAssignableRoles(this.grantBrandId).subscribe({
      next: (roles) => this.assignableRoles.set(roles),
      error: () => {
        // Leaving the picker empty is enough feedback if this fails.
      },
    });
  }

  grant(): void {
    this.successMessage.set(null);
    this.errorMessage.set(null);
    if (!this.grantBrandId || !this.grantEmail || !this.grantRoleName) {
      this.errorMessage.set('Brand, email and role are required');
      return;
    }
    this.api
      .grantUser({ brandId: this.grantBrandId, email: this.grantEmail, roleName: this.grantRoleName })
      .subscribe({
        next: () => {
          this.successMessage.set(`Granted ${this.grantRoleName} to ${this.grantEmail}`);
          this.grantEmail = '';
          this.grantRoleName = null;
          this.reload();
        },
        error: (err) => this.errorMessage.set(err.error ?? 'Failed to grant access'),
      });
  }

  /** Opens the "Add user" dialog (platform admin only) - creates a brand-new platform_user with
   *  no role assignment yet; grant one afterwards via the per-user "Details" dialog. */
  openAddUser(): void {
    this.addUserErrorMessage.set(null);
    this.createdUser.set(null);
    this.newUserEmail = '';
    this.newUserDisplayName = '';
    this.newUserActorType = null;
    this.addUserVisible.set(true);
  }

  closeAddUser(): void {
    this.addUserVisible.set(false);
  }

  createUser(): void {
    this.addUserErrorMessage.set(null);
    if (!this.newUserEmail || !this.newUserDisplayName || !this.newUserActorType) {
      this.addUserErrorMessage.set('Email, display name and actor type are required');
      return;
    }
    this.api
      .createUser({ email: this.newUserEmail, displayName: this.newUserDisplayName, actorType: this.newUserActorType })
      .subscribe({
        next: (created) => {
          this.createdUser.set({ email: created.email, tempPassword: created.tempPassword });
          this.reload();
        },
        error: (err) => this.addUserErrorMessage.set(err.error ?? 'Failed to create user'),
      });
  }

  /** Opens the per-user detail dialog and loads its data; also lazily loads the full role
   *  catalog (once) for the "add role" picker. */
  openDetail(userId: string): void {
    this.detailErrorMessage.set(null);
    this.detailSuccessMessage.set(null);
    this.newRoleName = null;
    this.newRoleBrandId = null;
    this.selectedUser.set(null);
    this.detailAssignableRoles.set([]);
    this.editMode.set(false);
    this.detailVisible.set(true);
    this.loadDetail(userId);
    if (this.isPlatformAdmin() && this.roleCatalog().length === 0) {
      this.api.listRoleCatalog().subscribe({
        next: (roles) => this.roleCatalog.set(roles),
        error: (err) => this.detailErrorMessage.set(err.error ?? 'Failed to load role catalog'),
      });
    }
  }

  closeDetail(): void {
    this.detailVisible.set(false);
    this.selectedUser.set(null);
    this.editMode.set(false);
  }

  private loadDetail(userId: string): void {
    this.api.getUserDetail(userId).subscribe({
      next: (detail) => {
        if (this.detailVisible()) {
          this.selectedUser.set(detail);
        }
      },
      error: (err) => this.detailErrorMessage.set(err.error || 'Failed to load user detail'),
    });
  }

  onDetailBrandChange(): void {
    this.newRoleName = null;
    this.detailAssignableRoles.set([]);
    const brandId = this.newRoleBrandId;
    if (!brandId || this.isPlatformAdmin()) {
      return;
    }
    this.api.getAssignableRoles(brandId).subscribe({
      next: (roles) => {
        if (this.newRoleBrandId === brandId) {
          this.detailAssignableRoles.set(roles);
        }
      },
      error: (err) => this.detailErrorMessage.set(err.error || 'Failed to load assignable roles'),
    });
  }

  canRevokeAssignment(assignment: RoleAssignmentDetail): boolean {
    return !this.isViewingSelf() && (this.isPlatformAdmin()
      || (assignment.scopeType === 'BRAND'
        && this.manageableBrands().some((brand) => brand.brandId === assignment.scopeId)));
  }

  /** Switches the detail dialog into edit mode, pre-filling the form from the currently loaded
   *  user - platform admin only (mirrors the backend's PLATFORM_IDENTITY:MANAGE check). */
  startEdit(): void {
    const user = this.selectedUser();
    if (!user) {
      return;
    }
    this.detailErrorMessage.set(null);
    this.detailSuccessMessage.set(null);
    this.editDisplayName = user.displayName;
    this.editActorType = user.actorType;
    this.editAccountStatus = user.accountStatus;
    this.editMode.set(true);
  }

  cancelEdit(): void {
    this.editMode.set(false);
  }

  saveEdit(): void {
    const user = this.selectedUser();
    if (!user) {
      return;
    }
    this.detailErrorMessage.set(null);
    if (!this.editDisplayName || !this.editActorType || !this.editAccountStatus) {
      this.detailErrorMessage.set('Display name, actor type and account status are required');
      return;
    }
    this.api
      .updateUser(user.userId, {
        displayName: this.editDisplayName,
        actorType: this.editActorType,
        accountStatus: this.editAccountStatus,
      })
      .subscribe({
        next: (updated) => {
          this.selectedUser.set(updated);
          this.detailSuccessMessage.set('User updated');
          this.editMode.set(false);
          this.reload();
        },
        error: (err) => this.detailErrorMessage.set(err.error ?? 'Failed to update user'),
      });
  }

  /** The role_type of whichever role is currently selected in the "add role" picker - null until
   *  a role is picked. Only "BRAND" requires the extra brand picker to also be filled in. */
  get selectedRoleType(): string | null {
    if (!this.isPlatformAdmin()) {
      return 'BRAND';
    }
    return this.roleCatalog().find((r) => r.roleName === this.newRoleName)?.roleType ?? null;
  }

  /** Whether the user being viewed in the dialog is the caller themselves - the backend refuses
   *  to let a caller revoke their own role assignments (to avoid accidental self-lockout from the
   *  admin dashboard), so the revoke button is hidden client-side too rather than letting the
   *  user click it and get an error back. */
  isViewingSelf(): boolean {
    const me = this.currentUser.me();
    const user = this.selectedUser();
    return !!me && !!user && me.userId === user.userId;
  }

  grantRoleToSelectedUser(): void {
    this.detailErrorMessage.set(null);
    this.detailSuccessMessage.set(null);
    const user = this.selectedUser();
    const scopeType = this.selectedRoleType;
    if (!user || !this.newRoleName || !scopeType) {
      this.detailErrorMessage.set('Select a role');
      return;
    }
    if (scopeType === 'BRAND' && !this.newRoleBrandId) {
      this.detailErrorMessage.set('Select a brand for this role');
      return;
    }
    const scopeId = scopeType === 'BRAND' ? this.newRoleBrandId : null;
    let request: Observable<unknown>;
    if (this.isPlatformAdmin()) {
      request = this.api.assignRole(user.userId, { roleName: this.newRoleName, scopeType, scopeId });
    } else {
      if (!scopeId) {
        this.detailErrorMessage.set('Select a brand for this role');
        return;
      }
      request = this.api.grantUser({ email: user.email, roleName: this.newRoleName, brandId: scopeId });
    }
    request.subscribe({
      next: () => {
        this.detailSuccessMessage.set(`Granted ${this.newRoleName}`);
        this.newRoleName = null;
        this.newRoleBrandId = null;
        this.detailAssignableRoles.set([]);
        this.loadDetail(user.userId);
        this.reload();
      },
      error: (err) => this.detailErrorMessage.set(err.error ?? 'Failed to grant role'),
    });
  }

  revokeRoleFromSelectedUser(roleAssignmentId: string): void {
    this.detailErrorMessage.set(null);
    this.detailSuccessMessage.set(null);
    const user = this.selectedUser();
    if (!user) {
      return;
    }
    const assignment = user.roleAssignments.find((role) => role.roleAssignmentId === roleAssignmentId);
    if (!assignment || !this.canRevokeAssignment(assignment)) {
      this.detailErrorMessage.set('You cannot revoke this role assignment');
      return;
    }
    const brandId = assignment.scopeId;
    let request: Observable<void>;
    if (this.isPlatformAdmin()) {
      request = this.api.revokeRole(user.userId, roleAssignmentId);
    } else {
      if (!brandId) {
        this.detailErrorMessage.set('A brand scope is required to revoke this assignment');
        return;
      }
      request = this.api.revokeUser(roleAssignmentId, brandId);
    }
    request.subscribe({
      next: () => {
        this.detailSuccessMessage.set('Role revoked');
        this.loadDetail(user.userId);
        this.reload();
      },
      error: (err) => this.detailErrorMessage.set(err.error ?? 'Failed to revoke role'),
    });
  }
}
