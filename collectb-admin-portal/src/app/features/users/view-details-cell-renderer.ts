import { Component } from '@angular/core';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';

export interface ViewDetailsCellRendererParams extends ICellRendererParams {
  onView: (userId: string) => void;
}

/** AG Grid cell renderer for the Users table's "Details" column - opens the per-user roles and
 *  permissions dialog (see Users#openDetail). Always rendered (unlike RevokeCellRenderer): every
 *  row, including a brand-less/role-less user, is still a real platform_user worth inspecting. */
@Component({
  selector: 'app-view-details-cell-renderer',
  template: `<button type="button" class="details-btn" (click)="view()">Details</button>`,
  styles: [
    `
      .details-btn {
        background: transparent;
        color: #60a5fa;
        border: 1px solid #60a5fa55;
        border-radius: 6px;
        padding: 0.25rem 0.65rem;
        font-size: 0.8rem;
        font-weight: 600;
        cursor: pointer;
        transition: background 0.12s ease, border-color 0.12s ease;
      }
      .details-btn:hover {
        background: #60a5fa1f;
        border-color: #60a5fa;
      }
    `,
  ],
})
export class ViewDetailsCellRenderer implements ICellRendererAngularComp {
  private params!: ViewDetailsCellRendererParams;

  agInit(params: ViewDetailsCellRendererParams): void {
    this.params = params;
  }

  refresh(): boolean {
    return false;
  }

  view(): void {
    this.params.onView(this.params.data.userId);
  }
}
