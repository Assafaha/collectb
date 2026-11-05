import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AgGridAngular } from 'ag-grid-angular';
import { AllCommunityModule, ColDef, ModuleRegistry } from 'ag-grid-community';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { RolesApiService } from '../../core/roles-api.service';
import { Permission, Role } from '../../core/models';
import { defaultColDef } from '../../core/grid-defaults';
import { registerAgGridDarkTheme } from '../../core/ag-grid-theme';
import { DescriptionCellRenderer } from '../../core/description-cell-renderer';
import { GrantCheckboxCellRenderer } from './grant-checkbox-cell-renderer';

ModuleRegistry.registerModules([AllCommunityModule]);
registerAgGridDarkTheme();

type PermissionRow = Permission & { granted: boolean };

/** Maps permissions to roles (role_permission). Selecting a role shows every permission with a
 *  checkbox; toggling one grants/revokes it immediately. Needs PLATFORM_IDENTITY:MANAGE. Uses the
 *  same AG Grid table UX as the Roles and Permissions screens. */
@Component({
  selector: 'app-role-permissions',
  imports: [AgGridAngular, FormsModule, MessageModule, SelectModule],
  templateUrl: './role-permissions.html',
})
export class RolePermissions implements OnInit {
  private readonly api = inject(RolesApiService);

  readonly roles = signal<Role[]>([]);
  readonly permissions = signal<Permission[]>([]);
  readonly grantedIds = signal<Set<string>>(new Set());
  readonly rows = signal<PermissionRow[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  selectedRoleId: string | null = null;

  readonly defaultColDef = defaultColDef;
  readonly columnDefs: ColDef<PermissionRow>[] = [
    {
      headerName: '',
      width: 70,
      cellRenderer: GrantCheckboxCellRenderer,
      cellRendererParams: {
        onToggle: (permission: Permission, checked: boolean) => this.toggle(permission, checked),
      },
    },
    { field: 'permissionKey', headerName: 'Permission', flex: 2 },
    { field: 'resourceType', headerName: 'Resource', flex: 1 },
    { field: 'action', headerName: 'Action', flex: 1 },
    { field: 'description', headerName: '', flex: 1, cellRenderer: DescriptionCellRenderer },
  ];

  ngOnInit(): void {
    this.api.listRoles().subscribe({
      next: (roles) => {
        this.roles.set(roles);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.api.listPermissions().subscribe({
      next: (p) => {
        this.permissions.set(p);
        this.buildRows();
      },
    });
  }

  onRoleChange(): void {
    this.errorMessage.set(null);
    this.grantedIds.set(new Set());
    this.buildRows();
    if (!this.selectedRoleId) {
      return;
    }
    this.api.listRolePermissionIds(this.selectedRoleId).subscribe({
      next: (ids) => {
        this.grantedIds.set(new Set(ids));
        this.buildRows();
      },
      error: (err) => this.errorMessage.set(err.error ?? 'Failed to load role permissions'),
    });
  }

  toggle(permission: Permission, checked: boolean): void {
    const roleId = this.selectedRoleId;
    if (!roleId) {
      return;
    }
    this.errorMessage.set(null);
    const request = checked
      ? this.api.grantPermission(roleId, permission.permissionId)
      : this.api.revokePermission(roleId, permission.permissionId);
    request.subscribe({
      next: () => {
        const next = new Set(this.grantedIds());
        if (checked) {
          next.add(permission.permissionId);
        } else {
          next.delete(permission.permissionId);
        }
        this.grantedIds.set(next);
        this.buildRows();
      },
      error: (err) => {
        this.errorMessage.set(err.error ?? 'Failed to update role permission');
        this.buildRows();
      },
    });
  }

  /** Recomputes the grid's row data from the permission catalog + the granted-id set, so AG Grid
   *  re-renders the checkbox column's state whenever either changes (role switch or toggle). */
  private buildRows(): void {
    const granted = this.grantedIds();
    this.rows.set(this.permissions().map((p) => ({ ...p, granted: granted.has(p.permissionId) })));
  }
}

