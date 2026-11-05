import { ColDef } from 'ag-grid-community';

/** Shared AG Grid column defaults (sortable/resizable/filterable) applied via each grid's
 *  [defaultColDef] input, so every table across the portal behaves consistently. */
export const defaultColDef: ColDef = {
  sortable: true,
  resizable: true,
  filter: true,
};

const LICENSE_STATUS_BADGE_CLASS: Record<string, string> = {
  ACTIVE: 'cb-badge-success',
  DRAFT: 'cb-badge-neutral',
  SUSPENDED: 'cb-badge-warning',
  REVOKED: 'cb-badge-danger',
  TERMINATED: 'cb-badge-danger',
};

/** Renders a brand's license status as a colored pill instead of plain text, reused by every
 *  grid that shows a `licenseStatus` column (My Brands, Brand Detail, Admin Brands/Detail). */
export function licenseStatusCellRenderer(params: { value?: string }): string {
  const status = params.value ?? '';
  const badgeClass = LICENSE_STATUS_BADGE_CLASS[status] ?? 'cb-badge-neutral';
  return `<span class="cb-badge ${badgeClass}">${status}</span>`;
}
