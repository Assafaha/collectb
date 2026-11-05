import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AgGridAngular } from 'ag-grid-angular';
import { AllCommunityModule, ColDef, ModuleRegistry } from 'ag-grid-community';
import { BrandPortalApiService } from '../../core/api.service';
import { MyBrandAccess } from '../../core/models';
import { defaultColDef, licenseStatusCellRenderer } from '../../core/grid-defaults';
import { registerAgGridDarkTheme } from '../../core/ag-grid-theme';

ModuleRegistry.registerModules([AllCommunityModule]);
registerAgGridDarkTheme();

/** "My Brands" landing screen: lists every brand the logged-in user holds a BRAND-scoped role
 *  on (via GET /api/v1/me/brands), and lets them drill into one to manage its users. */
@Component({
  selector: 'app-my-brands',
  imports: [AgGridAngular],
  templateUrl: './my-brands.html',
  styleUrl: './my-brands.scss',
})
export class MyBrands implements OnInit {
  private readonly api = inject(BrandPortalApiService);
  private readonly router = inject(Router);

  readonly rowData = signal<MyBrandAccess[]>([]);
  readonly loading = signal(true);

  readonly defaultColDef = defaultColDef;
  readonly columnDefs: ColDef<MyBrandAccess>[] = [
    { field: 'brandCode', headerName: 'Code', flex: 1 },
    { field: 'name', headerName: 'Name', flex: 2 },
    { field: 'licenseStatus', headerName: 'License status', flex: 1, cellRenderer: licenseStatusCellRenderer },
    { field: 'myRoleName', headerName: 'My role', flex: 1 },
    { field: 'createdAt', headerName: 'Created at', flex: 1 },
  ];

  ngOnInit(): void {
    this.api.getMyBrands().subscribe({
      next: (brands) => {
        this.rowData.set(brands);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onRowClicked(brandId: string | undefined): void {
    if (!brandId) {
      return;
    }
    this.router.navigate(['/brands', brandId]);
  }
}
