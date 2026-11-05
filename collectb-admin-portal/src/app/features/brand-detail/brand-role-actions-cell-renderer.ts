import { Component } from '@angular/core';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';
import { BrandUserRow } from '../../core/brand-user-rows';
import { BrandUser } from '../../core/models';

interface BrandRoleActionsParams extends ICellRendererParams<BrandUserRow> {
  onRevoke: (roleAssignmentId: string) => void;
}

@Component({
  selector: 'app-brand-role-actions-cell-renderer',
  template: `
    @for (assignment of assignments; track assignment.roleAssignmentId) {
      <button type="button" (click)="revoke(assignment.roleAssignmentId)">
        Revoke {{ assignment.roleName }}
      </button>
    }
  `,
  styles: [`
    :host { display: flex; flex-wrap: wrap; gap: 0.35rem; padding: 0.25rem 0; }
    button {
      background: transparent; color: #f87171; border: 1px solid #f8717155;
      border-radius: 6px; padding: 0.25rem 0.65rem; cursor: pointer;
    }
    button:hover { background: #ef44441f; border-color: #ef4444; }
  `],
})
export class BrandRoleActionsCellRenderer implements ICellRendererAngularComp {
  assignments: BrandUser[] = [];
  private params!: BrandRoleActionsParams;

  agInit(params: BrandRoleActionsParams): void {
    this.params = params;
    this.assignments = params.data?.assignments ?? [];
  }

  refresh(params: BrandRoleActionsParams): boolean {
    this.agInit(params);
    return true;
  }

  revoke(roleAssignmentId: string): void {
    this.params.onRevoke(roleAssignmentId);
  }
}
