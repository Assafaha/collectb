import { Component, OnInit, inject, signal } from '@angular/core';
import { AgGridAngular } from 'ag-grid-angular';
import { AllCommunityModule, ColDef, ModuleRegistry } from 'ag-grid-community';
import { RolesApiService } from '../../core/roles-api.service';
import { Role } from '../../core/models';
import { defaultColDef } from '../../core/grid-defaults';
import { registerAgGridDarkTheme } from '../../core/ag-grid-theme';

ModuleRegistry.registerModules([AllCommunityModule]);
registerAgGridDarkTheme();

/** Read-only table of the role catalog, for PLATFORM_ADMIN and BRAND_EDITOR. */
@Component({
  selector: 'app-roles',
  imports: [AgGridAngular],
  templateUrl: './roles.html',
})
export class Roles implements OnInit {
  private readonly api = inject(RolesApiService);

  readonly rows = signal<Role[]>([]);
  readonly loading = signal(true);

  readonly defaultColDef = defaultColDef;
  readonly columnDefs: ColDef<Role>[] = [
    { field: 'roleName', headerName: 'Role', flex: 1 },
    { field: 'roleType', headerName: 'Type', flex: 1 },
    { field: 'description', headerName: 'Description', flex: 3, valueFormatter: (p) => p.value ?? '—' },
    { field: 'privileged', headerName: 'Privileged', flex: 1, valueFormatter: (p) => (p.value ? 'Yes' : 'No') },
  ];

  ngOnInit(): void {
    this.api.listRoles().subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
