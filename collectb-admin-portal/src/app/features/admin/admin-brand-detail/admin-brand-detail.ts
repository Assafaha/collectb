import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AgGridAngular } from 'ag-grid-angular';
import { AllCommunityModule, ColDef, ModuleRegistry } from 'ag-grid-community';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { MessageModule } from 'primeng/message';
import { AdminApiService } from '../../../core/admin-api.service';
import { Brand } from '../../../core/models';
import { BrandUserRow, groupBrandUsers } from '../../../core/brand-user-rows';
import { BrandRoleActionsCellRenderer } from '../../brand-detail/brand-role-actions-cell-renderer';
import { defaultColDef } from '../../../core/grid-defaults';
import { registerAgGridDarkTheme } from '../../../core/ag-grid-theme';

ModuleRegistry.registerModules([AllCommunityModule]);
registerAgGridDarkTheme();

/** Mirrors com.example.collectb.catalog.model.BrandLicenseStatus. */
const LICENSE_STATUSES = ['DRAFT', 'ACTIVE', 'SUSPENDED', 'REVOKED'];

/** Admin dashboard brand detail screen: brand info + a license-status control (the "how do I
 *  approve a brand" flow) + a manage-any-brand's-users grid, all backed by the
 *  platform-admin-only /api/v1/admin/brands/** endpoints. Only reachable via adminGuard. */
@Component({
  selector: 'app-admin-brand-detail',
  imports: [
    AgGridAngular,
    FormsModule,
    RouterLink,
    ButtonModule,
    InputTextModule,
    SelectModule,
    MessageModule,
  ],
  templateUrl: './admin-brand-detail.html',
  styleUrl: './admin-brand-detail.scss',
})
export class AdminBrandDetail implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly route = inject(ActivatedRoute);

  private brandId = '';

  readonly brand = signal<Brand | null>(null);
  readonly users = signal<BrandUserRow[]>([]);
  readonly assignableRoles = signal<string[]>([]);
  readonly loading = signal(true);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly licenseStatuses = LICENSE_STATUSES;
  selectedLicenseStatus: string | null = null;

  grantEmail = '';
  grantRoleName: string | null = null;

  readonly columnDefs: ColDef<BrandUserRow>[] = [
    { field: 'userEmail', headerName: 'Email', flex: 2 },
    { field: 'userDisplayName', headerName: 'Display name', flex: 1 },
    { field: 'roleName', headerName: 'Roles', flex: 1 },
    { field: 'assignedAt', headerName: 'Assigned at', flex: 1 },
    {
      headerName: '',
      flex: 2,
      autoHeight: true,
      cellRenderer: BrandRoleActionsCellRenderer,
      cellRendererParams: {
        onRevoke: (roleAssignmentId: string) => this.revoke(roleAssignmentId),
      },
    },
  ];

  readonly defaultColDef = defaultColDef;

  ngOnInit(): void {
    this.brandId = this.route.snapshot.paramMap.get('brandId')!;
    this.reload();
    this.api.getAssignableRoles(this.brandId).subscribe({
      next: (roles) => this.assignableRoles.set(roles),
      error: () => {
        // Leaving the picker empty is enough feedback if this fails.
      },
    });
  }

  private reload(): void {
    this.loading.set(true);
    this.api.getBrand(this.brandId).subscribe((brand) => {
      this.brand.set(brand);
      this.selectedLicenseStatus = brand.licenseStatus;
    });
    this.api.getBrandUsers(this.brandId).subscribe({
      next: (users) => {
        this.users.set(groupBrandUsers(users));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  updateLicenseStatus(): void {
    this.successMessage.set(null);
    this.errorMessage.set(null);
    if (!this.selectedLicenseStatus) {
      return;
    }
    this.api.updateLicenseStatus(this.brandId, this.selectedLicenseStatus).subscribe({
      next: (brand) => {
        this.brand.set(brand);
        this.successMessage.set(`License status updated to ${brand.licenseStatus}`);
      },
      error: (err) => this.errorMessage.set(err.error ?? 'Failed to update license status'),
    });
  }

  grant(): void {
    this.successMessage.set(null);
    this.errorMessage.set(null);
    if (!this.grantEmail || !this.grantRoleName) {
      this.errorMessage.set('Email and role are required');
      return;
    }
    this.api
      .grantBrandUser(this.brandId, { email: this.grantEmail, roleName: this.grantRoleName })
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

  revoke(roleAssignmentId: string): void {
    this.successMessage.set(null);
    this.errorMessage.set(null);
    this.api.revokeBrandUser(this.brandId, roleAssignmentId).subscribe({
      next: () => {
        this.successMessage.set('Access revoked');
        this.reload();
      },
      error: (err) => this.errorMessage.set(err.error ?? 'Failed to revoke access'),
    });
  }
}
