import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AgGridAngular } from 'ag-grid-angular';
import { AllCommunityModule, ColDef, ModuleRegistry } from 'ag-grid-community';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { DialogModule } from 'primeng/dialog';
import { RolesApiService } from '../../core/roles-api.service';
import { Permission } from '../../core/models';
import { defaultColDef } from '../../core/grid-defaults';
import { registerAgGridDarkTheme } from '../../core/ag-grid-theme';
import { DescriptionCellRenderer } from '../../core/description-cell-renderer';
import { PermissionActionsCellRenderer } from './permission-actions-cell-renderer';

ModuleRegistry.registerModules([AllCommunityModule]);
registerAgGridDarkTheme();

/** Admin dashboard "Permissions" screen: full CRUD over the permission catalog itself (not the
 *  role-to-permission mapping - see RolePermissions for that). Platform admin only
 *  (PLATFORM_IDENTITY:MANAGE), same as every other identity-administration screen. Uses the same
 *  AG Grid table UX as the Roles screen. */
@Component({
  selector: 'app-permissions',
  imports: [AgGridAngular, FormsModule, ButtonModule, InputTextModule, MessageModule, DialogModule],
  templateUrl: './permissions.html',
})
export class Permissions implements OnInit {
  private readonly api = inject(RolesApiService);

  readonly permissions = signal<Permission[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly formVisible = signal(false);
  readonly formErrorMessage = signal<string | null>(null);
  readonly editingPermission = signal<Permission | null>(null);

  formPermissionKey = '';
  formResourceType = '';
  formAction = '';
  formDescription = '';

  readonly defaultColDef = defaultColDef;
  readonly columnDefs: ColDef<Permission>[] = [
    { field: 'permissionKey', headerName: 'Permission key', flex: 2 },
    { field: 'resourceType', headerName: 'Resource', flex: 1 },
    { field: 'action', headerName: 'Action', flex: 1 },
    { field: 'description', headerName: '', flex: 1, cellRenderer: DescriptionCellRenderer },
    {
      headerName: '',
      flex: 2,
      cellRenderer: PermissionActionsCellRenderer,
      cellRendererParams: {
        onEdit: (permission: Permission) => this.openEdit(permission),
        onDelete: (permission: Permission) => this.delete(permission),
      },
    },
  ];

  ngOnInit(): void {
    this.reload();
  }

  private reload(): void {
    this.loading.set(true);
    this.api.listPermissions().subscribe({
      next: (permissions) => {
        this.permissions.set(permissions);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  /** Opens the dialog for creating a brand-new permission - a blank form. */
  openCreate(): void {
    this.editingPermission.set(null);
    this.formErrorMessage.set(null);
    this.formPermissionKey = '';
    this.formResourceType = '';
    this.formAction = '';
    this.formDescription = '';
    this.formVisible.set(true);
  }

  /** Opens the dialog pre-filled with an existing permission's fields, to edit them. */
  openEdit(permission: Permission): void {
    this.editingPermission.set(permission);
    this.formErrorMessage.set(null);
    this.formPermissionKey = permission.permissionKey;
    this.formResourceType = permission.resourceType;
    this.formAction = permission.action;
    this.formDescription = permission.description ?? '';
    this.formVisible.set(true);
  }

  closeForm(): void {
    this.formVisible.set(false);
  }

  save(): void {
    this.formErrorMessage.set(null);
    if (!this.formPermissionKey || !this.formResourceType || !this.formAction) {
      this.formErrorMessage.set('Permission key, resource type and action are required');
      return;
    }
    const request = {
      permissionKey: this.formPermissionKey,
      resourceType: this.formResourceType,
      action: this.formAction,
      description: this.formDescription || null,
    };
    const editing = this.editingPermission();
    const save$ = editing
      ? this.api.updatePermission(editing.permissionId, request)
      : this.api.createPermission(request);
    save$.subscribe({
      next: () => {
        this.formVisible.set(false);
        this.reload();
      },
      error: (err) => this.formErrorMessage.set(err.error ?? 'Failed to save permission'),
    });
  }

  delete(permission: Permission): void {
    this.errorMessage.set(null);
    this.api.deletePermission(permission.permissionId).subscribe({
      next: () => this.reload(),
      error: (err) => this.errorMessage.set(err.error ?? 'Failed to delete permission'),
    });
  }
}
