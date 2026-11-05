import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AgGridAngular } from 'ag-grid-angular';
import { AllCommunityModule, ColDef, ModuleRegistry } from 'ag-grid-community';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { AdminApiService } from '../../../core/admin-api.service';
import { Brand } from '../../../core/models';
import { defaultColDef, licenseStatusCellRenderer } from '../../../core/grid-defaults';
import { registerAgGridDarkTheme } from '../../../core/ag-grid-theme';

ModuleRegistry.registerModules([AllCommunityModule]);
registerAgGridDarkTheme();

/** Admin dashboard landing screen: lists every brand on the platform (not just ones the caller
 *  has access to) and lets a PLATFORM_ADMIN create a new one. Only reachable via {@code
 *  adminGuard} (PLATFORM_BRAND:MANAGE/PLATFORM_IDENTITY:MANAGE), re-checked by the backend
 *  regardless of what this UI shows. */
@Component({
  selector: 'app-admin-brands',
  imports: [AgGridAngular, FormsModule, ButtonModule, InputTextModule, MessageModule],
  templateUrl: './admin-brands.html',
  styleUrl: './admin-brands.scss',
})
export class AdminBrands implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly router = inject(Router);

  readonly rowData = signal<Brand[]>([]);
  readonly loading = signal(true);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  newBrandCode = '';
  newBrandName = '';
  newBrandDescription = '';

  readonly columnDefs: ColDef<Brand>[] = [
    { field: 'brandCode', headerName: 'Code', flex: 1 },
    { field: 'name', headerName: 'Name', flex: 2 },
    { field: 'licenseStatus', headerName: 'License status', flex: 1, cellRenderer: licenseStatusCellRenderer },
    { field: 'createdAt', headerName: 'Created at', flex: 1 },
  ];

  readonly defaultColDef = defaultColDef;

  ngOnInit(): void {
    this.reload();
  }

  private reload(): void {
    this.loading.set(true);
    this.api.listBrands().subscribe({
      next: (brands) => {
        this.rowData.set(brands);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  createBrand(): void {
    this.successMessage.set(null);
    this.errorMessage.set(null);
    if (!this.newBrandCode || !this.newBrandName) {
      this.errorMessage.set('Brand code and name are required');
      return;
    }
    this.api
      .createBrand({
        brandCode: this.newBrandCode,
        name: this.newBrandName,
        description: this.newBrandDescription,
      })
      .subscribe({
        next: (brand) => {
          this.successMessage.set(`Created brand: ${brand.brandCode}`);
          this.newBrandCode = '';
          this.newBrandName = '';
          this.newBrandDescription = '';
          this.reload();
        },
        error: (err) => this.errorMessage.set(err.error ?? 'Failed to create brand'),
      });
  }

  onRowClicked(brandId: string | undefined): void {
    if (!brandId) {
      return;
    }
    this.router.navigate(['/admin/brands', brandId]);
  }
}
