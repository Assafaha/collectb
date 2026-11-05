import { Component } from '@angular/core';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';

export interface RevokeCellRendererParams extends ICellRendererParams {
  onRevoke: (roleAssignmentId: string) => void;
  /** Optional override for whether this row has anything to revoke at all - defaults to just
   *  checking {@code data.roleAssignmentId}. The admin dashboard's "Users" table (which can also
   *  show a user with only a PLATFORM-scoped role, or no role at all - see UsersController)
   *  passes a stricter check requiring a brandId too, since only BRAND-scoped grants are
   *  revocable through this UI. */
  isRevocable?: (data: unknown) => boolean;
}

/** AG Grid cell renderer for the users table's "Revoke" action column. Renders nothing for a row
 *  with nothing to revoke. */
@Component({
  selector: 'app-revoke-cell-renderer',
  template: `@if (isRevocable()) {
    <button type="button" class="revoke-btn" (click)="revoke()">Revoke</button>
  }`,
  styles: [
    `
      .revoke-btn {
        background: transparent;
        color: #f87171;
        border: 1px solid #f8717155;
        border-radius: 6px;
        padding: 0.25rem 0.65rem;
        font-size: 0.8rem;
        font-weight: 600;
        cursor: pointer;
        transition: background 0.12s ease, border-color 0.12s ease;
      }
      .revoke-btn:hover {
        background: #ef44441f;
        border-color: #ef4444;
      }
    `,
  ],
})
export class RevokeCellRenderer implements ICellRendererAngularComp {
  params!: RevokeCellRendererParams;

  agInit(params: RevokeCellRendererParams): void {
    this.params = params;
  }

  refresh(): boolean {
    return false;
  }

  isRevocable(): boolean {
    if (this.params.isRevocable) {
      return this.params.isRevocable(this.params.data);
    }
    return !!this.params.data.roleAssignmentId;
  }

  revoke(): void {
    this.params.onRevoke(this.params.data.roleAssignmentId);
  }
}
